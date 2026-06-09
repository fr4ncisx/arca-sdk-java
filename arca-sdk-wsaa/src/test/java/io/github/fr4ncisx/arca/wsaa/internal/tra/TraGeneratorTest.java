package io.github.fr4ncisx.arca.wsaa.internal.tra;

import io.github.fr4ncisx.arca.core.clock.FixedClock;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates the WSAA TRA XML generator.
 * <p>
 * The tests verify deterministic clock usage, XML well-formedness, escaped
 * service content, and input validation failures.
 *
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
class TraGeneratorTest {

    /**
     * Validates that a TRA generated from a fixed clock contains the expected
     * service and Argentina offset timestamps.
     */
    @Test
    void generatesWellFormedTraWithInjectedClock() throws Exception {
        var generator = new TraGenerator(new FixedClock(Instant.parse("2026-05-13T13:00:00Z")));

        String xml = generator.generate("wsfe", Duration.ofHours(12));
        Document document = parse(xml);

        assertThat(document.getDocumentElement().getNodeName()).isEqualTo("loginTicketRequest");
        assertThat(document.getDocumentElement().getAttribute("version")).isEqualTo("1.0");
        assertThat(text(document, "service")).isEqualTo("wsfe");
        assertThat(text(document, "generationTime")).isEqualTo("2026-05-13T10:00:00-03:00");
        assertThat(text(document, "expirationTime")).isEqualTo("2026-05-13T22:00:00-03:00");
        assertThat(text(document, "uniqueId")).isEqualTo("1778677200");
    }

    /**
     * Validates that service text is escaped so special characters cannot break
     * the generated XML document.
     */
    @Test
    void escapesServiceContent() throws Exception {
        var generator = new TraGenerator(new FixedClock(Instant.parse("2026-05-13T13:00:00Z")));

        String xml = generator.generate("wsfe&test<one>", Duration.ofMinutes(30));
        Document document = parse(xml);

        assertThat(xml).contains("wsfe&amp;test&lt;one&gt;");
        assertThat(text(document, "service")).isEqualTo("wsfe&test<one>");
    }

    /**
     * Validates that the constructor rejects a null clock dependency.
     */
    @Test
    void rejectsNullClock() {
        assertThatThrownBy(() -> new TraGenerator(null))
            .isInstanceOf(ArcaValidationException.class)
            .hasMessageContaining("clock");
    }

    /**
     * Validates that the generator rejects a null service value.
     */
    @Test
    void rejectsNullService() {
        var generator = new TraGenerator(new FixedClock(Instant.parse("2026-05-13T13:00:00Z")));

        assertThatThrownBy(() -> generator.generate(null, Duration.ofHours(12)))
            .isInstanceOf(ArcaValidationException.class)
            .hasMessageContaining("service");
    }

    /**
     * Validates that the generator rejects a blank service value.
     */
    @Test
    void rejectsBlankService() {
        var generator = new TraGenerator(new FixedClock(Instant.parse("2026-05-13T13:00:00Z")));

        assertThatThrownBy(() -> generator.generate("   ", Duration.ofHours(12)))
            .isInstanceOf(ArcaValidationException.class)
            .hasMessageContaining("service");
    }

    /**
     * Validates that the generator rejects a null time-to-live value.
     */
    @Test
    void rejectsNullTtl() {
        var generator = new TraGenerator(new FixedClock(Instant.parse("2026-05-13T13:00:00Z")));

        assertThatThrownBy(() -> generator.generate("wsfe", null))
            .isInstanceOf(ArcaValidationException.class)
            .hasMessageContaining("ttl");
    }

    /**
     * Validates that the generator rejects a zero time-to-live value.
     */
    @Test
    void rejectsZeroTtl() {
        var generator = new TraGenerator(new FixedClock(Instant.parse("2026-05-13T13:00:00Z")));

        assertThatThrownBy(() -> generator.generate("wsfe", Duration.ZERO))
            .isInstanceOf(ArcaValidationException.class)
            .hasMessageContaining("ttl");
    }

    /**
     * Validates that the generator rejects a negative time-to-live value.
     */
    @Test
    void rejectsNegativeTtl() {
        var generator = new TraGenerator(new FixedClock(Instant.parse("2026-05-13T13:00:00Z")));

        assertThatThrownBy(() -> generator.generate("wsfe", Duration.ofSeconds(-1)))
            .isInstanceOf(ArcaValidationException.class)
            .hasMessageContaining("ttl");
    }

    private static Document parse(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        try (var stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            return factory.newDocumentBuilder().parse(stream);
        }
    }

    private static String text(Document document, String tagName) {
        return document.getElementsByTagName(tagName).item(0).getTextContent();
    }
}
