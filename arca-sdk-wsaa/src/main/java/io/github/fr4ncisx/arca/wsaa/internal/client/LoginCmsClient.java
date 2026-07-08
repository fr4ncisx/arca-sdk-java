package io.github.fr4ncisx.arca.wsaa.internal.client;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.internal.adapter.ArcaSoapClient;
import io.github.fr4ncisx.arca.soap.internal.config.SoapConfig;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import org.jspecify.annotations.Nullable;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPFaultException;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

/**
 * Client for invoking the WSAA service to obtain access tickets.
 * <p>
 * This class orchestrates the dynamic JAX-WS invocation of the WSAA SOAP service,
 * routes the request through the SOAP transport adapter, parses the XML response,
 * and maps results or failures to SDK exceptions.
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
     * Overloaded constructor for testing purposes, allowing a custom endpoint URL.
     *
     * @param config the SDK configuration containing timeouts.
     * @param wsaaEndpointUrl the custom endpoint URL of the WSAA service.
     * @throws ArcaValidationException if config or endpoint is null.
     */
    public LoginCmsClient(ArcaConfig config, String wsaaEndpointUrl) {
        if (config == null)
            throw new ArcaValidationException("The ARCA SDK configuration (ArcaConfig) cannot be null.");
        if (wsaaEndpointUrl == null || wsaaEndpointUrl.isBlank())
            throw new ArcaValidationException("The WSAA endpoint URL cannot be null, empty, or blank.");

        @Nullable URL wsdlUrl = LoginCmsClient.class.getResource("/wsdl/wsaa.wsdl");
        if (wsdlUrl == null)
            throw new ArcaSoapException("The WSAA WSDL resource cannot be found in the classpath.");

        QName serviceName = new QName("http://wsaa.view.sua.dvadac.desein.afip.gov", "LoginCMSService");
        QName portName = new QName("http://wsaa.view.sua.dvadac.desein.afip.gov", "LoginCms");

        Service service = Service.create(wsdlUrl, serviceName);
        LoginCmsWebService port = service.getPort(portName, LoginCmsWebService.class);

        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, wsaaEndpointUrl);
        this.soapPort = new ArcaSoapClient<>(bp, port::loginCms, SoapConfig.from(config));
    }

    /**
     * Invokes WSAA loginCms with the signed CMS payload.
     *
     * @param cmsSigned the Base64-encoded CMS signed TRA.
     * @return the parsed ArcaAccessTicket.
     * @throws ArcaAuthException if WSAA rejects the login or the response XML is invalid.
     * @throws ArcaSoapException if a network or SOAP transport error occurs.
     */
    public ArcaAccessTicket loginCms(String cmsSigned) throws ArcaAuthException, ArcaSoapException {
        if (cmsSigned == null || cmsSigned.isBlank())
            throw new ArcaValidationException("The Base64-encoded CMS signed payload cannot be null, empty, or blank.");

        String responseXml;
        try {
            responseXml = soapPort.invoke(cmsSigned);
        } catch (ArcaSoapException e) {
            if (e.getCause() instanceof SOAPFaultException faultException)
                throw new ArcaAuthException("WSAA service rejected authentication: " + faultException.getMessage(), faultException);
            throw e;
        }

        return parseResponse(responseXml);
    }

    private ArcaAccessTicket parseResponse(String xmlResponse) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));

            String token = doc.getElementsByTagName("token").item(0).getTextContent().trim();
            String sign = doc.getElementsByTagName("sign").item(0).getTextContent().trim();
            String genTimeStr = doc.getElementsByTagName("generationTime").item(0).getTextContent().trim();
            String expTimeStr = doc.getElementsByTagName("expirationTime").item(0).getTextContent().trim();

            Instant generationTime = OffsetDateTime.parse(genTimeStr).toInstant();
            Instant expirationTime = OffsetDateTime.parse(expTimeStr).toInstant();

            return new ArcaAccessTicket(token, sign, generationTime, expirationTime);
        } catch (Exception e) {
            throw new ArcaAuthException("Failed to parse the XML response from WSAA: " + e.getMessage(), e);
        }
    }
}
