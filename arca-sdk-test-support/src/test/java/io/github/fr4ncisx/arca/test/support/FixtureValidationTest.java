package io.github.fr4ncisx.arca.test.support;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that all XML fixture files in the fixtures directory are well-formed.
 * <p>
 * Each fixture is loaded from the classpath and parsed with a standard XML parser.
 * The test ensures that fixture files are structurally correct before they are
 * used in other tests (ArcaMockServer, XMLUnit, etc.).
 *
 * @author fr4ncisx
 * @since 0.1.0-M1
 */
class FixtureValidationTest {

    /**
     * Validates that the XML file at the given path is well-formed and can be
     * parsed without throwing an exception.
     *
     * @param path the classpath-relative path to the fixture file
     */
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
            String xml = TestFixtureLoader.load(path);
            try (var in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
                var factory = DocumentBuilderFactory.newInstance();
                factory.newDocumentBuilder().parse(in);
            }
        }).doesNotThrowAnyException();
    }
}
