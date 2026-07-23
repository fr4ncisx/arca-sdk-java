package io.github.fr4ncisx.arca.wsfev1.internal.usecase.batch;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.cae.RequestCaeUseCase;
import io.github.fr4ncisx.arca.wsfev1.model.batch.BatchRequest;
import io.github.fr4ncisx.arca.wsfev1.model.batch.BatchResponse;
import io.github.fr4ncisx.arca.wsfev1.model.batch.BatchStrategy;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeRequest;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeResponse;
import io.github.fr4ncisx.arca.wsfev1.model.common.ConceptType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BatchProcessUseCase}.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
@SuppressWarnings("null")
class BatchProcessUseCaseTest {

    private ExecutorService executorService;
    private RequestCaeUseCase requestCaeUseCaseMock;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(4);
        requestCaeUseCaseMock = mock(RequestCaeUseCase.class);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdown();
    }

    @Test
    void constructorRejectsNulls() {
        assertThatThrownBy(() -> new BatchProcessUseCase(null, executorService))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> new BatchProcessUseCase(requestCaeUseCaseMock, null))
                .isInstanceOf(ArcaValidationException.class);
    }

    @Test
    void executeSequentialSucceeds() {
        CaeRequest r1 = createRequest(1L);
        CaeRequest r2 = createRequest(2L);
        CaeResponse res = new CaeResponse(true, "cae-val", LocalDate.now(), List.of());

        when(requestCaeUseCaseMock.execute(r1)).thenReturn(res);
        when(requestCaeUseCaseMock.execute(r2)).thenReturn(res);

        BatchProcessUseCase useCase = new BatchProcessUseCase(requestCaeUseCaseMock, executorService);
        BatchRequest batchReq = new BatchRequest(List.of(r1, r2), BatchStrategy.SEQUENTIAL, 0);

        BatchResponse response = useCase.execute(batchReq);

        assertThat(response.interrupted()).isFalse();
        assertThat(response.entries()).hasSize(2);
        assertThat(response.entries().get(0).response()).isEqualTo(res);
    }

    @Test
    void executeSequentialFailFastAbortsOnFirstNetworkException() {
        CaeRequest r1 = createRequest(1L);
        CaeRequest r2 = createRequest(2L);

        when(requestCaeUseCaseMock.execute(r1)).thenThrow(new ArcaSoapException("Network down"));
        when(requestCaeUseCaseMock.execute(r2)).thenReturn(new CaeResponse(true, "cae-val", LocalDate.now(), List.of()));

        BatchProcessUseCase useCase = new BatchProcessUseCase(requestCaeUseCaseMock, executorService);
        BatchRequest batchReq = new BatchRequest(List.of(r1, r2), BatchStrategy.FAIL_FAST, 0);

        BatchResponse response = useCase.execute(batchReq);

        assertThat(response.interrupted()).isTrue();
        assertThat(response.entries()).hasSize(1);
        assertThat(response.entries().get(0).error()).isNotNull();
        assertThat(response.entries().get(0).error()).isInstanceOf(ArcaSoapException.class);
    }

    @Test
    void executeParallelLimitedRespectsConcurrencyLimit() throws Exception {
        List<CaeRequest> requests = new ArrayList<>();
        for (long i = 1; i <= 4; i++) {
            requests.add(createRequest(i));
        }

        AtomicInteger activeCount = new AtomicInteger(0);
        AtomicInteger maxActive = new AtomicInteger(0);

        when(requestCaeUseCaseMock.execute(ArgumentMatchers.any(CaeRequest.class))).thenAnswer(invocation -> {
            int active = activeCount.incrementAndGet();
            synchronized (maxActive) {
                if (active > maxActive.get()) {
                    maxActive.set(active);
                }
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            activeCount.decrementAndGet();
            return new CaeResponse(true, "cae-val", LocalDate.now(), List.of());
        });

        BatchProcessUseCase useCase = new BatchProcessUseCase(requestCaeUseCaseMock, executorService);
        BatchRequest batchReq = new BatchRequest(requests, BatchStrategy.PARALLEL_LIMITED, 2);

        BatchResponse response = useCase.execute(batchReq);

        assertThat(response.interrupted()).isFalse();
        assertThat(response.entries()).hasSize(4);
        assertThat(maxActive.get()).isLessThanOrEqualTo(2);
    }

    private CaeRequest createRequest(long number) {
        return new CaeRequest(
                VoucherType.INVOICE_A,
                1,
                number,
                ConceptType.PRODUCTS,
                Cuit.parse("20-33333333-4"),
                100.0,
                0.0,
                0.0,
                21.0,
                121.0,
                LocalDate.now(),
                List.of()
        );
    }
}
