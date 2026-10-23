package io.github.fr4ncisx.arca.soap.internal.resilience;

import io.github.fr4ncisx.arca.core.exception.ArcaCircuitOpenException;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
            throw new ArcaSoapException("SOAP invocation timed out", new SocketTimeoutException("Read timed out"));
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
                throw new ArcaSoapException("SOAP invocation timed out", new HttpTimeoutException("Response timed out"));
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

    /**
     * Verifies that only one request probes the remote service while the circuit is half-open.
     *
     * @throws Exception if the probe does not finish within the test deadline.
     */
    @Test
    void allowsOnlyOneConcurrentHalfOpenProbe() throws Exception {
        var callCount = new AtomicInteger();
        var probeStarted = new CountDownLatch(1);
        var releaseProbe = new CountDownLatch(1);
        ArcaSoapPort<String, String> delegate = req -> {
            int attempt = callCount.incrementAndGet();
            if (attempt == 1) {
                throw new ArcaSoapException("Timeout", new SocketTimeoutException("Read timed out"));
            }
            if (attempt == 2) {
                probeStarted.countDown();
                try {
                    releaseProbe.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new ArcaSoapException("Probe interrupted", exception);
                }
            }
            return RESPONSE;
        };
        var port = new ResilienceDecorator<>(delegate, 1, 0L, 1, 0L);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            assertThatThrownBy(() -> port.invoke(REQUEST)).isInstanceOf(ArcaSoapException.class);
            Thread.sleep(5);
            Future<String> probe = executor.submit(() -> port.invoke(REQUEST));

            assertThat(probeStarted.await(2, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> port.invoke(REQUEST)).isInstanceOf(ArcaCircuitOpenException.class);
            releaseProbe.countDown();

            assertThat(probe.get(2, TimeUnit.SECONDS)).isEqualTo(RESPONSE);
            assertThat(callCount.get()).isEqualTo(2);
        } finally {
            releaseProbe.countDown();
            executor.shutdownNow();
        }
    }

    /**
     * Verifies that the retry limit counts the initial attempt in the total invocation limit.
     */
    @Test
    void limitsSafeOperationToThreeTotalAttempts() {
        var callCount = new AtomicInteger();
        ArcaSoapPort<String, String> delegate = req -> {
            callCount.incrementAndGet();
            throw new ArcaSoapException("Timeout", new SocketTimeoutException("Read timed out"));
        };
        var port = new ResilienceDecorator<>(delegate, 3, 0L, 5, 1000L);

        assertThatThrownBy(() -> port.invoke(REQUEST)).isInstanceOf(ArcaSoapException.class);

        assertThat(callCount.get()).isEqualTo(3);
    }

    /**
     * Verifies that a transient failure from a half-open probe reopens the circuit.
     *
     * @throws Exception if the circuit cooldown cannot be reached.
     */
    @Test
    void reopensCircuitAfterFailedHalfOpenProbe() throws Exception {
        var callCount = new AtomicInteger();
        ArcaSoapPort<String, String> delegate = req -> {
            callCount.incrementAndGet();
            throw new ArcaSoapException("Timeout", new SocketTimeoutException("Read timed out"));
        };
        var port = new ResilienceDecorator<>(delegate, 1, 0L, 1, 1_000L);

        assertThatThrownBy(() -> port.invoke(REQUEST)).isInstanceOf(ArcaSoapException.class);
        Thread.sleep(1_100L);
        assertThatThrownBy(() -> port.invoke(REQUEST)).isInstanceOf(ArcaSoapException.class);

        assertThatThrownBy(() -> port.invoke(REQUEST)).isInstanceOf(ArcaCircuitOpenException.class);
        assertThat(callCount.get()).isEqualTo(2);
    }
}
