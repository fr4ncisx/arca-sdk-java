package io.github.fr4ncisx.arca.core.config;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;

import java.time.Duration;

/**
 * Central configuration for the ARCA SDK.
 * <p>
 * Holds the taxpayer CUIT, target environment, and network timeout settings.
 * All values are immutable.
 * <p>
 * Consumed by WSAA and WSFEv1 adapters to establish connection parameters.
 *
 * @param cuit taxpayer CUIT used by the SDK clients.
 * @param environment target ARCA environment.
 * @param connectTimeout maximum time allowed establishing a network connection.
 * @param readTimeout maximum time allowed waiting for response data.
 *
 * @author fr4ncisx
 * @since 0.1.0-M2
 */
public record ArcaConfig(Cuit cuit,
                         ArcaEnvironment environment,
                         Duration connectTimeout,
                         Duration readTimeout) {

    public ArcaConfig {
        if (cuit == null)
            throw new ArcaValidationException("cuit must not be null");
        if (environment == null)
            throw new ArcaValidationException("environment must not be null");
        if (connectTimeout == null || connectTimeout.isNegative())
            throw new ArcaValidationException(
                    "connectTimeout must not be null or negative");
        if (readTimeout == null || readTimeout.isNegative())
            throw new ArcaValidationException(
                    "readTimeout must not be null or negative");
    }

    /**
     * Returns a new configuration instance with the provided connection timeout.
     * <p>
     * The original instance remains unchanged. The returned instance preserves
     * the CUIT, environment, and read timeout values.
     *
     * @param timeout new connection timeout value.
     * @return a new configuration instance with the updated connection timeout.
     * @throws ArcaValidationException if timeout is null or negative.
     */
    public ArcaConfig withConnectTimeout(Duration timeout) {
        validateTimeout(timeout, "connectTimeout");
        return new ArcaConfig(cuit, environment, timeout, readTimeout);
    }

    /**
     * Returns a new configuration instance with the provided read timeout.
     * <p>
     * The original instance remains unchanged. The returned instance preserves
     * the CUIT, environment, and connection timeout values.
     *
     * @param timeout new read timeout value.
     * @return a new configuration instance with the updated read timeout.
     * @throws ArcaValidationException if timeout is null or negative.
     */
    public ArcaConfig withReadTimeout(Duration timeout) {
        validateTimeout(timeout, "readTimeout");
        return new ArcaConfig(cuit, environment, connectTimeout, timeout);
    }

    private static void validateTimeout(Duration timeout, String fieldName) {
        if (timeout == null || timeout.isNegative()) {
            throw new ArcaValidationException(fieldName + " must not be null or negative");
        }
    }
}
