package io.github.fr4ncisx.arca.test.support;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates consumer usage of ArcaMockServer without importing WireMock.
 * <p>
 * The scenarios represent WSAA and WSFEv1 integration tests that only depend on
 * the public test-support API.
 *
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
class ArcaMockServerUsageTest {

    private static final String FECAESOLICITAR_ACTION = "http://ar.gov.afip.dif.FEV1/FECAESolicitar";

    /**
     * Validates that a WSAA-style consumer can use only the public mock server
     * API to receive a successful LoginCms fixture.
     */
    @Test
    void wsaaIntegrationUsesOnlyPublicArcaMockServerApi() throws Exception {
        try (var server = new ArcaMockServer()) {
            server.start();
            server.stubLoginCmsSuccess();

            HttpResponse<String> response = post(server.baseUrl(), "/ws/services/LoginCms", null);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("<loginTicketReturn");
        }
    }

    /**
     * Validates that a WSFEv1-style consumer can use only the public mock server
     * API to receive a CAE fixture.
     */
    @Test
    void wsfev1IntegrationUsesOnlyPublicArcaMockServerApi() throws Exception {
        try (var server = new ArcaMockServer()) {
            server.start();
            server.stubCaeSuccess();

            HttpResponse<String> response = post(server.baseUrl(), "/wsfev1/service.asmx", FECAESOLICITAR_ACTION);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("<FECAESolicitarResult");
        }
    }

    private static HttpResponse<String> post(URI baseUrl, String path, String soapAction) throws Exception {
        var builder = HttpRequest.newBuilder(baseUrl.resolve(path))
            .POST(HttpRequest.BodyPublishers.ofString("<soap/>"))
            .header("Content-Type", "text/xml; charset=UTF-8");
        if (soapAction != null) {
            builder.header("SOAPAction", soapAction);
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
