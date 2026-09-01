package io.github.fr4ncisx.arca.wsaa.internal.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Factory of hardened DOM parsers that neutralize XML External Entity (XXE)
 * attack vectors when untrusted SOAP payloads are processed.
 * <p>
 * Every parser produced by this factory disallows DOCTYPE declarations,
 * external general and parameter entities, XInclude processing, and inline
 * entity expansion. Parsing an attacker-crafted document therefore fails
 * closed instead of resolving external references from the host filesystem
 * or network.
 *
 * @author fr4ncisx
 * @since 1.3.0
 */
public final class HardenedXmlParser {

    private static final String DISALLOW_DOCTYPE_DECL_FEATURE =
            "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String EXTERNAL_GENERAL_ENTITIES_FEATURE =
            "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES_FEATURE =
            "http://xml.org/sax/features/external-parameter-entities";

    private HardenedXmlParser() {
    }

    /**
     * Parses the given XML string using a hardened, namespace-aware parser.
     *
     * @param xml the raw XML content to parse.
     * @return the parsed DOM document.
     * @throws ParserConfigurationException if the hardened features cannot be applied.
     * @throws SAXException                 if the document contains a forbidden construct
     *                                      such as a DOCTYPE declaration or is not well-formed.
     * @throws IOException                  if the document cannot be read.
     */
    public static Document parse(String xml)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder builder = newHardenedDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Creates a {@link DocumentBuilder} configured against XXE vectors.
     *
     * @return a hardened document builder.
     * @throws ParserConfigurationException if the underlying implementation
     *                                      rejects any hardening feature.
     */
    public static DocumentBuilder newHardenedDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature(DISALLOW_DOCTYPE_DECL_FEATURE, true);
        factory.setFeature(EXTERNAL_GENERAL_ENTITIES_FEATURE, false);
        factory.setFeature(EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
        return factory.newDocumentBuilder();
    }
}
