package io.github.fr4ncisx.arca.core.config;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates construction and fail-fast validation rules of {@link ResilienceConfig}.
 *
 * @author fr4ncisx
 * @since 1.3.0
 */
class ResilienceConfigTest {

    @Test
    void defaultInstanceShouldHaveProductionDefaults() {
        ResilienceConfig config = ResilienceConfig.DEFAULT;

        assertThat(config.maxAttempts()).isEqualTo(3);
        assertThat(config.baseDelay()).isEqualTo(Duration.ofMillis(500));
        assertThat(config.maxConsecutiveFailures()).isEqualTo(5);
        assertThat(config.resetTimeout()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void shouldCreateCustomInstanceWithValidParameters() {
        ResilienceConfig config = new ResilienceConfig(5, Duration.ofSeconds(1), 10, Duration.ofSeconds(30));

        assertThat(config.maxAttempts()).isEqualTo(5);
        assertThat(config.baseDelay()).isEqualTo(Duration.ofSeconds(1));
        assertThat(config.maxConsecutiveFailures()).isEqualTo(10);
        assertThat(config.resetTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void shouldRejectInvalidMaxAttempts() {
        assertThatThrownBy(() -> new ResilienceConfig(0, Duration.ofMillis(500), 5, Duration.ofSeconds(60)))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("maxAttempts must be greater than 0");

        assertThatThrownBy(() -> new ResilienceConfig(-1, Duration.ofMillis(500), 5, Duration.ofSeconds(60)))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("maxAttempts must be greater than 0");
    }

    @Test
    void shouldRejectInvalidBaseDelay() {
        assertThatThrownBy(() -> new ResilienceConfig(3, null, 5, Duration.ofSeconds(60)))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("baseDelay must not be null or negative");

        assertThatThrownBy(() -> new ResilienceConfig(3, Duration.ofMillis(-100), 5, Duration.ofSeconds(60)))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("baseDelay must not be null or negative");
    }

    @Test
    void shouldRejectInvalidMaxConsecutiveFailures() {
        assertThatThrownBy(() -> new ResilienceConfig(3, Duration.ofMillis(500), 0, Duration.ofSeconds(60)))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("maxConsecutiveFailures must be greater than 0");

        assertThatThrownBy(() -> new ResilienceConfig(3, Duration.ofMillis(500), -2, Duration.ofSeconds(60)))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("maxConsecutiveFailures must be greater than 0");
    }

    @Test
    void shouldRejectInvalidResetTimeout() {
        assertThatThrownBy(() -> new ResilienceConfig(3, Duration.ofMillis(500), 5, null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("resetTimeout must not be null or negative");

        assertThatThrownBy(() -> new ResilienceConfig(3, Duration.ofMillis(500), 5, Duration.ofSeconds(-1)))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("resetTimeout must not be null or negative");
    }
}
