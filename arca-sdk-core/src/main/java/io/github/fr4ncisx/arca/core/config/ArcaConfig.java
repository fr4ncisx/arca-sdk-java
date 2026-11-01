package io.github.fr4ncisx.arca.core.config;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Central configuration for the ARCA SDK.
 * <p>
 * Holds the taxpayer CUIT, target environment, network timeout settings,
 * and optional ticket cache directory for WSAA session persistence.
 * All values are immutable.
 * <p>
 * Consumed by WSAA and WSFEv1 adapters to establish connection parameters.
 *
 * @param cuit taxpayer CUIT used by the SDK clients.
 * @param environment target ARCA environment.
 * @param connectTimeout maximum time allowed establishing a network connection; zero disables the limit.
 * @param readTimeout maximum time allowed waiting for response data; zero disables the limit.
 * @param resilienceEnabled whether resilience (retries/circuit breaker) is enabled.
 * @param ticketCacheDir optional root directory for per-environment and per-CUIT WSAA ticket caches.
 *
 * @author fr4ncisx
 * @since 0.1.0-M2
 */
public record ArcaConfig(Cuit cuit,
                         ArcaEnvironment environment,
                         Duration connectTimeout,
                         Duration readTimeout,
                         boolean resilienceEnabled,
                         @Nullable Path ticketCacheDir,
                         ResilienceConfig resilienceConfig) {

    /**
     * Creates an ArcaConfig with default resilience parameters.
     *
     * @param cuit taxpayer CUIT.
     * @param environment target environment.
     * @param connectTimeout connection timeout; zero disables the limit.
     * @param readTimeout read timeout; zero disables the limit.
     * @param resilienceEnabled whether resilience is enabled.
     * @param ticketCacheDir optional ticket cache directory.
     * @since 0.1.0-M2
     */
    public ArcaConfig(Cuit cuit, ArcaEnvironment environment, Duration connectTimeout, Duration readTimeout,
                      boolean resilienceEnabled, @Nullable Path ticketCacheDir) {
        this(cuit, environment, connectTimeout, readTimeout, resilienceEnabled, ticketCacheDir, ResilienceConfig.DEFAULT);
    }

    /**
     * Creates an ArcaConfig with resilience enabled and no persistent ticket cache.
     *
     * @param cuit taxpayer CUIT.
     * @param environment target environment.
     * @param connectTimeout connection timeout; zero disables the limit.
     * @param readTimeout read timeout; zero disables the limit.
     */
    public ArcaConfig(Cuit cuit, ArcaEnvironment environment, Duration connectTimeout, Duration readTimeout) {
        this(cuit, environment, connectTimeout, readTimeout, true, null);
    }

    /**
     * Creates an ArcaConfig with resilience enabled by default.
     *
     * @param cuit taxpayer CUIT.
     * @param environment target environment.
     * @param connectTimeout connection timeout; zero disables the limit.
     * @param readTimeout read timeout; zero disables the limit.
     * @param resilienceEnabled whether resilience is enabled.
     */
    public ArcaConfig(Cuit cuit, ArcaEnvironment environment, Duration connectTimeout, Duration readTimeout,
                      boolean resilienceEnabled) {
        this(cuit, environment, connectTimeout, readTimeout, resilienceEnabled, null);
    }

    /**
     * Validates all fields at construction time.
     *
     * @throws ArcaValidationException if {@code cuit} or {@code environment} is null,
     *         or if {@code connectTimeout} or {@code readTimeout} is null or negative.
     */
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
        if (resilienceConfig == null)
            throw new ArcaValidationException("resilienceConfig must not be null");
    }

    /**
     * Returns a new configuration instance with the provided connection timeout.
     * <p>
     * The original instance remains unchanged. The returned instance preserves
     * the CUIT, environment, read timeout, resilience, and ticket cache settings.
     *
     * @param timeout new connection timeout value; zero disables the limit.
     * @return a new configuration instance with the updated connection timeout.
     * @throws ArcaValidationException if timeout is null or negative.
     */
    public ArcaConfig withConnectTimeout(Duration timeout) {
        validateTimeout(timeout, "connectTimeout");
        return new ArcaConfig(cuit, environment, timeout, readTimeout, resilienceEnabled, ticketCacheDir, resilienceConfig);
    }

    /**
     * Returns a new configuration instance with the provided read timeout.
     * <p>
     * The original instance remains unchanged. The returned instance preserves
     * the CUIT, environment, connection timeout, resilience, and ticket cache settings.
     *
     * @param timeout new read timeout value; zero disables the limit.
     * @return a new configuration instance with the updated read timeout.
     * @throws ArcaValidationException if timeout is null or negative.
     */
    public ArcaConfig withReadTimeout(Duration timeout) {
        validateTimeout(timeout, "readTimeout");
        return new ArcaConfig(cuit, environment, connectTimeout, timeout, resilienceEnabled, ticketCacheDir, resilienceConfig);
    }

    /**
     * Returns a new configuration instance with the resilience setting updated.
     * <p>
     * The original instance remains unchanged.
     *
     * @param enabled whether resilience (retries/circuit breaker) should be enabled.
     * @return a new configuration instance with the updated resilience setting.
     * @since 1.1.0
     */
    public ArcaConfig withResilienceEnabled(boolean enabled) {
        return new ArcaConfig(cuit, environment, connectTimeout, readTimeout, enabled, ticketCacheDir, resilienceConfig);
    }

    /**
     * Returns a new configuration instance with the provided ticket cache directory.
     * <p>
     * The original instance remains unchanged. When a non-null directory is provided,
     * WSAA access tickets are persisted below per-environment and per-CUIT subdirectories
     * and reused across application restarts.
     *
     * @param directory the root directory for scoped ticket caches, or null to disable persistence.
     * @return a new configuration instance with the updated ticket cache directory.
     * @since 1.2.0
     */
    public ArcaConfig withTicketCacheDir(@Nullable Path directory) {
        return new ArcaConfig(cuit, environment, connectTimeout, readTimeout, resilienceEnabled, directory, resilienceConfig);
    }

    /**
     * Returns a new configuration instance with the resilience parameters updated.
     * <p>
     * The original instance remains unchanged.
     *
     * @param config the new resilience configuration.
     * @return a new configuration instance with the updated resilience configuration.
     * @throws ArcaValidationException if config is null.
     * @since 1.3.0
     */
    public ArcaConfig withResilienceConfig(ResilienceConfig config) {
        if (config == null) {
            throw new ArcaValidationException("resilienceConfig must not be null");
        }
        return new ArcaConfig(cuit, environment, connectTimeout, readTimeout, resilienceEnabled, ticketCacheDir, config);
    }

    private static void validateTimeout(Duration timeout, String fieldName) {
        if (timeout == null || timeout.isNegative()) {
            throw new ArcaValidationException(fieldName + " must not be null or negative");
        }
    }
}
