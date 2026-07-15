package io.github.fr4ncisx.arca.soap.internal.resilience;

import io.github.fr4ncisx.arca.core.exception.ArcaCircuitOpenException;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResilienceDecoratorTest {

    private static final String REQUEST = "req";
    private static final String RESPONSE = "res";

    @Test
    void invokesDelegateDirectlyAndSucceedsWithoutRetries() throws Exception {
        var callCount = new AtomicInteger(0);
        ArcaSoapPort<String, String> delegate = req -> {
            callCount.incrementAndGet();
            return RESPONSE;
        };

        var port = new ResilienceDecorator<>(delegate);
        String result = port.invoke(REQUEST);

        assertThat(result).isEqualTo(RESPONSE);
        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    void retriesOnTransientExceptionAndRecovers() throws Exception {
        var callCount = new AtomicInteger(0);
        ArcaSoapPort<String, String> delegate = req -> {
            int count = callCount.incrementAndGet();
            if (count == 1) {
                throw new ArcaSoapException("Timeout", new SocketTimeoutException("Read timed out"));
            }
            return RESPONSE;
        };

        var port = new ResilienceDecorator<>(delegate, 3, 10L, 5, 1000L);
        String result = port.invoke(REQUEST);

        assertThat(result).isEqualTo(RESPONSE);
        assertThat(callCount.get()).isEqualTo(2);
    }

    @Test
    void doesNotRetryOnNonTransientException() {
        var callCount = new AtomicInteger(0);
        ArcaSoapPort<String, String> delegate = req -> {
            callCount.incrementAndGet();
            throw new ArcaSoapException("Business fault", new RuntimeException("Validation failed"));
        };

        var port = new ResilienceDecorator<>(delegate, 3, 10L, 5, 1000L);

        assertThatThrownBy(() -> port.invoke(REQUEST))
                .isInstanceOf(ArcaSoapException.class)
                .hasMessageContaining("Business fault");

        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    void tripsCircuitAfterMaxConsecutiveFailuresAndFailsFast() {
        var callCount = new AtomicInteger(0);
        ArcaSoapPort<String, String> delegate = req -> {
            callCount.incrementAndGet();
            throw new ArcaSoapException("SOAP invocation timed out");
        };

        var port = new ResilienceDecorator<>(delegate, 1, 1L, 3, 1000L);

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> port.invoke(REQUEST))
                    .isInstanceOf(ArcaSoapException.class)
                    .hasMessageContaining("SOAP invocation timed out");
        }

        assertThat(callCount.get()).isEqualTo(3);

        assertThatThrownBy(() -> port.invoke(REQUEST))
                .isInstanceOf(ArcaCircuitOpenException.class)
                .hasMessageContaining("Circuit Breaker is OPEN");

        assertThat(callCount.get()).isEqualTo(3);
    }

    @Test
    void halfOpenTrialCallClosesCircuitOnSuccess() throws Exception {
        var callCount = new AtomicInteger(0);
        var succeed = new AtomicInteger(0);

        ArcaSoapPort<String, String> delegate = req -> {
            callCount.incrementAndGet();
            if (succeed.get() == 0) {
                throw new ArcaSoapException("SOAP invocation timed out");
            }
            return RESPONSE;
        };

        var port = new ResilienceDecorator<>(delegate, 1, 1L, 1, 100L);

        assertThatThrownBy(() -> port.invoke(REQUEST))
                .isInstanceOf(ArcaSoapException.class);

        assertThatThrownBy(() -> port.invoke(REQUEST))
                .isInstanceOf(ArcaCircuitOpenException.class);

        Thread.sleep(150);
        succeed.set(1);

        String result = port.invoke(REQUEST);
        assertThat(result).isEqualTo(RESPONSE);
        assertThat(callCount.get()).isEqualTo(2);

        String result2 = port.invoke(REQUEST);
        assertThat(result2).isEqualTo(RESPONSE);
        assertThat(callCount.get()).isEqualTo(3);
    }
}
