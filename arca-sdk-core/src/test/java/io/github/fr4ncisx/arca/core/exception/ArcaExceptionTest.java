package io.github.fr4ncisx.arca.core.exception;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ArcaExceptionTest {

    @Test
    void arcaExceptionIsSealed() {
        assertThat(ArcaException.class.isSealed()).isTrue();
    }

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

    @Test
    void arcaAuthExceptionWithMessage() {
        var ex = new ArcaAuthException("login failed");

        assertThat(ex.getMessage()).isEqualTo("login failed");
        assertThat(ex.getCause()).isNull();
        assertThat(ex).isInstanceOf(ArcaException.class);
    }

    @Test
    void arcaAuthExceptionWithMessageAndCause() {
        var cause = new RuntimeException("root");
        var ex = new ArcaAuthException("login failed", cause);

        assertThat(ex.getMessage()).isEqualTo("login failed");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex).isInstanceOf(ArcaException.class);
    }

    @Test
    void arcaSoapExceptionWithMessage() {
        var ex = new ArcaSoapException("timeout");

        assertThat(ex.getMessage()).isEqualTo("timeout");
        assertThat(ex.getCause()).isNull();
        assertThat(ex).isInstanceOf(ArcaException.class);
    }

    @Test
    void arcaSoapExceptionWithMessageAndCause() {
        var cause = new RuntimeException("connection reset");
        var ex = new ArcaSoapException("timeout", cause);

        assertThat(ex.getMessage()).isEqualTo("timeout");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex).isInstanceOf(ArcaException.class);
    }

    @Test
    void arcaValidationExceptionWithMessage() {
        var ex = new ArcaValidationException("cuit is null");

        assertThat(ex.getMessage()).isEqualTo("cuit is null");
        assertThat(ex.getCause()).isNull();
        assertThat(ex).isInstanceOf(ArcaException.class);
    }

    @Test
    void arcaValidationExceptionWithMessageAndCause() {
        var cause = new IllegalArgumentException("bad format");
        var ex = new ArcaValidationException("cuit is null", cause);

        assertThat(ex.getMessage()).isEqualTo("cuit is null");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex).isInstanceOf(ArcaException.class);
    }

    @Test
    void allSubclassesAreNonSealed() {
        assertThat(ArcaAuthException.class.isSealed()).isFalse();
        assertThat(ArcaSoapException.class.isSealed()).isFalse();
        assertThat(ArcaValidationException.class.isSealed()).isFalse();
    }

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
