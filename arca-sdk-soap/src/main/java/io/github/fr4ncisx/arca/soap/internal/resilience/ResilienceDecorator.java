package io.github.fr4ncisx.arca.soap.internal.resilience;

import io.github.fr4ncisx.arca.core.exception.ArcaCircuitOpenException;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe decorator that adds exponential retry backoff and circuit breaker logic
 * to any {@link ArcaSoapPort} invocation.
 *
 * @param <R> the request type
 * @param <S> the response type
 * @author fr4ncisx
 * @since 1.1.0
 */
public final class ResilienceDecorator<R, S> implements ArcaSoapPort<R, S> {

    @SuppressWarnings("null")
    private static final Logger LOGGER = LoggerFactory.getLogger(ResilienceDecorator.class);

    private final ArcaSoapPort<R, S> delegate;
    private final int maxAttempts;
    private final long baseDelayMs;
    private final int maxConsecutiveFailures;
    private final long resetTimeoutMs;
    private final boolean retrySafe;

    private enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0L);

    /**
     * Creates a new ResilienceDecorator wrapping the specified delegate port
     * with default production resilience parameters.
     *
     * @param delegate the concrete SOAP port to decorate
     * @throws ArcaValidationException if the delegate is null.
     */
    public ResilienceDecorator(ArcaSoapPort<R, S> delegate) {
        this(delegate, 3, 500L, 5, 60_000L, true);
    }

    /**
     * Creates a new ResilienceDecorator with retries enabled only for retry-safe operations.
     *
     * @param delegate the concrete SOAP port to decorate.
     * @param retrySafe whether transient failures can safely trigger a retry.
     * @throws ArcaValidationException if the delegate is null.
     * @since 1.2.4
     */
    public ResilienceDecorator(ArcaSoapPort<R, S> delegate, boolean retrySafe) {
        this(delegate, 3, 500L, 5, 60_000L, retrySafe);
    }

    /**
     * Creates a new ResilienceDecorator with custom resilience parameters for testing.
     *
     * @param delegate               the concrete SOAP port to decorate
     * @param maxAttempts            maximum total invocations, including the initial attempt
     * @param baseDelayMs            base backoff delay in milliseconds
     * @param maxConsecutiveFailures consecutive failures allowed before tripping the circuit
     * @param resetTimeoutMs         cooldown time before attempting a reset in half-open state
     */
    ResilienceDecorator(ArcaSoapPort<R, S> delegate, int maxAttempts, long baseDelayMs,
                        int maxConsecutiveFailures, long resetTimeoutMs) {
        this(delegate, maxAttempts, baseDelayMs, maxConsecutiveFailures, resetTimeoutMs, true);
    }

    private ResilienceDecorator(ArcaSoapPort<R, S> delegate, int maxAttempts, long baseDelayMs,
                                int maxConsecutiveFailures, long resetTimeoutMs, boolean retrySafe) {
        if (delegate == null) {
            throw new ArcaValidationException("delegate port must not be null");
        }
        if (maxAttempts <= 0) {
            throw new ArcaValidationException("maxAttempts must be greater than 0");
        }
        if (baseDelayMs < 0L) {
            throw new ArcaValidationException("baseDelayMs must not be negative");
        }
        if (maxConsecutiveFailures <= 0) {
            throw new ArcaValidationException("maxConsecutiveFailures must be greater than 0");
        }
        if (resetTimeoutMs < 0L) {
            throw new ArcaValidationException("resetTimeoutMs must not be negative");
        }
        this.delegate = delegate;
        this.maxAttempts = maxAttempts;
        this.baseDelayMs = baseDelayMs;
        this.maxConsecutiveFailures = maxConsecutiveFailures;
        this.resetTimeoutMs = resetTimeoutMs;
        this.retrySafe = retrySafe;
    }

    /**
     * Invokes the operation with up to three total attempts for retry-safe timeouts.
     * <p>
     * An open circuit rejects calls. Only one recovery probe is admitted after
     * cooldown, and that probe is never retried automatically.
     *
     * @param request the request passed to the decorated port.
     * @return the response returned by the decorated port.
     * @throws ArcaCircuitOpenException if the circuit is open or a probe is already running.
     * @throws ArcaSoapException if transport fails or retry waiting is interrupted.
     */
    @Override
    public S invoke(R request) throws ArcaSoapException {
        boolean halfOpenProbe = checkCircuitState();

        int attempt = 0;
        long delay = baseDelayMs;

        while (true) {
            try {
                attempt++;
                S response = delegate.invoke(request);
                onSuccess();
                return response;
            } catch (ArcaSoapException exception) {
                if (attempt >= maxAttempts || halfOpenProbe || !retrySafe || !isTransient(exception)) {
                    onFailure(exception);
                    throw exception;
                }

                long jitteredDelay = calculateJitter(delay);
                LOGGER.warn("SOAP invocation failed (attempt {}/{}). Retrying in {}ms. Error: {}",
                        attempt, maxAttempts, jitteredDelay, exception.getMessage());

                try {
                    Thread.sleep(jitteredDelay);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new ArcaSoapException("Retry backoff interrupted", interruptedException);
                }
                delay *= 3;
            }
        }
    }

    private boolean checkCircuitState() throws ArcaSoapException {
        while (true) {
            State currentState = state.get();
            if (currentState == State.CLOSED) {
                return false;
            }
            if (currentState == State.HALF_OPEN) {
                throw new ArcaCircuitOpenException("Circuit Breaker is HALF_OPEN. A recovery probe is in progress.");
            }

            long elapsed = System.currentTimeMillis() - lastFailureTime.get();
            if (elapsed <= resetTimeoutMs) {
                throw new ArcaCircuitOpenException("Circuit Breaker is OPEN. Outage detected. Remaining cooldown: "
                        + (resetTimeoutMs - elapsed) + "ms");
            }
            if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                LOGGER.info("Circuit Breaker transitioned to HALF_OPEN. Testing next request.");
                return true;
            }
        }
    }

    private void onSuccess() {
        if (state.get() != State.CLOSED) {
            state.set(State.CLOSED);
            consecutiveFailures.set(0);
            LOGGER.info("Circuit Breaker transitioned to CLOSED. Remote services are healthy.");
        } else {
            consecutiveFailures.set(0);
        }
    }

    private void onFailure(ArcaSoapException exception) {
        if (isTransient(exception)) {
            int failures = consecutiveFailures.incrementAndGet();
            if (state.get() == State.HALF_OPEN || failures >= maxConsecutiveFailures) {
                synchronized (this) {
                    if (state.get() != State.OPEN) {
                        state.set(State.OPEN);
                        lastFailureTime.set(System.currentTimeMillis());
                        LOGGER.error("Circuit Breaker transitioned to OPEN due to {} consecutive failures. Outage detected. First error: {}",
                                failures, exception.getMessage());
                    }
                }
            }
        } else {
            consecutiveFailures.set(0);
            state.compareAndSet(State.HALF_OPEN, State.CLOSED);
        }
    }

    private static boolean isTransient(ArcaSoapException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static long calculateJitter(long delay) {
        double factor = 0.8 + Math.random() * 0.4;
        return (long) (delay * factor);
    }
}
