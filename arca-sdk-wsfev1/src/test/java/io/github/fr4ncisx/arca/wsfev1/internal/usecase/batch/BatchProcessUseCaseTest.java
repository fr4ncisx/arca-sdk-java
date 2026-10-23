package io.github.fr4ncisx.arca.wsfev1.internal.usecase.batch;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.cae.RequestCaeUseCase;
import io.github.fr4ncisx.arca.wsfev1.model.batch.BatchEntry;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

    /**
     * Verifies that a large batch submits no more than its concurrency window while tasks are pending.
     *
     * @throws Exception if batch execution does not complete within the test deadline.
     */
    @Test
    void executeParallelLimitedSubmitsOnlyTheConcurrencyWindow() throws Exception {
        int maxParallel = 3;
        CountDownLatch started = new CountDownLatch(maxParallel);
        CountDownLatch release = new CountDownLatch(1);
        var batchExecutor = new ThreadPoolExecutor(
                maxParallel,
                maxParallel,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        var callerExecutor = Executors.newSingleThreadExecutor();
        List<CaeRequest> requests = new ArrayList<>();
        CaeResponse response = new CaeResponse(true, "cae-val", LocalDate.now(), List.of());
        for (long number = 1; number <= 100; number++) {
            requests.add(createRequest(number));
        }
        when(requestCaeUseCaseMock.execute(ArgumentMatchers.any(CaeRequest.class))).thenAnswer(invocation -> {
            started.countDown();
            release.await();
            return response;
        });

        try {
            BatchProcessUseCase useCase = new BatchProcessUseCase(requestCaeUseCaseMock, batchExecutor);
            Future<BatchResponse> batch = callerExecutor.submit(() -> useCase.execute(
                    new BatchRequest(requests, BatchStrategy.PARALLEL_LIMITED, maxParallel)));

            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(100);

            assertThat(batchExecutor.getActiveCount() + batchExecutor.getQueue().size())
                    .isLessThanOrEqualTo(maxParallel);
            release.countDown();
            assertThat(batch.get(5, TimeUnit.SECONDS).entries()).hasSize(requests.size());
        } finally {
            release.countDown();
            callerExecutor.shutdownNow();
            batchExecutor.shutdownNow();
        }
    }

    /**
     * Verifies that interruption returns completed entries and restores the caller interrupt flag.
     *
     * @throws Exception if the worker does not start or the caller does not stop within the deadline.
     */
    @Test
    void executeParallelLimitedReportsCallerInterruption() throws Exception {
        CaeRequest first = createRequest(1L);
        CaeRequest second = createRequest(2L);
        CaeResponse response = new CaeResponse(true, "cae-val", LocalDate.now(), List.of());
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch releaseSecond = new CountDownLatch(1);
        AtomicReference<BatchResponse> batchResponse = new AtomicReference<>();
        AtomicBoolean interruptRestored = new AtomicBoolean();
        when(requestCaeUseCaseMock.execute(first)).thenReturn(response);
        when(requestCaeUseCaseMock.execute(second)).thenAnswer(invocation -> {
            secondStarted.countDown();
            try {
                releaseSecond.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ArcaSoapException("Batch worker interrupted", exception);
            }
            return response;
        });
        BatchProcessUseCase useCase = new BatchProcessUseCase(requestCaeUseCaseMock, executorService);
        Thread caller = new Thread(() -> {
            batchResponse.set(useCase.execute(
                    new BatchRequest(List.of(first, second), BatchStrategy.PARALLEL_LIMITED, 1)));
            interruptRestored.set(Thread.currentThread().isInterrupted());
        });

        try {
            caller.start();
            assertThat(secondStarted.await(2, TimeUnit.SECONDS)).isTrue();
            caller.interrupt();
            caller.join(2_000L);
            assertThat(caller.isAlive()).isFalse();
        } finally {
            releaseSecond.countDown();
            caller.join(2_000L);
        }

        assertThat(batchResponse.get()).isNotNull();
        assertThat(batchResponse.get().interrupted()).isTrue();
        assertThat(batchResponse.get().entries()).extracting(BatchEntry::request).containsExactly(first, second);
        assertThat(batchResponse.get().entries().get(0).response()).isSameAs(response);
        assertThat(batchResponse.get().entries().get(1).error()).isInstanceOf(ArcaSoapException.class)
                .hasMessageContaining("outcome is unknown");
        assertThat(interruptRestored.get()).isTrue();
    }

    /**
     * Stops sequential dispatch after interruption and preserves every input position.
     */
    @Test
    void stopsSequentialDispatchOnInterruption() {
        CaeRequest first = createRequest(1L);
        CaeRequest second = createRequest(2L);
        CaeResponse success = new CaeResponse(true, "cae", LocalDate.now(), List.of());
        when(requestCaeUseCaseMock.execute(first)).thenAnswer(invocation -> {
            Thread.currentThread().interrupt();
            return success;
        });
        try {
            BatchResponse response = new BatchProcessUseCase(requestCaeUseCaseMock, executorService)
                    .execute(new BatchRequest(List.of(first, second), BatchStrategy.SEQUENTIAL, 0));
            assertThat(response.interrupted()).isTrue();
            assertThat(response.entries()).extracting(BatchEntry::request).containsExactly(first, second);
            assertThat(response.entries().get(0).response()).isSameAs(success);
            assertThat(response.entries().get(1).error()).isInstanceOf(ArcaSoapException.class)
                    .hasMessageContaining("not submitted");
            verify(requestCaeUseCaseMock, never()).execute(second);
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * Preserves duplicate positions without dispatching an already interrupted batch.
     */
    @Test
    void doesNotSubmitPreInterruptedParallelBatch() {
        CaeRequest duplicate = createRequest(1L);
        Thread.currentThread().interrupt();
        try {
            BatchResponse response = new BatchProcessUseCase(requestCaeUseCaseMock, executorService)
                    .execute(new BatchRequest(List.of(duplicate, duplicate), BatchStrategy.PARALLEL_LIMITED, 2));
            assertThat(response.interrupted()).isTrue();
            assertThat(response.entries()).hasSize(2);
            assertThat(response.entries()).allSatisfy(entry -> assertThat(entry.error())
                    .isInstanceOf(ArcaSoapException.class).hasMessageContaining("not submitted"));
            verifyNoInteractions(requestCaeUseCaseMock);
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * Verifies that fatal JVM errors are not converted into ordinary batch failures.
     */
    @Test
    void executeSequentialDoesNotSwallowErrors() {
        CaeRequest request = createRequest(1L);
        AssertionError failure = new AssertionError("fatal failure");
        when(requestCaeUseCaseMock.execute(request)).thenThrow(failure);
        BatchProcessUseCase useCase = new BatchProcessUseCase(requestCaeUseCaseMock, executorService);

        assertThatThrownBy(() -> useCase.execute(new BatchRequest(
                List.of(request), BatchStrategy.SEQUENTIAL, 0)))
                .isSameAs(failure);
    }

    /**
     * Verifies that equal requests retain distinct outcomes in parallel batches.
     */
    @Test
    void executeParallelLimitedKeepsSeparateResultsForDuplicateRequests() {
        CaeRequest duplicate = createRequest(1L);
        CaeResponse success = new CaeResponse(true, "cae-val", LocalDate.now(), List.of());
        ArcaSoapException failure = new ArcaSoapException("Duplicate voucher rejected");
        when(requestCaeUseCaseMock.execute(duplicate)).thenReturn(success).thenThrow(failure);

        BatchProcessUseCase useCase = new BatchProcessUseCase(requestCaeUseCaseMock, executorService);
        BatchResponse response = useCase.execute(
                new BatchRequest(List.of(duplicate, duplicate), BatchStrategy.PARALLEL_LIMITED, 2));

        assertThat(response.entries()).hasSize(2);
        assertThat(response.entries()).extracting(BatchEntry::response)
                .containsExactlyInAnyOrder(success, null);
        assertThat(response.entries()).filteredOn(entry -> entry.error() != null).hasSize(1);
    }

    /**
     * Verifies that parallel results remain in input order when requests finish out of order.
     *
     * @throws Exception if the second request does not complete within the test deadline.
     */
    @Test
    void executeParallelLimitedReturnsResultsInInputOrder() throws Exception {
        CaeRequest first = createRequest(1L);
        CaeRequest second = createRequest(2L);
        CaeResponse firstResponse = new CaeResponse(true, "cae-first", LocalDate.now(), List.of());
        CaeResponse secondResponse = new CaeResponse(true, "cae-second", LocalDate.now(), List.of());
        CountDownLatch secondFinished = new CountDownLatch(1);
        when(requestCaeUseCaseMock.execute(first)).thenAnswer(invocation -> {
            if (!secondFinished.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("second request did not finish");
            }
            return firstResponse;
        });
        when(requestCaeUseCaseMock.execute(second)).thenAnswer(invocation -> {
            secondFinished.countDown();
            return secondResponse;
        });

        BatchProcessUseCase useCase = new BatchProcessUseCase(requestCaeUseCaseMock, executorService);
        BatchResponse response = useCase.execute(
                new BatchRequest(List.of(first, second), BatchStrategy.PARALLEL_LIMITED, 2));

        assertThat(response.entries()).extracting(BatchEntry::request).containsExactly(first, second);
        assertThat(response.entries()).extracting(BatchEntry::response)
                .containsExactly(firstResponse, secondResponse);
    }

    /**
     * Verifies that batch requests snapshot their input and reject null entries.
     */
    @Test
    void batchRequestSnapshotsInputAndRejectsNullEntries() {
        CaeRequest first = createRequest(1L);
        List<CaeRequest> mutableRequests = new ArrayList<>(List.of(first));
        BatchRequest request = new BatchRequest(mutableRequests, BatchStrategy.SEQUENTIAL, 0);
        mutableRequests.add(createRequest(2L));

        assertThat(request.requests()).containsExactly(first);
        assertThatThrownBy(() -> request.requests().add(createRequest(3L)))
                .isInstanceOf(UnsupportedOperationException.class);

        BatchEntry entry = new BatchEntry(first, null, null);
        List<BatchEntry> mutableEntries = new ArrayList<>(List.of(entry));
        BatchResponse response = new BatchResponse(mutableEntries, false);
        mutableEntries.clear();

        assertThat(response.entries()).containsExactly(entry);
        assertThatThrownBy(() -> response.entries().add(entry))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new BatchRequest(
                Arrays.asList(first, null), BatchStrategy.SEQUENTIAL, 0))
                .isInstanceOf(ArcaValidationException.class);
    }

    /**
     * Retains duplicate positions and distinguishes unresolved work from work never submitted.
     *
     * @throws Exception if the caller or worker does not reach the synchronization point.
     */
    @Test
    void retainsDuplicatePositionsWhenInterruptedOutOfOrder() throws Exception {
        CaeRequest first = createRequest(1L);
        CaeRequest duplicate = createRequest(1L);
        CaeRequest third = createRequest(3L);
        CaeRequest fourth = createRequest(4L);
        CaeResponse success = new CaeResponse(true, "cae-second", LocalDate.now(), List.of());
        CountDownLatch thirdStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<BatchResponse> result = new AtomicReference<>();
        when(requestCaeUseCaseMock.execute(ArgumentMatchers.any(CaeRequest.class))).thenAnswer(invocation -> {
            CaeRequest input = invocation.getArgument(0);
            if (input == duplicate) {
                return success;
            }
            if (input == third) {
                thirdStarted.countDown();
            }
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ArcaSoapException("worker interrupted", exception);
            }
            return success;
        });
        Thread caller = new Thread(() -> result.set(
                new BatchProcessUseCase(requestCaeUseCaseMock, executorService).execute(new BatchRequest(
                        List.of(first, duplicate, third, fourth), BatchStrategy.PARALLEL_LIMITED, 2))));
        try {
            caller.start();
            assertThat(thirdStarted.await(2, TimeUnit.SECONDS)).isTrue();
            caller.interrupt();
            caller.join(2_000L);
            assertThat(caller.isAlive()).isFalse();
            assertThat(result.get().interrupted()).isTrue();
            assertThat(result.get().entries()).hasSize(4);
            assertThat(result.get().entries().get(0).error()).isInstanceOf(ArcaSoapException.class)
                    .hasMessageContaining("outcome is unknown");
            assertThat(result.get().entries().get(1).response()).isSameAs(success);
            assertThat(result.get().entries().get(2).error()).isInstanceOf(ArcaSoapException.class)
                    .hasMessageContaining("outcome is unknown");
            assertThat(result.get().entries().get(3).error()).isInstanceOf(ArcaSoapException.class)
                    .hasMessageContaining("not submitted");
        } finally {
            release.countDown();
            caller.interrupt();
            caller.join(2_000L);
        }
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
