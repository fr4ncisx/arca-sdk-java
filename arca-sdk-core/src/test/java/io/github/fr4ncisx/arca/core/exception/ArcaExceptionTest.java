package io.github.fr4ncisx.arca.core.exception;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the {@link ArcaException} sealed hierarchy.
 * <p>
 * Verifies that the sealed hierarchy permits exactly three subclasses,
 * all subclasses are non-sealed, and exception message/cause behavior
 * works correctly for each subclass.
 *
 * @author fr4ncisx
 * @since 0.1.0-M1
 */
class ArcaExceptionTest {

    /**
     * Validates that ArcaException is declared as a sealed class.
     */
    @Test
    void arcaExceptionIsSealed() {
        assertThat(ArcaException.class.isSealed()).isTrue();
    }

    /**
     * Validates that the sealed hierarchy permits exactly three subclasses:
     * ArcaAuthException, ArcaSoapException, and ArcaValidationException.
     */
    @Test
    void sealedHierarchyPermitsExactlyThreeSubclasses() {
        var permitted = ArcaException.class.getPermittedSubclasses();

        assertThat(permitted)
            .isNotNull()
            .hasSize(3)
            .containsExactlyInAnyOrder(
                ArcaAuthException.class,
                ArcaSoapException.class,
                ArcaValidationException.class);
    }

    /**
     * Validates that ArcaAuthException can be created with a message only,
     * and that the message is accessible via getMessage().
     */
    @Test
    void arcaAuthExceptionWithMessage() {
        var ex = new ArcaAuthException("login failed");

        assertThat(ex.getMessage()).isEqualTo("login failed");
        assertThat(ex.getCause()).isNull();
        assertThat(ex).isInstanceOf(ArcaException.class);
    }

    /**
     * Validates that ArcaAuthException can be created with a message and cause,
     * and that both are accessible.
     */
    @Test
    void arcaAuthExceptionWithMessageAndCause() {
        var cause = new RuntimeException("root");
        var ex = new ArcaAuthException("login failed", cause);

        assertThat(ex.getMessage()).isEqualTo("login failed");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex).isInstanceOf(ArcaException.class);
    }

    /**
     * Validates that ArcaSoapException can be created with a message only,
     * and that the message is accessible via getMessage().
     */
    @Test
    void arcaSoapExceptionWithMessage() {
        var ex = new ArcaSoapException("timeout");

        assertThat(ex.getMessage()).isEqualTo("timeout");
        assertThat(ex.getCause()).isNull();
        assertThat(ex).isInstanceOf(ArcaException.class);
    }

    /**
     * Validates that ArcaSoapException can be created with a message and cause,
     * and that both are accessible.
     */
    @Test
    void arcaSoapExceptionWithMessageAndCause() {
        var cause = new RuntimeException("connection reset");
        var ex = new ArcaSoapException("timeout", cause);

        assertThat(ex.getMessage()).isEqualTo("timeout");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex).isInstanceOf(ArcaException.class);
    }

    /**
     * Validates that ArcaValidationException can be created with a message only,
     * and that the message is accessible via getMessage().
     */
    @Test
    void arcaValidationExceptionWithMessage() {
        var ex = new ArcaValidationException("cuit is null");

        assertThat(ex.getMessage()).isEqualTo("cuit is null");
        assertThat(ex.getCause()).isNull();
        assertThat(ex).isInstanceOf(ArcaException.class);
    }

    /**
     * Validates that ArcaValidationException can be created with a message and cause,
     * and that both are accessible.
     */
    @Test
    void arcaValidationExceptionWithMessageAndCause() {
        var cause = new IllegalArgumentException("bad format");
        var ex = new ArcaValidationException("cuit is null", cause);

        assertThat(ex.getMessage()).isEqualTo("cuit is null");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex).isInstanceOf(ArcaException.class);
    }

    /**
     * Validates that all three permitted subclasses are declared as non-sealed,
     * allowing further extension outside the sealed hierarchy.
     */
    @Test
    void allSubclassesAreNonSealed() {
        assertThat(ArcaAuthException.class.isSealed()).isFalse();
        assertThat(ArcaSoapException.class.isSealed()).isFalse();
        assertThat(ArcaValidationException.class.isSealed()).isFalse();
    }

    /**
     * Validates that static message strings used in tests do not accidentally
     * contain sensitive data like tokens or credentials.
     */
    @Test
    void noSensitiveDataInStaticMessages() {
        var fakeToken = Base64.getEncoder().encodeToString(new byte[64]);
        var messages = new String[] {
            "login failed",
            "timeout",
            "cuit is null",
            "connection reset",
            "bad format",
        };

        for (var msg : messages) {
            assertThat(msg.length()).isLessThan(50);
            assertThat(msg).doesNotContain(fakeToken.substring(0, 20));
        }
    }

    @Test
    void defaultErrorCodesForSubclasses() {
        assertThat(new ArcaAuthException("err").errorCode()).isEqualTo(ArcaErrorCode.AUTHFAILED);
        assertThat(new ArcaSoapException("err").errorCode()).isEqualTo(ArcaErrorCode.SOAPFAULT);
        assertThat(new ArcaValidationException("err").errorCode()).isEqualTo(ArcaErrorCode.VALIDATIONERROR);
    }

    @Test
    void explicitErrorCodeForSoapException() {
        var ex = new ArcaSoapException(ArcaErrorCode.SOAPTIMEOUT, "connection timeout");
        assertThat(ex.errorCode()).isEqualTo(ArcaErrorCode.SOAPTIMEOUT);
        assertThat(ex.getMessage()).isEqualTo("connection timeout");
    }

    @Test
    void metadataValidationAndSanitization() {
        var exNullMeta = new ArcaValidationException("err", (Map<String, String>) null);
        assertThat(exNullMeta.metadata()).isNotNull().isEmpty();

        Map<String, String> badMeta1 = new HashMap<>();
        badMeta1.put(null, "val");
        assertThatThrownBy(() -> new ArcaValidationException("err", badMeta1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metadata key cannot be null, empty, or blank");

        Map<String, String> badMeta2 = new HashMap<>();
        badMeta2.put(" ", "val");
        assertThatThrownBy(() -> new ArcaValidationException("err", badMeta2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metadata key cannot be null, empty, or blank");

        Map<String, String> badMeta3 = new HashMap<>();
        badMeta3.put("key", null);
        assertThatThrownBy(() -> new ArcaValidationException("err", badMeta3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metadata value for key 'key' cannot be null");

        Map<String, String> sensitiveMeta = Map.of(
            "normalKey", "normalValue",
            "password", "secretPassword123"
        );
        var exSanitized = new ArcaValidationException("err", sensitiveMeta);
        assertThat(exSanitized.metadata().get("normalKey")).isEqualTo("normalValue");
        assertThat(exSanitized.metadata().get("password")).isEqualTo("[REDACTED]");
    }
}
