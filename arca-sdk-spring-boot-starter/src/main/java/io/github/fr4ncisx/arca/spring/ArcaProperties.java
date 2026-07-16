package io.github.fr4ncisx.arca.spring;

import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
    @Nullable String certificatePassword
) {}
