package io.github.fr4ncisx.arca.test.support;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates the reusable local ARCA mock server.
 * <p>
 * The tests verify lifecycle behavior, fixture-backed stubs, operation matching,
 * and deterministic fixture loading failures.
 *
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
class ArcaMockServerTest {

    private static final String WSAA_PATH = "/ws/services/LoginCms";
    private static final String WSFEV1_PATH = "/wsfev1/service.asmx";
    private static final String FECAESOLICITAR_ACTION = "http://ar.gov.afip.dif.FEV1/FECAESolicitar";
    private static final String FECOMPULTIMOAUTORIZADO_ACTION =
        "http://ar.gov.afip.dif.FEV1/FECompUltimoAutorizado";

    /**
     * Validates that the mock server starts on localhost and exposes a base URL.
     */
    @Test
    void startsAndReturnsBaseUrl() {
        try (var server = new ArcaMockServer()) {
            server.start();

            URI baseUrl = server.baseUrl();

            assertThat(baseUrl.getScheme()).isEqualTo("http");
            assertThat(baseUrl.getHost()).isEqualTo("localhost");
            assertThat(baseUrl.getPort()).isPositive();
        }
    }

    /**
     * Validates that baseUrl fails deterministically before the server starts.
     */
    @Test
    void baseUrlRejectsStoppedServer() {
        try (var server = new ArcaMockServer()) {
            assertThatThrownBy(server::baseUrl)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("started");
        }
    }

    /**
     * Validates that reset removes registered stubs.
     */
    @Test
    void resetRemovesRegisteredStubs() throws Exception {
        try (var server = new ArcaMockServer()) {
            server.start();
            server.stubLoginCmsSuccess();

            assertThat(post(server.baseUrl(), WSAA_PATH, null).statusCode()).isEqualTo(200);

            server.reset();

            assertThat(post(server.baseUrl(), WSAA_PATH, null).statusCode()).isEqualTo(404);
        }
    }

    /**
     * Validates that LoginCms stubs return the contractual WSAA fixtures.
     */
    @Test
    void stubsLoginCmsResponsesFromContractFixtures() throws Exception {
        try (var server = new ArcaMockServer()) {
            server.start();
            server.stubLoginCmsSuccess();

            HttpResponse<String> success = post(server.baseUrl(), WSAA_PATH, null);

            assertThat(success.statusCode()).isEqualTo(200);
            assertThat(success.body()).contains("<loginCmsReturn>");

            server.reset();
            server.stubLoginCmsError();

            HttpResponse<String> fault = post(server.baseUrl(), WSAA_PATH, null);

            assertThat(fault.statusCode()).isEqualTo(500);
            assertThat(fault.body()).contains("<soap:Fault");
        }
    }

    /**
     * Validates that WSFEv1 stubs use the real service path and distinguish
     * operations by SOAPAction.
     */
    @Test
    void stubsWsfev1ResponsesBySoapAction() throws Exception {
        try (var server = new ArcaMockServer()) {
            server.start();
            server.stubLastVoucherSuccess();
            server.stubCaeSuccess();

            HttpResponse<String> lastVoucher = post(server.baseUrl(), WSFEV1_PATH, FECOMPULTIMOAUTORIZADO_ACTION);
            HttpResponse<String> cae = post(server.baseUrl(), WSFEV1_PATH, FECAESOLICITAR_ACTION);

            assertThat(lastVoucher.statusCode()).isEqualTo(200);
            assertThat(lastVoucher.body()).contains("<FECompUltimoAutorizadoResult");
            assertThat(cae.statusCode()).isEqualTo(200);
            assertThat(cae.body()).contains("<Resultado>A</Resultado>");

            server.reset();
            server.stubCaeRejected();

            HttpResponse<String> rejected = post(server.baseUrl(), WSFEV1_PATH, FECAESOLICITAR_ACTION);

            assertThat(rejected.statusCode()).isEqualTo(200);
            assertThat(rejected.body()).contains("<Resultado>R</Resultado>");
        }
    }

    /**
     * Validates that missing fixtures fail deterministically when a stub method
     * attempts to register the response.
     */
    @Test
    void stubFailsDeterministicallyWhenFixtureIsMissing() {
        var missingLoader = new FixtureLoader(new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) {
                return null;
            }
        });

        try (var server = new ArcaMockServer(missingLoader)) {
            server.start();

            assertThatThrownBy(server::stubLoginCmsSuccess)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fixture not found");
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
