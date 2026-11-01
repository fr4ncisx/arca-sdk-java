package io.github.fr4ncisx.arca.core.config;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.time.Duration;

/**
 * Immutable configuration parameters for SOAP transport resilience.
 * <p>
 * Controls retry policies, exponential backoff, and circuit breaker trip thresholds.
 *
 * @param maxAttempts maximum invocations including the initial attempt.
 * @param baseDelay initial exponential backoff delay.
 * @param maxConsecutiveFailures consecutive failures allowed before opening the circuit.
 * @param resetTimeout cooldown period before attempting circuit breaker reset.
 * @author fr4ncisx
 * @since 1.3.0
 */
public record ResilienceConfig(
        int maxAttempts,
        Duration baseDelay,
        int maxConsecutiveFailures,
        Duration resetTimeout
) {

    /**
     * Standard production resilience defaults.
     */
    public static final ResilienceConfig DEFAULT = new ResilienceConfig(
            3,
            Duration.ofMillis(500),
            5,
            Duration.ofSeconds(60)
    );

    /**
     * Validates that all resilience parameters are positive and non-null.
     *
     * @throws ArcaValidationException if any parameter violates validation constraints.
     */
    public ResilienceConfig {
        if (maxAttempts <= 0) {
            throw new ArcaValidationException("maxAttempts must be greater than 0");
        }
        if (baseDelay == null || baseDelay.isNegative()) {
            throw new ArcaValidationException("baseDelay must not be null or negative");
        }
        if (maxConsecutiveFailures <= 0) {
            throw new ArcaValidationException("maxConsecutiveFailures must be greater than 0");
        }
        if (resetTimeout == null || resetTimeout.isNegative()) {
            throw new ArcaValidationException("resetTimeout must not be null or negative");
        }
    }
}
