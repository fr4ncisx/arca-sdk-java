package io.github.fr4ncisx.arca.wsaa.internal.client;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.test.support.ArcaMockServer;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.jspecify.annotations.NullMarked;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link LoginCmsClient} using {@link ArcaMockServer}.
 * <p>
 * Verifies that LoginCmsClient dynamically targets the SOAP WSAA service, applies
 * timeouts, parses responses correctly, and maps SOAP faults and protocol errors
 * to appropriate domain exceptions. Security regression tests cover XXE-hardened
 * fault parsing, fail-closed handling of malformed faults, TLS-only endpoints,
 * and strict Base64 payload validation.
 *
 * @author fr4ncisx
 * @since 0.1.0-M4
 */
@NullMarked
@SuppressWarnings("null")
class LoginCmsClientTest {

    private static final String VALID_BASE64_CMS_PAYLOAD =
            Base64.getEncoder().encodeToString("dummy-cms-payload".getBytes(StandardCharsets.UTF_8));

    private static final String XXE_FAULT_ATTACK_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE soapenv:Envelope [<!ENTITY xxe SYSTEM "file:///etc/hostname">]>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
              <soapenv:Body>
                <soapenv:Fault>
                  <faultstring>&xxe;</faultstring>
                </soapenv:Fault>
              </soapenv:Body>
            </soapenv:Envelope>
            """;

    private static final String UNPARSEABLE_FAULT_XML = "<soapenv:Envelope><soapenv:Body><Fault><<<<broken";

    private ArcaMockServer mockServer;
    private ArcaConfig config;

    @BeforeEach
    void setUp() {
        mockServer = new ArcaMockServer();
        mockServer.start();

        config = new ArcaConfig(
                Cuit.parse("20-33333333-4"),
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(2),
                Duration.ofSeconds(5)
        );
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    /**
     * Verifies that LoginCmsClient successfully logs in and parses the access token,
     * signature, and generation/expiration times from a successful WSAA SOAP response.
     */
    @Test
    void loginCmsShouldReturnTicketOnSuccess() {
        mockServer.stubLoginCmsSuccess();

        LoginCmsClient client = new LoginCmsClient(config, mockServer.baseUrl().resolve("/ws/services/LoginCms").toString());

        ArcaAccessTicket ticket = client.loginCms(VALID_BASE64_CMS_PAYLOAD);

        assertThat(ticket).isNotNull();
        // Values verified against 'login-cms-success.xml' fixture.
        assertThat(ticket.token()).isEqualTo(
                "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz48bG9naW5UaWNrZXRSZXF1ZXN0IHZlcnNpb249IjEuMCI+PCEtLSBUT0tFTiBOTyBSRUFMPS0tPjwvbG9naW5UaWNrZXRSZXF1ZXN0Pg=="
        );
        assertThat(ticket.sign()).isEqualTo(
                "MIICSgYJKoZIhvcNAQcCoIICOzCCAjcCAQExCzAJBgUrDgMCGgUAMIAGCSqGSIb3DQEHAaCAgX1YZXMgbGEgZmlybWEgZGUgZXN0ZSBmaXh0dXJlIHNvbG8gZXMgdW4gZWplbXBsbyBkZSB0ZXN0LCBubyBjb250aWVuZSBkYXRvcyByZWFsZXM="
        );
        assertThat(ticket.generationTime()).isEqualTo(Instant.parse("2026-05-13T13:00:00Z")); // UTC translated
        assertThat(ticket.expirationTime()).isEqualTo(Instant.parse("2026-05-14T01:00:00Z")); // UTC translated
    }

    /**
     * Verifies that LoginCmsClient maps a SOAP Fault returned by WSAA to {@link ArcaAuthException}.
     */
    @Test
    void loginCmsShouldThrowArcaAuthExceptionOnSoapFault() {
        mockServer.stubLoginCmsError();

        LoginCmsClient client = new LoginCmsClient(config, mockServer.baseUrl().resolve("/ws/services/LoginCms").toString());

        assertThatThrownBy(() -> client.loginCms(VALID_BASE64_CMS_PAYLOAD))
                .isInstanceOf(ArcaAuthException.class)
                .hasMessageContaining("WSAA service rejected authentication");
    }

    /**
     * Verifies that a SOAP fault carrying a DOCTYPE-based XXE payload is
     * rejected without resolving the external entity or leaking file content.
     */
    @Test
    void loginCmsShouldRejectXxeFaultWithoutLeakingFileContent() {
        mockServer.stubLoginCmsRawBody(XXE_FAULT_ATTACK_XML, 500);

        LoginCmsClient client = new LoginCmsClient(config, mockServer.baseUrl().resolve("/ws/services/LoginCms").toString());

        assertThatThrownBy(() -> client.loginCms(VALID_BASE64_CMS_PAYLOAD))
                .isInstanceOf(ArcaAuthException.class)
                .hasMessageContaining("unparseable SOAP fault")
                .hasMessageNotContaining("/etc/hostname");
    }

    /**
     * Verifies that an unparseable SOAP fault fails closed: no raw response
     * content is propagated into the exception message.
     */
    @Test
    void loginCmsShouldFailClosedOnUnparseableSoapFault() {
        mockServer.stubLoginCmsRawBody(UNPARSEABLE_FAULT_XML, 500);

        LoginCmsClient client = new LoginCmsClient(config, mockServer.baseUrl().resolve("/ws/services/LoginCms").toString());

        assertThatThrownBy(() -> client.loginCms(VALID_BASE64_CMS_PAYLOAD))
                .isInstanceOf(ArcaAuthException.class)
                .hasMessageContaining("unparseable SOAP fault")
                .hasMessageNotContaining("broken");
    }

    /**
     * Verifies that a non-HTTPS endpoint on a non-loopback host is rejected at
     * construction time.
     */
    @Test
    void constructorShouldRejectNonHttpsNonLoopbackEndpoint() {
        assertThatThrownBy(() -> new LoginCmsClient(config, "http://malicious.example.com/ws/services/LoginCms"))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("HTTPS");
    }

    /**
     * Verifies that a CMS payload containing characters outside the strict
     * Base64 alphabet is rejected before it can reach the SOAP envelope.
     */
    @Test
    void loginCmsShouldRejectNonBase64Payload() {
        LoginCmsClient client = new LoginCmsClient(config, mockServer.baseUrl().resolve("/ws/services/LoginCms").toString());

        assertThatThrownBy(() -> client.loginCms("MII_DUMMY_CMS <script>alert(1)</script>"))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("strict Base64");
    }

    /**
     * Verifies that a null CMS payload is rejected with ArcaValidationException.
     */
    @Test
    void loginCmsShouldRejectNullPayload() {
        LoginCmsClient client = new LoginCmsClient(config, mockServer.baseUrl().resolve("/ws/services/LoginCms").toString());

        assertThatThrownBy(() -> client.loginCms(null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("cannot be null, empty, or blank");
    }

    /**
     * Verifies that a blank CMS payload is rejected with ArcaValidationException.
     */
    @Test
    void loginCmsShouldRejectBlankPayload() {
        LoginCmsClient client = new LoginCmsClient(config, mockServer.baseUrl().resolve("/ws/services/LoginCms").toString());

        assertThatThrownBy(() -> client.loginCms("   "))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("cannot be null, empty, or blank");
    }

    /**
     * Verifies that a null config is rejected with ArcaValidationException.
     */
    @Test
    void constructorShouldRejectNullConfig() {
        assertThatThrownBy(() -> new LoginCmsClient(null, "http://any-endpoint"))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("ArcaConfig");
    }

    /**
     * Verifies that a null or blank endpoint URL is rejected with ArcaValidationException.
     */
    @Test
    void constructorShouldRejectBlankEndpoint() {
        assertThatThrownBy(() -> new LoginCmsClient(config, "   "))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("endpoint URL");
    }
}
