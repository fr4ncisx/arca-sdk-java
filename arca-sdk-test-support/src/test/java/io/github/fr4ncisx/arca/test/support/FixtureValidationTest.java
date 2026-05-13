package io.github.fr4ncisx.arca.test.support;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifica que los 6 archivos XML de ejemplo en fixtures/ sean XML bien formados.
 * <p>
 * Cada fixture se carga desde classpath, se parsea con un parser XML estandar,
 * y no debe lanzar ninguna excepcion de parseo. Este test garantiza que los
 * archivos XML estan estructuralmente correctos antes de usarlos en otros tests
 * (ArcaMockServer, XMLUnit, etc.).
 */
class FixtureValidationTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "/fixtures/wsaa/tra-valid-001.xml",
        "/fixtures/wsaa/login-cms-success.xml",
        "/fixtures/wsaa/login-cms-error.xml",
        "/fixtures/wsfev1/last-voucher-success.xml",
        "/fixtures/wsfev1/cae-request-success.xml",
        "/fixtures/wsfev1/cae-request-rejection-001.xml"
    })
    void shouldBeWellFormedXml(String path) {
        assertThatCode(() -> {
            try (var in = getClass().getResourceAsStream(path)) {
                var factory = DocumentBuilderFactory.newInstance();
                factory.newDocumentBuilder().parse(in);
            }
        }).doesNotThrowAnyException();
    }
}
