package io.github.fr4ncisx.arca.soap.internal.config;

import java.time.Duration;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.internal.security.SoapHardeningUtil;

/**
 * Internal SOAP transport configuration derived from the SDK configuration.
 * <p>
 * The record centralizes the effective timeout values required by the SOAP
 * infrastructure adapter. Concrete SOAP runtime details remain inside internal
 * SOAP packages and are not exposed through the public SDK API.
 * @param connectTimeout maximum time allowed to establish a SOAP connection.
 * @param readTimeout maximum time allowed to wait for SOAP response data.
 * @param resilienceEnabled whether retry and circuit breaker logic is enabled.
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
public record SoapConfig(Duration connectTimeout, Duration readTimeout, boolean resilienceEnabled) {

    static {
        SoapHardeningUtil.apply();
    }

    private static final String NULL_CONFIG = "config must not be null";
    private static final String CONNECT_TIMEOUT = "connectTimeout";
    private static final String READ_TIMEOUT = "readTimeout";

    /**
     * Creates an internal SOAP configuration with resilience enabled by default.
     *
     * @param connectTimeout maximum time allowed to establish a SOAP connection.
     * @param readTimeout maximum time allowed to wait for SOAP response data.
     */
    public SoapConfig(Duration connectTimeout, Duration readTimeout) {
        this(connectTimeout, readTimeout, true);
    }

    /**
     * Creates an internal SOAP configuration.
     *
     * @param connectTimeout maximum time allowed to establish a SOAP connection.
     * @param readTimeout maximum time allowed to wait for SOAP response data.
     * @param resilienceEnabled whether retry and circuit breaker logic is enabled.
     * @throws ArcaValidationException if a timeout is null, negative, or exceeds the
     *                                 Metro integer millisecond limit.
     */
    public SoapConfig {
        validateTimeout(connectTimeout, CONNECT_TIMEOUT);
        validateTimeout(readTimeout, READ_TIMEOUT);
    }

    /**
     * Creates a SOAP configuration from the public SDK configuration.
     *
     * @param config source SDK configuration.
     * @return SOAP configuration using the effective SDK timeouts.
     * @throws ArcaValidationException if config is null.
     */
    public static SoapConfig from(ArcaConfig config) {
        if (config == null) {
            throw new ArcaValidationException(NULL_CONFIG);
        }
        return new SoapConfig(config.connectTimeout(), config.readTimeout(), config.resilienceEnabled());
    }

    /**
     * Returns the connection timeout in integer milliseconds accepted by Metro.
     *
     * @return connection timeout in milliseconds.
     */
    public int connectTimeoutMillis() {
        return toMillis(connectTimeout, CONNECT_TIMEOUT);
    }

    /**
     * Returns the read timeout in integer milliseconds accepted by Metro.
     *
     * @return read timeout in milliseconds.
     */
    public int readTimeoutMillis() {
        return toMillis(readTimeout, READ_TIMEOUT);
    }

    private static void validateTimeout(Duration timeout, String fieldName) {
        if (timeout == null || timeout.isNegative()) {
            throw new ArcaValidationException(fieldName + " must not be null or negative");
        }
        toMillis(timeout, fieldName);
    }

    private static int toMillis(Duration timeout, String fieldName) {
        long millis = timeout.toMillis();
        if (millis > Integer.MAX_VALUE) {
            throw new ArcaValidationException(fieldName + " must not exceed " + Integer.MAX_VALUE + " milliseconds");
        }
        return Math.toIntExact(millis);
    }
}
