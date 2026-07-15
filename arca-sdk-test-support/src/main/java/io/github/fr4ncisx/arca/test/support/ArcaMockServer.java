package io.github.fr4ncisx.arca.test.support;

import com.github.tomakehurst.wiremock.WireMockServer;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Reusable local mock server for ARCA SOAP integration tests.
 * <p>
 * The server exposes real ARCA endpoint paths under a localhost base URL and
 * serves contractual XML fixtures without requiring certificates, credentials,
 * API keys, or network access to ARCA.
 *
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
public final class ArcaMockServer implements AutoCloseable {

    private static final String WSAA_LOGIN_CMS_PATH = "/ws/services/LoginCms";
    private static final String WSFEV1_SERVICE_PATH = "/wsfev1/service.asmx";
    private static final String WSFEXV1_SERVICE_PATH = "/wsfexv1/service.asmx";
    private static final String WSMTXCA_SERVICE_PATH = "/wsmtxca/services/MTXCAService";
    private static final String SOAP_ACTION_HEADER = "SOAPAction";
    private static final String FECAESOLICITAR_ACTION = "http://ar.gov.afip.dif.FEV1/FECAESolicitar";
    private static final String FECOMPULTIMOAUTORIZADO_ACTION =
        "http://ar.gov.afip.dif.FEV1/FECompUltimoAutorizado";
    private static final String WSFEX_LAST_VOUCHER_ACTION = "http://ar.gov.afip.dif.fexv1/FEXGetLast_CMP";
    private static final String WSFEX_AUTHORIZE_ACTION = "http://ar.gov.afip.dif.fexv1/FEXAuthorize";
    private static final String WSMTXCA_LAST_VOUCHER_ACTION = "http://impl.service.wsmtxca.afip.gov.ar/service/consultarUltimoComprobanteAutorizado";
    private static final String WSMTXCA_AUTHORIZE_ACTION = "http://impl.service.wsmtxca.afip.gov.ar/service/autorizarComprobante";
    private static final String WSMTXCA_GET_VOUCHER_ACTION = "http://impl.service.wsmtxca.afip.gov.ar/service/consultarComprobante";
    private static final String XML_CONTENT_TYPE = "text/xml; charset=UTF-8";

    private static final String LOGIN_CMS_SUCCESS_FIXTURE = "/fixtures/wsaa/login-cms-success.xml";
    private static final String LOGIN_CMS_ERROR_FIXTURE = "/fixtures/wsaa/login-cms-error.xml";
    private static final String LAST_VOUCHER_SUCCESS_FIXTURE = "/fixtures/wsfev1/last-voucher-success.xml";
    private static final String CAE_SUCCESS_FIXTURE = "/fixtures/wsfev1/cae-request-success.xml";
    private static final String CAE_REJECTED_FIXTURE = "/fixtures/wsfev1/cae-request-rejection-001.xml";
    private static final String WSFEX_LAST_VOUCHER_SUCCESS_FIXTURE = "/fixtures/wsfexv1/last-voucher-success.xml";
    private static final String WSFEX_CAE_SUCCESS_FIXTURE = "/fixtures/wsfexv1/cae-success.xml";
    private static final String WSMTXCA_LAST_VOUCHER_SUCCESS_FIXTURE = "/fixtures/wsmtxca/last-voucher-success.xml";
    private static final String WSMTXCA_CAE_SUCCESS_FIXTURE = "/fixtures/wsmtxca/cae-success.xml";
    private static final String WSMTXCA_GET_VOUCHER_SUCCESS_FIXTURE = "/fixtures/wsmtxca/get-voucher-success.xml";

    private final WireMockServer server;
    private final FixtureLoader fixtureLoader;

    /**
     * Creates a mock server that listens on a dynamic local port.
     */
    public ArcaMockServer() {
        this(new FixtureLoader(ArcaMockServer.class.getClassLoader()));
    }

    ArcaMockServer(FixtureLoader fixtureLoader) {
        this(new WireMockServer(wireMockConfig().dynamicPort()), fixtureLoader);
    }

    private ArcaMockServer(WireMockServer server, FixtureLoader fixtureLoader) {
        this.server = server;
        this.fixtureLoader = fixtureLoader;
    }

    /**
     * Starts the local mock server if it is not already running.
     */
    public void start() {
        if (!server.isRunning()) {
            server.start();
        }
    }

    /**
     * Stops the local mock server if it is running.
     */
    public void stop() {
        if (server.isRunning()) {
            server.stop();
        }
    }

    /**
     * Stops the local mock server and releases its local port.
     */
    @Override
    public void close() {
        stop();
    }

    /**
     * Returns the local base URL where ARCA endpoint paths are exposed.
     *
     * @return the localhost base URI
     * @throws IllegalStateException if the server has not been started
     */
    public URI baseUrl() {
        ensureStarted();
        return URI.create(server.baseUrl());
    }

    /**
     * Removes all registered stubs and request history.
     *
     * @throws IllegalStateException if the server has not been started
     */
    public void reset() {
        ensureStarted();
        server.resetAll();
    }

    /**
     * Registers a successful WSAA LoginCms response.
     *
     * @throws IllegalStateException if the server is stopped or the fixture cannot be loaded
     */
    public void stubLoginCmsSuccess() {
        stubPath(WSAA_LOGIN_CMS_PATH, LOGIN_CMS_SUCCESS_FIXTURE, 200);
    }

    /**
     * Registers a WSAA LoginCms SOAP fault response.
     *
     * @throws IllegalStateException if the server is stopped or the fixture cannot be loaded
     */
    public void stubLoginCmsError() {
        stubPath(WSAA_LOGIN_CMS_PATH, LOGIN_CMS_ERROR_FIXTURE, 500);
    }

    /**
     * Registers a successful WSFEv1 FECompUltimoAutorizado response.
     *
     * @throws IllegalStateException if the server is stopped or the fixture cannot be loaded
     */
    public void stubLastVoucherSuccess() {
        stubSoapAction(FECOMPULTIMOAUTORIZADO_ACTION, LAST_VOUCHER_SUCCESS_FIXTURE, 200);
    }

    /**
     * Registers an approved WSFEv1 FECAESolicitar response.
     *
     * @throws IllegalStateException if the server is stopped or the fixture cannot be loaded
     */
    public void stubCaeSuccess() {
        stubSoapAction(FECAESOLICITAR_ACTION, CAE_SUCCESS_FIXTURE, 200);
    }

    /**
     * Registers a rejected WSFEv1 FECAESolicitar response.
     *
     * @throws IllegalStateException if the server is stopped or the fixture cannot be loaded
     */
    public void stubCaeRejected() {
        stubSoapAction(FECAESOLICITAR_ACTION, CAE_REJECTED_FIXTURE, 200);
    }

    /**
     * Registers a successful WSFEXv1 FEXGetLast_CMP response.
     *
     * @throws IllegalStateException if the server is stopped or the fixture cannot be loaded
     */
    public void stubWsfexLastVoucherSuccess() {
        stubSoapAction(WSFEXV1_SERVICE_PATH, WSFEX_LAST_VOUCHER_ACTION, WSFEX_LAST_VOUCHER_SUCCESS_FIXTURE, 200);
    }

    /**
     * Registers an approved WSFEXv1 FEXAuthorize response.
     *
     * @throws IllegalStateException if the server is stopped or the fixture cannot be loaded
     */
    public void stubWsfexCaeSuccess() {
        stubSoapAction(WSFEXV1_SERVICE_PATH, WSFEX_AUTHORIZE_ACTION, WSFEX_CAE_SUCCESS_FIXTURE, 200);
    }

    /**
     * Registers a successful WSMTXCA consultarUltimoComprobanteAutorizado response.
     *
     * @throws IllegalStateException if the server is stopped or the fixture cannot be loaded
     */
    public void stubWsmtxcaLastVoucherSuccess() {
        stubSoapAction(WSMTXCA_SERVICE_PATH, WSMTXCA_LAST_VOUCHER_ACTION, WSMTXCA_LAST_VOUCHER_SUCCESS_FIXTURE, 200);
    }

    /**
     * Registers an approved WSMTXCA autorizarComprobante response.
     *
     * @throws IllegalStateException if the server is stopped or the fixture cannot be loaded
     */
    public void stubWsmtxcaCaeSuccess() {
        stubSoapAction(WSMTXCA_SERVICE_PATH, WSMTXCA_AUTHORIZE_ACTION, WSMTXCA_CAE_SUCCESS_FIXTURE, 200);
    }

    /**
     * Registers a successful WSMTXCA consultarComprobante response.
     *
     * @throws IllegalStateException if the server is stopped or the fixture cannot be loaded
     */
    public void stubWsmtxcaGetVoucherSuccess() {
        stubSoapAction(WSMTXCA_SERVICE_PATH, WSMTXCA_GET_VOUCHER_ACTION, WSMTXCA_GET_VOUCHER_SUCCESS_FIXTURE, 200);
    }

    private void stubPath(String path, String fixturePath, int status) {
        ensureStarted();
        String xml = fixtureLoader.load(fixturePath);
        server.stubFor(post(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", XML_CONTENT_TYPE)
                .withBody(xml)));
    }

    private void stubSoapAction(String soapAction, String fixturePath, int status) {
        stubSoapAction(WSFEV1_SERVICE_PATH, soapAction, fixturePath, status);
    }

    private void stubSoapAction(String servicePath, String soapAction, String fixturePath, int status) {
        ensureStarted();
        String xml = fixtureLoader.load(fixturePath);
        server.stubFor(post(urlPathEqualTo(servicePath))
            .withHeader(SOAP_ACTION_HEADER, containing(soapAction))
            .willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", XML_CONTENT_TYPE)
                .withBody(xml)));
    }

    private void ensureStarted() {
        if (!server.isRunning()) {
            throw new IllegalStateException("ArcaMockServer must be started before use");
        }
    }
}
