package io.github.fr4ncisx.arca.soap.internal.config;

import java.time.Duration;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SoapConfig}.
 * <p>
 * Verifies that the internal SOAP configuration is derived from {@link ArcaConfig}
 * and rejects timeout values that Metro cannot consume safely.
 *
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
class SoapConfigTest {

    private static final Cuit CUIT = new Cuit(20_333_333_334L);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration NEGATIVE_TIMEOUT = Duration.ofMillis(-1);
    private static final Duration OVERFLOW_TIMEOUT = Duration.ofMillis((long) Integer.MAX_VALUE + 1L);

    /**
     * Verifies that SoapConfig copies the effective timeout values from ArcaConfig.
     */
    @Test
    void fromCopiesTimeoutsFromArcaConfig() {
        ArcaConfig arcaConfig = arcaConfig(CONNECT_TIMEOUT, READ_TIMEOUT);

        SoapConfig soapConfig = SoapConfig.from(arcaConfig);

        assertThat(soapConfig.connectTimeout()).isEqualTo(CONNECT_TIMEOUT);
        assertThat(soapConfig.readTimeout()).isEqualTo(READ_TIMEOUT);
    }

    /**
     * Verifies that SoapConfig rejects a null source SDK configuration.
     */
    @Test
    void fromRejectsNullArcaConfig() {
        assertThatThrownBy(() -> SoapConfig.from(null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("config must not be null");
    }

    /**
     * Verifies that SoapConfig exposes timeout values as Metro-compatible integer milliseconds.
     */
    @Test
    void exposesTimeoutsAsIntegerMilliseconds() {
        SoapConfig soapConfig = new SoapConfig(CONNECT_TIMEOUT, READ_TIMEOUT);

        assertThat(soapConfig.connectTimeoutMillis()).isEqualTo(10_000);
        assertThat(soapConfig.readTimeoutMillis()).isEqualTo(30_000);
    }

    /**
     * Verifies that a null connection timeout is rejected.
     */
    @Test
    void rejectsNullConnectTimeout() {
        assertThatThrownBy(() -> new SoapConfig(null, READ_TIMEOUT))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("connectTimeout must not be null or negative");
    }

    /**
     * Verifies that a null read timeout is rejected.
     */
    @Test
    void rejectsNullReadTimeout() {
        assertThatThrownBy(() -> new SoapConfig(CONNECT_TIMEOUT, null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("readTimeout must not be null or negative");
    }

    /**
     * Verifies that a negative connection timeout is rejected.
     */
    @Test
    void rejectsNegativeConnectTimeout() {
        assertThatThrownBy(() -> new SoapConfig(NEGATIVE_TIMEOUT, READ_TIMEOUT))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("connectTimeout must not be null or negative");
    }

    /**
     * Verifies that a negative read timeout is rejected.
     */
    @Test
    void rejectsNegativeReadTimeout() {
        assertThatThrownBy(() -> new SoapConfig(CONNECT_TIMEOUT, NEGATIVE_TIMEOUT))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("readTimeout must not be null or negative");
    }

    /**
     * Verifies that zero timeouts are accepted because the shared SDK configuration allows them.
     */
    @Test
    void acceptsZeroTimeouts() {
        SoapConfig soapConfig = new SoapConfig(Duration.ZERO, Duration.ZERO);

        assertThat(soapConfig.connectTimeoutMillis()).isZero();
        assertThat(soapConfig.readTimeoutMillis()).isZero();
    }

    /**
     * Verifies that timeout values exceeding Metro integer milliseconds are rejected.
     */
    @Test
    void rejectsTimeoutsExceedingIntegerMilliseconds() {
        assertThatThrownBy(() -> new SoapConfig(OVERFLOW_TIMEOUT, READ_TIMEOUT))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("connectTimeout must not exceed " + Integer.MAX_VALUE + " milliseconds");

        assertThatThrownBy(() -> new SoapConfig(CONNECT_TIMEOUT, OVERFLOW_TIMEOUT))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("readTimeout must not exceed " + Integer.MAX_VALUE + " milliseconds");
    }

    private static ArcaConfig arcaConfig(Duration connectTimeout, Duration readTimeout) {
        return new ArcaConfig(CUIT, ArcaEnvironment.HOMOLOGACION, connectTimeout, readTimeout);
    }
}
