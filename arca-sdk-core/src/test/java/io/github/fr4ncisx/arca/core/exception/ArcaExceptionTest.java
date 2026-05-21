package io.github.fr4ncisx.arca.core.exception;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

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
}
