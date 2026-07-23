package io.github.fr4ncisx.arca.wsaa.internal.client;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Client for invoking the WSAA service to obtain access tickets.
 * <p>
 * This class orchestrates a raw SOAP HTTP invocation of the WSAA {@code loginCms}
 * operation, parses the XML response, and maps results or failures to SDK exceptions.
 * The split-namespace WSDL design of the real ARCA WSAA is handled transparently
 * because responses are parsed by local element name only, without namespace matching.
 *
 * @author fr4ncisx
 * @since 0.1.0-M4
 */
public final class LoginCmsClient {

    private final ArcaSoapPort<String, String> soapPort;

    /**
     * Creates a new LoginCmsClient with the given SDK configuration.
     *
     * @param config the SDK configuration containing timeouts and environment.
     * @throws ArcaValidationException if config is null.
     */
    public LoginCmsClient(ArcaConfig config) {
        this(config, config.environment().getWsaaUrl().toString());
    }

    /**
     * Creates a new LoginCmsClient with a custom endpoint URL.
     *
     * @param config          the SDK configuration containing timeouts.
     * @param wsaaEndpointUrl the custom endpoint URL of the WSAA service.
     * @throws ArcaValidationException if config or endpoint is null or blank.
     */
    public LoginCmsClient(ArcaConfig config, String wsaaEndpointUrl) {
        if (config == null) {
            throw new ArcaValidationException("The ARCA SDK configuration (ArcaConfig) cannot be null.");
        }
        if (wsaaEndpointUrl == null || wsaaEndpointUrl.isBlank()) {
            throw new ArcaValidationException("The WSAA endpoint URL cannot be null, empty, or blank.");
        }

        URI endpoint = URI.create(wsaaEndpointUrl);
        this.soapPort = new RawSoapHttpClient(endpoint, config.connectTimeout(), config.readTimeout());
    }

    /**
     * Invokes WSAA {@code loginCms} with the signed CMS payload.
     *
     * @param cmsSigned the Base64-encoded CMS signed TRA.
     * @return the parsed ArcaAccessTicket.
     * @throws ArcaAuthException if WSAA rejects the login or the response XML is invalid.
     * @throws ArcaSoapException if a network or SOAP transport error occurs.
     */
    public ArcaAccessTicket loginCms(String cmsSigned) throws ArcaAuthException, ArcaSoapException {
        if (cmsSigned == null || cmsSigned.isBlank()) {
            throw new ArcaValidationException("The CMS signed payload cannot be null, empty, or blank.");
        }

        String responseXml;
        try {
            responseXml = soapPort.invoke(cmsSigned);
        } catch (ArcaSoapException e) {
            if (e.getMessage() != null && e.getMessage().contains("coe.alreadyAuthenticated")) {
                throw new ArcaAuthException(
                    "WSAA already issued a valid TA for this CUIT and service. "
                    + "Reuse the cached TA instead of requesting a new one.", e);
            }
            throw e;
        }

        if (RawSoapHttpClient.isSoapFault(responseXml)) {
            throw new ArcaAuthException("WSAA service rejected authentication: " + extractFaultString(responseXml));
        }

        String innerXml = extractLoginCmsReturn(responseXml);
        return parseResponse(innerXml);
    }

    private static String extractLoginCmsReturn(String xmlResponse) {
        if (xmlResponse.contains("loginCmsReturn")) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));

                NodeList nodes = doc.getElementsByTagNameNS("*", "loginCmsReturn");
                if (nodes.getLength() > 0 && nodes.item(0) != null) {
                    String content = nodes.item(0).getTextContent();
                    if (content != null && !content.isBlank()) {
                        return content;
                    }
                }
            } catch (ParserConfigurationException | SAXException | IOException ignored) {
            }
        }
        return xmlResponse;
    }

    private ArcaAccessTicket parseResponse(String xmlResponse) {
        if (xmlResponse == null || xmlResponse.isBlank()) {
            throw new ArcaAuthException(
                "WSAA returned an empty response. Possible causes: "
                + "certificate not registered for this CUIT, invalid CMS signature, "
                + "incorrect keystore password, or WSAA server error.");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));

            NodeList tokens = doc.getElementsByTagNameNS("*", "token");
            NodeList signs = doc.getElementsByTagNameNS("*", "sign");
            NodeList genTimes = doc.getElementsByTagNameNS("*", "generationTime");
            NodeList expTimes = doc.getElementsByTagNameNS("*", "expirationTime");

            if (tokens.getLength() == 0 || tokens.item(0) == null
                || signs.getLength() == 0 || signs.item(0) == null
                || genTimes.getLength() == 0 || genTimes.item(0) == null
                || expTimes.getLength() == 0 || expTimes.item(0) == null) {
                throw new ArcaAuthException(
                    "WSAA returned a response without the expected ticket elements "
                    + "(token, sign, generationTime, expirationTime).");
            }

            String token = tokens.item(0).getTextContent().trim();
            String sign = signs.item(0).getTextContent().trim();
            String genTimeStr = genTimes.item(0).getTextContent().trim();
            String expTimeStr = expTimes.item(0).getTextContent().trim();

            Instant generationTime = OffsetDateTime.parse(genTimeStr).toInstant();
            Instant expirationTime = OffsetDateTime.parse(expTimeStr).toInstant();

            return new ArcaAccessTicket(token, sign, generationTime, expirationTime);
        } catch (ArcaAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new ArcaAuthException("Failed to parse the XML response from WSAA: " + e.getMessage(), e);
        }
    }

    private static String extractFaultString(String xmlResponse) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));
            var faultNodes = doc.getElementsByTagName("faultstring");
            if (faultNodes.getLength() > 0) {
                return faultNodes.item(0).getTextContent();
            }
        } catch (ParserConfigurationException | SAXException | IOException ignored) {
        }
        return "unknown error";
    }
}
