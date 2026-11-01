package io.github.fr4ncisx.arca.spring;

import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.config.ResilienceConfig;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;

import java.time.Duration;

/**
 * Configuration properties for ARCA SDK integration.
 * <p>
 * These properties are mapped using the {@code arca} prefix.
 *
 * @param cuit                 the taxpayer CUIT.
 * @param environment          the target ARCA environment.
 * @param connectTimeout       the connection timeout duration.
 * @param readTimeout          the read timeout duration.
 * @param resilienceEnabled   whether SOAP resilience features are enabled.
 * @param certificateLocation the certificate file location (resource).
 * @param certificatePassword the certificate password.
 * @param resilience          configurable resilience parameters for retries and circuit breaker.
 * @author fr4ncisx
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "arca")
public record ArcaProperties(
    @Nullable String cuit,
    @DefaultValue("HOMOLOGACION") ArcaEnvironment environment,
    @DefaultValue("10s") Duration connectTimeout,
    @DefaultValue("30s") Duration readTimeout,
    @DefaultValue("true") boolean resilienceEnabled,
    @Nullable Resource certificateLocation,
    @Nullable String certificatePassword,
    @Nullable ResilienceProperties resilience
) {

    /**
     * Compact constructor annotated with {@link ConstructorBinding} for Spring Boot property binding.
     */
    @ConstructorBinding
    public ArcaProperties {
    }

    /**
     * Preserves binary and source compatibility with caller code compiled against 1.2.x.
     *
     * @param cuit the taxpayer CUIT.
     * @param environment the target ARCA environment.
     * @param connectTimeout the connection timeout duration.
     * @param readTimeout the read timeout duration.
     * @param resilienceEnabled whether SOAP resilience features are enabled.
     * @param certificateLocation the certificate file location (resource).
     * @param certificatePassword the certificate password.
     */
    public ArcaProperties(
        @Nullable String cuit,
        ArcaEnvironment environment,
        Duration connectTimeout,
        Duration readTimeout,
        boolean resilienceEnabled,
        @Nullable Resource certificateLocation,
        @Nullable String certificatePassword
    ) {
        this(cuit, environment, connectTimeout, readTimeout, resilienceEnabled, certificateLocation, certificatePassword, null);
    }

    /**
     * Configuration properties for SOAP transport resilience.
     *
     * @param maxAttempts maximum invocations including the initial attempt.
     * @param baseDelay initial exponential backoff delay.
     * @param maxConsecutiveFailures consecutive failures allowed before opening the circuit.
     * @param resetTimeout cooldown period before attempting circuit breaker reset.
     * @author fr4ncisx
     * @since 1.3.0
     */
    public record ResilienceProperties(
        @DefaultValue("3") int maxAttempts,
        @DefaultValue("500ms") Duration baseDelay,
        @DefaultValue("5") int maxConsecutiveFailures,
        @DefaultValue("60s") Duration resetTimeout
    ) {

        /**
         * Converts this property container into an immutable domain {@link ResilienceConfig}.
         *
         * @return domain resilience configuration.
         */
        public ResilienceConfig toConfig() {
            return new ResilienceConfig(maxAttempts, baseDelay, maxConsecutiveFailures, resetTimeout);
        }
    }
}
