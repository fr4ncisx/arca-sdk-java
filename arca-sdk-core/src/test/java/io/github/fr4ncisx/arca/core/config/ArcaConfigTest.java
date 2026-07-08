package io.github.fr4ncisx.arca.core.config;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ArcaConfig}.
 * <p>
 * These tests verify the base immutable configuration contract used by SDK
 * clients. They cover valid creation, constructor validation, timeout
 * validation, and copy semantics for the {@code withXxx} methods.
 *
 * @author fr4ncisx
 * @since 0.1.0-M2
 */
@SuppressWarnings("null")
class ArcaConfigTest {

    private static final Cuit CUIT = new Cuit(20_333_333_334L);
    private static final ArcaEnvironment ENVIRONMENT = ArcaEnvironment.HOMOLOGACION;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration NEGATIVE_TIMEOUT = Duration.ofMillis(-1);

    /**
     * Verifies that a configuration can be created when all required fields are
     * present, and both timeout values are valid.
     */
    @Test
    void shouldCreateValidConfig() {
        ArcaConfig config = new ArcaConfig(
                CUIT,
                ENVIRONMENT,
                CONNECT_TIMEOUT,
                READ_TIMEOUT
        );

        assertThat(config.cuit()).isEqualTo(CUIT);
        assertThat(config.environment()).isEqualTo(ENVIRONMENT);
        assertThat(config.connectTimeout()).isEqualTo(CONNECT_TIMEOUT);
        assertThat(config.readTimeout()).isEqualTo(READ_TIMEOUT);
    }

    /**
     * Verifies that the canonical constructor rejects a null CUIT.
     */
    @Test
    void shouldRejectNullCuit() {
        assertThatThrownBy(() -> new ArcaConfig(
                null,
                ENVIRONMENT,
                CONNECT_TIMEOUT,
                READ_TIMEOUT
        ))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("cuit");
    }

    /**
     * Verifies that the canonical constructor rejects a null environment.
     */
    @Test
    void shouldRejectNullEnvironment() {
        assertThatThrownBy(() -> new ArcaConfig(
                CUIT,
                null,
                CONNECT_TIMEOUT,
                READ_TIMEOUT
        ))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("environment");
    }

    /**
     * Verifies that the canonical constructor rejects a null connection timeout.
     */
    @Test
    void shouldRejectNullConnectTimeout() {
        assertThatThrownBy(() -> new ArcaConfig(
                CUIT,
                ENVIRONMENT,
                null,
                READ_TIMEOUT
        ))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("connectTimeout");
    }

    /**
     * Verifies that the canonical constructor rejects a null read timeout.
     */
    @Test
    void shouldRejectNullReadTimeout() {
        assertThatThrownBy(() -> new ArcaConfig(
                CUIT,
                ENVIRONMENT,
                CONNECT_TIMEOUT,
                null
        ))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("readTimeout");
    }

    /**
     * Verifies that the canonical constructor rejects a negative connection
     * timeout.
     */
    @Test
    void shouldRejectNegativeConnectTimeout() {
        assertThatThrownBy(() -> new ArcaConfig(
                CUIT,
                ENVIRONMENT,
                NEGATIVE_TIMEOUT,
                READ_TIMEOUT
        ))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("connectTimeout");
    }

    /**
     * Verifies that the canonical constructor rejects a negative read timeout.
     */
    @Test
    void shouldRejectNegativeReadTimeout() {
        assertThatThrownBy(() -> new ArcaConfig(
                CUIT,
                ENVIRONMENT,
                CONNECT_TIMEOUT,
                NEGATIVE_TIMEOUT
        ))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("readTimeout");
    }

    /**
     * Verifies that zero is accepted as a connection timeout because the
     * contract only rejects null, or negative durations.
     */
    @Test
    void shouldAcceptZeroConnectTimeout() {
        ArcaConfig config = new ArcaConfig(
                CUIT,
                ENVIRONMENT,
                Duration.ZERO,
                READ_TIMEOUT
        );

        assertThat(config.connectTimeout()).isEqualTo(Duration.ZERO);
    }

    /**
     * Verifies that zero is accepted as a read timeout because the contract only
     * rejects null, or negative durations.
     */
    @Test
    void shouldAcceptZeroReadTimeout() {
        ArcaConfig config = new ArcaConfig(
                CUIT,
                ENVIRONMENT,
                CONNECT_TIMEOUT,
                Duration.ZERO
        );

        assertThat(config.readTimeout()).isEqualTo(Duration.ZERO);
    }

    /**
     * Verifies that {@link ArcaConfig#withConnectTimeout(Duration)} returns a
     * new configuration instance, replaces only the connection timeout, and
     * preserves CUIT, environment, and read timeout.
     */
    @Test
    void withConnectTimeoutShouldReturnNewInstanceReplacingOnlyConnectTimeout() {
        ArcaConfig original = validConfig();
        Duration newConnectTimeout = Duration.ofSeconds(20);

        ArcaConfig updated = original.withConnectTimeout(newConnectTimeout);

        assertThat(updated).isNotSameAs(original);

        assertThat(updated.cuit()).isEqualTo(original.cuit());
        assertThat(updated.environment()).isEqualTo(original.environment());
        assertThat(updated.connectTimeout()).isEqualTo(newConnectTimeout);
        assertThat(updated.readTimeout()).isEqualTo(original.readTimeout());

        assertThat(original.connectTimeout()).isEqualTo(CONNECT_TIMEOUT);
        assertThat(original.readTimeout()).isEqualTo(READ_TIMEOUT);
    }

    /**
     * Verifies that {@link ArcaConfig#withReadTimeout(Duration)} returns a new
     * configuration instance, replaces only the read timeout, and preserves CUIT,
     * environment, and connection timeout.
     */
    @Test
    void withReadTimeoutShouldReturnNewInstanceReplacingOnlyReadTimeout() {
        ArcaConfig original = validConfig();
        Duration newReadTimeout = Duration.ofSeconds(60);

        ArcaConfig updated = original.withReadTimeout(newReadTimeout);

        assertThat(updated).isNotSameAs(original);

        assertThat(updated.cuit()).isEqualTo(original.cuit());
        assertThat(updated.environment()).isEqualTo(original.environment());
        assertThat(updated.connectTimeout()).isEqualTo(original.connectTimeout());
        assertThat(updated.readTimeout()).isEqualTo(newReadTimeout);

        assertThat(original.connectTimeout()).isEqualTo(CONNECT_TIMEOUT);
        assertThat(original.readTimeout()).isEqualTo(READ_TIMEOUT);
    }

    /**
     * Verifies that {@link ArcaConfig#withConnectTimeout(Duration)} rejects a
     * null timeout before returning a copied configuration.
     */
    @Test
    void withConnectTimeoutShouldRejectNullTimeout() {
        ArcaConfig config = validConfig();

        assertThatThrownBy(() -> config.withConnectTimeout(null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("connectTimeout");
    }

    /**
     * Verifies that {@link ArcaConfig#withConnectTimeout(Duration)} rejects a
     * negative timeout before returning a copied configuration.
     */
    @Test
    void withConnectTimeoutShouldRejectNegativeTimeout() {
        ArcaConfig config = validConfig();

        assertThatThrownBy(() -> config.withConnectTimeout(NEGATIVE_TIMEOUT))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("connectTimeout");
    }

    /**
     * Verifies that {@link ArcaConfig#withReadTimeout(Duration)} rejects a null
     * timeout before returning a copied configuration.
     */
    @Test
    void withReadTimeoutShouldRejectNullTimeout() {
        ArcaConfig config = validConfig();

        assertThatThrownBy(() -> config.withReadTimeout(null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("readTimeout");
    }

    /**
     * Verifies that {@link ArcaConfig#withReadTimeout(Duration)} rejects a
     * negative timeout before returning a copied configuration.
     */
    @Test
    void withReadTimeoutShouldRejectNegativeTimeout() {
        ArcaConfig config = validConfig();

        assertThatThrownBy(() -> config.withReadTimeout(NEGATIVE_TIMEOUT))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("readTimeout");
    }

    /**
     * Creates a valid configuration fixture used by copy, and validation tests.
     *
     * @return a valid immutable ARCA configuration.
     */
    private static ArcaConfig validConfig() {
        return new ArcaConfig(
                CUIT,
                ENVIRONMENT,
                CONNECT_TIMEOUT,
                READ_TIMEOUT
        );
    }

    /**
     * Verifies that the configuration can be created for every supported ARCA
     * environment.
     *
     * @param environment supported ARCA environment.
     */
    @ParameterizedTest
    @EnumSource(ArcaEnvironment.class)
    void shouldCreateConfigForEverySupportedEnvironment(ArcaEnvironment environment) {
        ArcaConfig config = new ArcaConfig(
                CUIT,
                environment,
                CONNECT_TIMEOUT,
                READ_TIMEOUT
        );
        assertThat(config.environment()).isEqualTo(environment);
    }
}
