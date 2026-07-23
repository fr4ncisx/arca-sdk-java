package io.github.fr4ncisx.arca.wsaa.internal.client;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Raw SOAP 1.1 HTTP client for the WSAA {@code loginCms} operation.
 * <p>
 * This client constructs SOAP envelopes as plain XML strings, sends them via
 * {@link HttpClient}, and returns the raw response body. It does not depend
 * on WSDL, JAX-WS, JAXB, or any code-generation tooling. The split-namespace
 * design of the real ARCA WSAA WSDL is handled transparently because the
 * response is parsed by local element name only, without namespace matching.
 *
 * @author fr4ncisx
 * @since 1.2.1
 */
final class RawSoapHttpClient implements ArcaSoapPort<String, String> {

    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String WSAA_NS = "http://wsaa.view.sua.dvadac.desein.afip.gov";
    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";

    private final HttpClient httpClient;
    private final URI endpoint;
    private final Duration readTimeout;

    /**
     * Creates a raw SOAP HTTP client for the given endpoint and timeouts.
     *
     * @param endpoint     the WSAA SOAP endpoint URL.
     * @param connectTimeout maximum time allowed to establish a connection.
     * @param readTimeout    maximum time allowed waiting for a response.
     * @throws ArcaValidationException if any argument is null or negative.
     */
    RawSoapHttpClient(URI endpoint, Duration connectTimeout, Duration readTimeout) {
        if (endpoint == null) {
            throw new ArcaValidationException("endpoint must not be null");
        }
        if (connectTimeout == null || connectTimeout.isNegative()) {
            throw new ArcaValidationException("connectTimeout must not be null or negative");
        }
        if (readTimeout == null || readTimeout.isNegative()) {
            throw new ArcaValidationException("readTimeout must not be null or negative");
        }
        this.endpoint = endpoint;
        this.readTimeout = readTimeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sends a SOAP 1.1 {@code loginCms} envelope to the WSAA endpoint and
     * returns the raw XML response body. The request envelope is constructed
     * using the real ARCA WSAA element namespace.
     *
     * @param cmsBase64 the Base64-encoded CMS signed TRA payload.
     * @return the raw XML response body from the WSAA service.
     * @throws ArcaSoapException if the HTTP request fails or the response is invalid.
     */
    @Override
    public String invoke(String cmsBase64) throws ArcaSoapException {
        if (cmsBase64 == null || cmsBase64.isBlank()) {
            throw new ArcaValidationException("cmsBase64 must not be null or blank");
        }

        String envelope = buildEnvelope(cmsBase64);
        HttpResponse<String> response = sendRequest(envelope);
        validateResponse(response);
        return response.body();
    }

    private String buildEnvelope(String cmsBase64) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="%s"
                                  xmlns:wsaa="%s">
                    <soapenv:Header/>
                    <soapenv:Body>
                        <wsaa:loginCms>
                            <wsaa:in0>%s</wsaa:in0>
                        </wsaa:loginCms>
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(SOAP_NS, WSAA_NS, cmsBase64);
    }

    private HttpResponse<String> sendRequest(String envelope) throws ArcaSoapException {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(endpoint)
                    .header("Content-Type", CONTENT_TYPE)
                    .header("SOAPAction", "")
                    .timeout(readTimeout)
                    .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
                    .build();
        } catch (IllegalArgumentException e) {
            throw new ArcaSoapException("Invalid SOAP endpoint URI: " + endpoint, e);
        }

        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new ArcaSoapException("Timeout communicating with WSAA at " + endpoint, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ArcaSoapException("Interrupted while communicating with WSAA at " + endpoint, e);
        } catch (IOException e) {
            throw new ArcaSoapException("Network error communicating with WSAA at " + endpoint, e);
        }
    }

    private static void validateResponse(HttpResponse<String> response) throws ArcaSoapException {
        int statusCode = response.statusCode();
        if (statusCode == 200) {
            return;
        }

        String body = response.body();
        if (body != null && containsSoapFault(body)) {
            return;
        }

        throw new ArcaSoapException("HTTP " + statusCode + " from WSAA at " + response.uri());
    }

    private static boolean containsSoapFault(String xml) {
        return xml.contains(":Fault>") || xml.contains("<Fault>");
    }

    static boolean isSoapFault(String responseBody) {
        return responseBody != null
            && (responseBody.contains(":Fault>") || responseBody.contains("<Fault>"));
    }
}
