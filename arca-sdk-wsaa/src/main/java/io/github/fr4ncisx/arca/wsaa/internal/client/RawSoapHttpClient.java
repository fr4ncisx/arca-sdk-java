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
import java.util.Locale;
import java.util.regex.Pattern;

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
    private static final Pattern STRICT_BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/]+={0,2}$");
    private static final Pattern LOOPBACK_IPV4_PATTERN = Pattern.compile("^127(\\.\\d{1,3}){3}$");

    private final HttpClient httpClient;
    private final URI endpoint;
    private final Duration readTimeout;

    /**
     * Creates a raw SOAP HTTP client for the given endpoint and timeouts.
     * <p>
     * Only HTTPS endpoints are accepted. Loopback hosts are exempted from the
     * TLS requirement so that local mock servers can be used in tests.
     *
     * @param endpoint     the WSAA SOAP endpoint URL.
     * @param connectTimeout maximum time allowed to establish a connection.
     * @param readTimeout    maximum time allowed waiting for a response.
     * @throws ArcaValidationException if any argument is null or negative, or
     *                                 the endpoint scheme is not HTTPS on a non-loopback host.
     */
    RawSoapHttpClient(URI endpoint, Duration connectTimeout, Duration readTimeout) {
        if (endpoint == null) {
            throw new ArcaValidationException("endpoint must not be null");
        }
        if (!isSecureEndpoint(endpoint)) {
            throw new ArcaValidationException(
                "Only HTTPS endpoints are allowed for SOAP transport; "
                + "loopback hosts are exempted for testing: " + endpoint);
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

    private static boolean isSecureEndpoint(URI endpoint) {
        String scheme = endpoint.getScheme();
        return "https".equalsIgnoreCase(scheme) || isLoopbackHost(endpoint);
    }

    private static boolean isLoopbackHost(URI endpoint) {
        String host = endpoint.getHost();
        if (host == null) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.equals("localhost")
            || normalized.equals("::1")
            || LOOPBACK_IPV4_PATTERN.matcher(normalized).matches();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sends a SOAP 1.1 {@code loginCms} envelope to the WSAA endpoint and
     * returns the raw XML response body. The request envelope is constructed
     * using the real ARCA WSAA element namespace. The CMS payload must be
     * strict Base64; any other content is rejected before it can reach the
     * envelope template.
     *
     * @param cmsBase64 the Base64-encoded CMS signed TRA payload.
     * @return the raw XML response body from the WSAA service.
     * @throws ArcaValidationException if the payload is null, blank, or not
     *                                 strict Base64.
     * @throws ArcaSoapException if the HTTP request fails or the response is invalid.
     */
    @Override
    public String invoke(String cmsBase64) throws ArcaSoapException {
        if (cmsBase64 == null || cmsBase64.isBlank()) {
            throw new ArcaValidationException("cmsBase64 must not be null or blank");
        }
        if (!isValidBase64Payload(cmsBase64)) {
            throw new ArcaValidationException("cmsBase64 must contain only strict Base64 characters");
        }

        String envelope = buildEnvelope(cmsBase64);
        HttpResponse<String> response = sendRequest(envelope);
        validateResponse(response);
        return response.body();
    }

    private static boolean isValidBase64Payload(String cmsBase64) {
        String compact = cmsBase64.replaceAll("[\r\n]", "");
        return compact.length() % 4 == 0 && STRICT_BASE64_PATTERN.matcher(compact).matches();
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
