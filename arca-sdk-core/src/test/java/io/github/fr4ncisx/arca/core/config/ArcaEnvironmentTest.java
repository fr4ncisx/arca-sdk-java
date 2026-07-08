package io.github.fr4ncisx.arca.core.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Modifier;
import java.net.URI;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ArcaEnvironment} enum endpoints and immutability.
 * <p>
 * Verifies that both homologacion and production environments expose the
 * correct official WSAA and WSFEv1 URLs, enum fields are final, and
 * serialization returns the canonical enum instance.
 *
 * @author fr4ncisx
 * @since 0.1.0-M1
 */
@SuppressWarnings("null")
class ArcaEnvironmentTest {

    /**
     * Validates that the enum contains exactly two values: HOMOLOGACION and PRODUCCION.
     */
    @Test
    void enumContainsExactlyTwoValues() {
        assertThat(ArcaEnvironment.values()).hasSize(2);
    }

    /**
     * Validates that HOMOLOGACION exposes the correct official WSAA endpoint URL.
     */
    @Test
    void homologacionWsaaUrlMatchesOfficialEndpoint() {
        var expected = URI.create("https://wsaahomo.afip.gov.ar/ws/services/LoginCms");
        assertThat(ArcaEnvironment.HOMOLOGACION.getWsaaUrl()).isEqualTo(expected);
    }

    /**
     * Validates that HOMOLOGACION exposes the correct official WSFEv1 endpoint URL.
     */
    @Test
    void homologacionWsfev1UrlMatchesOfficialEndpoint() {
        var expected = URI.create("https://wswhomo.afip.gov.ar/wsfev1/service.asmx");
        assertThat(ArcaEnvironment.HOMOLOGACION.getWsfev1Url()).isEqualTo(expected);
    }

    /**
     * Validates that PRODUCCION exposes the correct official WSAA endpoint URL.
     */
    @Test
    void produccionWsaaUrlMatchesOfficialEndpoint() {
        var expected = URI.create("https://wsaa.afip.gov.ar/ws/services/LoginCms");
        assertThat(ArcaEnvironment.PRODUCCION.getWsaaUrl()).isEqualTo(expected);
    }

    /**
     * Validates that PRODUCCION exposes the correct official WSFEv1 endpoint URL.
     */
    @Test
    void produccionWsfev1UrlMatchesOfficialEndpoint() {
        var expected = URI.create("https://servicios1.afip.gov.ar/wsfev1/service.asmx");
        assertThat(ArcaEnvironment.PRODUCCION.getWsfev1Url()).isEqualTo(expected);
    }

    /**
     * Validates that the enum fields are declared as final to prevent reassignment.
     */
    @Test
    void enumFieldsAreFinal() throws NoSuchFieldException {
        var wsaaField = ArcaEnvironment.class.getDeclaredField("wsaaUrl");
        var wsfev1Field = ArcaEnvironment.class.getDeclaredField("wsfev1Url");

        assertThat(Modifier.isFinal(wsaaField.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(wsfev1Field.getModifiers())).isTrue();
    }

    /**
     * Validates that serializing and deserializing an enum value returns the
     * same canonical instance, confirming enum serialization safety.
     */
    @Test
    void enumIsImmutableUnderSerialisation() throws Exception {
        var original = ArcaEnvironment.HOMOLOGACION;

        var baos = new ByteArrayOutputStream();
        try (var oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        var bais = new ByteArrayInputStream(baos.toByteArray());
        try (var ois = new ObjectInputStream(bais)) {
            var deserialized = (ArcaEnvironment) ois.readObject();
            assertThat(deserialized).isSameAs(original);
        }
    }

    /**
     * Validates that all enum values return non-null URLs for both WSAA and WSFEv1.
     */
    @Test
    void enumValuesAreNonNull() {
        for (var env : ArcaEnvironment.values()) {
            assertThat(env.getWsaaUrl()).isNotNull();
            assertThat(env.getWsfev1Url()).isNotNull();
        }
    }
}
