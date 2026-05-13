package io.github.fr4ncisx.arca.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Modifier;
import java.net.URI;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ArcaEnvironment enum.
 * Verifies immutability, correct endpoint values, and serialisation safety.
 */
class ArcaEnvironmentTest {

    @Test
    void enumContainsExactlyTwoValues() {
        assertThat(ArcaEnvironment.values()).hasSize(2);
    }

    @Test
    void homologacionWsaaUrlMatchesOfficialEndpoint() {
        var expected = URI.create("https://wsaahomo.afip.gov.ar/ws/services/LoginCms");
        assertThat(ArcaEnvironment.HOMOLOGACION.getWsaaUrl()).isEqualTo(expected);
    }

    @Test
    void homologacionWsfev1UrlMatchesOfficialEndpoint() {
        var expected = URI.create("https://wswhomo.afip.gov.ar/wsfev1/service.asmx");
        assertThat(ArcaEnvironment.HOMOLOGACION.getWsfev1Url()).isEqualTo(expected);
    }

    @Test
    void produccionWsaaUrlMatchesOfficialEndpoint() {
        var expected = URI.create("https://wsaa.afip.gov.ar/ws/services/LoginCms");
        assertThat(ArcaEnvironment.PRODUCCION.getWsaaUrl()).isEqualTo(expected);
    }

    @Test
    void produccionWsfev1UrlMatchesOfficialEndpoint() {
        var expected = URI.create("https://servicios1.afip.gov.ar/wsfev1/service.asmx");
        assertThat(ArcaEnvironment.PRODUCCION.getWsfev1Url()).isEqualTo(expected);
    }

    @Test
    void enumFieldsAreFinal() throws NoSuchFieldException {
        var wsaaField = ArcaEnvironment.class.getDeclaredField("wsaaUrl");
        var wsfev1Field = ArcaEnvironment.class.getDeclaredField("wsfev1Url");

        assertThat(Modifier.isFinal(wsaaField.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(wsfev1Field.getModifiers())).isTrue();
    }

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

    @Test
    void enumValuesAreNonNull() {
        for (var env : ArcaEnvironment.values()) {
            assertThat(env.getWsaaUrl()).isNotNull();
            assertThat(env.getWsfev1Url()).isNotNull();
        }
    }
}
