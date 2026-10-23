package io.github.fr4ncisx.arca.client;

import io.github.fr4ncisx.arca.client.spi.ArcaClient;
import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.test.support.ArcaMockServer;
import io.github.fr4ncisx.arca.wsaa.spi.CertificateSource;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;
import io.github.fr4ncisx.arca.wsfev1.model.common.ConceptType;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeRequest;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeVatLine;
import io.github.fr4ncisx.arca.wsfev1.model.common.VatType;
import io.github.fr4ncisx.arca.wsfev1.model.voucher.VoucherConsultRequest;
import io.github.fr4ncisx.arca.wsfev1.model.lastvoucher.LastVoucherRequest;
import io.github.fr4ncisx.arca.wsfev1.model.lastvoucher.LastVoucherResponse;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Integration tests verifying the SOAP client resilience and fault tolerance (chaos simulation).
 * <p>
 * Simulates transient read timeouts using WireMock and verifies that the client
 * transparently retries and recovers.
 *
 * @author fr4ncisx
 * @since 1.1.0
 */
class SoapResilienceIntegrationTest {

    private static ArcaMockServer server;

    private static URI originalWsaaUrl;
    private static URI originalWsfev1Url;
    private static URI originalRegistryUrl;
    private static URI originalWsfexv1Url;
    private static URI originalWsmtxcaUrl;
    private static URI originalWscdcUrl;

    @BeforeAll
    static void setUpAll() throws Exception {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }

        server = new ArcaMockServer();
        server.start();

        originalWsaaUrl = ArcaEnvironment.HOMOLOGACION.getWsaaUrl();
        originalWsfev1Url = ArcaEnvironment.HOMOLOGACION.getWsfev1Url();
        originalRegistryUrl = ArcaEnvironment.HOMOLOGACION.getRegistryUrl();
        originalWsfexv1Url = ArcaEnvironment.HOMOLOGACION.getWsfexv1Url();
        originalWsmtxcaUrl = ArcaEnvironment.HOMOLOGACION.getWsmtxcaUrl();
        originalWscdcUrl = ArcaEnvironment.HOMOLOGACION.getWscdcUrl();

        URI mockBase = server.baseUrl();
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "wsaaUrl", mockBase.resolve("/ws/services/LoginCms"));
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "wsfev1Url", mockBase.resolve("/wsfev1/service.asmx"));
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "registryUrl", mockBase.resolve("/sr-padron/webservices/personaServiceA4"));
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "wsfexv1Url", mockBase.resolve("/wsfexv1/service.asmx"));
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "wsmtxcaUrl", mockBase.resolve("/wsmtxca/services/MTXCAService"));
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "wscdcUrl", mockBase.resolve("/WSCDC/service.asmx"));
    }

    @AfterAll
    static void tearDownAll() throws Exception {
        if (server != null) {
            server.stop();
        }
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "wsaaUrl", originalWsaaUrl);
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "wsfev1Url", originalWsfev1Url);
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "registryUrl", originalRegistryUrl);
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "wsfexv1Url", originalWsfexv1Url);
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "wsmtxcaUrl", originalWsmtxcaUrl);
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "wscdcUrl", originalWscdcUrl);
    }

    /**
     * Resets simulated remote state and request history between recovery scenarios.
     */
    @BeforeEach
    void resetScenario() {
        server.reset();
        WireMock.configureFor("localhost", server.baseUrl().getPort());
    }

    @Test
    void recoversFromTransientTimeoutsUsingRetries() throws Exception {
        server.stubLoginCmsSuccess();

        WireMock.configureFor("localhost", server.baseUrl().getPort());

        String successBody = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <FECompUltimoAutorizadoResponse xmlns="http://ar.gov.afip.dif.FEV1/">
                  <FECompUltimoAutorizadoResult>
                    <CbteNro>105</CbteNro>
                    <PtoVta>1</PtoVta>
                    <CbteTipo>11</CbteTipo>
                  </FECompUltimoAutorizadoResult>
                </FECompUltimoAutorizadoResponse>
              </soap:Body>
            </soap:Envelope>
            """;

        stubFor(post(urlPathEqualTo("/wsfev1/service.asmx"))
                .inScenario("Timeouts Scenario")
                .whenScenarioStateIs(Scenario.STARTED)
                .withHeader("SOAPAction", containing("FECompUltimoAutorizado"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml; charset=utf-8")
                        .withFixedDelay(1500))
                .willSetStateTo("First Timeout"));

        stubFor(post(urlPathEqualTo("/wsfev1/service.asmx"))
                .inScenario("Timeouts Scenario")
                .whenScenarioStateIs("First Timeout")
                .withHeader("SOAPAction", containing("FECompUltimoAutorizado"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml; charset=utf-8")
                        .withBody(successBody)));

        ArcaConfig config = new ArcaConfig(
                Cuit.parse("20-33333333-4"),
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(2),
                Duration.ofMillis(500)
        );

        KeyStore keyStore = createKeyStore("CN=Test, SERIALNUMBER=20333333334");
        CertificateSource certificate = () -> keyStore;

        ArcaClient client = ArcaClient.builder()
                .config(config)
                .certificate(certificate)
                .build();

        LastVoucherResponse response = client.wsfev1().getLastVoucher(
                new LastVoucherRequest(1, VoucherType.INVOICE_B)
        );

        assertThat(response).isNotNull();
        assertThat(response.lastNumber()).isEqualTo(105);
    }

    /**
     * Recovers an authorization whose response was lost by querying the original voucher.
     *
     * @throws Exception if the local certificate cannot be created.
     */
    @Test
    void recoversAuthorizationThroughQueryAfterLostResponse() throws Exception {
        ArcaClient client = clientWithLostAuthorizationResponse();
        stubFor(post(urlPathEqualTo("/wsfev1/service.asmx"))
                .inScenario("Authorization recovery").whenScenarioStateIs("Authorized")
                .withHeader("SOAPAction", containing("FECompConsultar"))
                .willReturn(aResponse().withHeader("Content-Type", "text/xml; charset=utf-8")
                        .withBody(queryResponse("""
                            <ResultGet>
                              <Concepto>1</Concepto><DocTipo>80</DocTipo><DocNro>20333333334</DocNro>
                              <CbteDesde>1</CbteDesde><CbteHasta>1</CbteHasta><CbteFch>20261022</CbteFch>
                              <ImpTotal>121</ImpTotal><ImpTotConc>0</ImpTotConc><ImpNeto>100</ImpNeto>
                              <ImpOpEx>0</ImpOpEx><ImpIVA>21</ImpIVA><ImpTrib>0</ImpTrib>
                              <MonId>PES</MonId><MonCotiz>1</MonCotiz><Resultado>A</Resultado>
                              <CodAutorizacion>70000000000001</CodAutorizacion><EmisionTipo>CAE</EmisionTipo>
                              <FchVto>20261101</FchVto><PtoVta>1</PtoVta><CbteTipo>1</CbteTipo>
                            </ResultGet>
                            """))));

        assertUnknownAuthorization(client);
        var response = client.wsfev1().getVoucher(new VoucherConsultRequest(1, VoucherType.INVOICE_A, 1));

        assertThat(response.detail()).isNotNull();
        assertThat(response.detail().number()).isEqualTo(1);
        assertThat(response.detail().salesPoint()).isEqualTo(1);
        assertThat(response.detail().voucherType()).isEqualTo(VoucherType.INVOICE_A);
        assertThat(response.detail().total()).isEqualTo(121);
        assertThat(response.detail().cae()).isEqualTo("70000000000001");
        verifySingleAuthorizationAndQueries(1);
    }

    /**
     * Leaves reconciliation to the caller when the timed-out voucher is not found.
     *
     * @throws Exception if the local certificate cannot be created.
     */
    @Test
    void neverResubmitsWhenQueryDoesNotFindVoucher() throws Exception {
        ArcaClient client = clientWithLostAuthorizationResponse();
        stubFor(post(urlPathEqualTo("/wsfev1/service.asmx"))
                .withHeader("SOAPAction", containing("FECompConsultar"))
                .willReturn(aResponse().withHeader("Content-Type", "text/xml; charset=utf-8")
                        .withBody(queryResponse("<Errors><Err><Code>602</Code><Msg>Not found</Msg></Err></Errors>"))));

        assertUnknownAuthorization(client);
        var response = client.wsfev1().getVoucher(new VoucherConsultRequest(1, VoucherType.INVOICE_A, 1));

        assertThat(response.detail()).isNull();
        assertThat(response.errors()).extracting(error -> error.code()).containsExactly(602);
        verifySingleAuthorizationAndQueries(1);
    }

    /**
     * Retries only the safe query when both authorization and reconciliation time out.
     *
     * @throws Exception if the local certificate cannot be created.
     */
    @Test
    void neverResubmitsWhenQueryTimesOut() throws Exception {
        ArcaClient client = clientWithLostAuthorizationResponse();
        stubFor(post(urlPathEqualTo("/wsfev1/service.asmx"))
                .withHeader("SOAPAction", containing("FECompConsultar"))
                .willReturn(aResponse().withFixedDelay(1500)));

        assertUnknownAuthorization(client);
        assertThatThrownBy(() -> client.wsfev1().getVoucher(
                new VoucherConsultRequest(1, VoucherType.INVOICE_A, 1)))
                .isInstanceOf(ArcaSoapException.class);
        verifySingleAuthorizationAndQueries(3);
    }

    private static ArcaClient clientWithLostAuthorizationResponse() throws Exception {
        server.stubLoginCmsSuccess();
        stubFor(post(urlPathEqualTo("/wsfev1/service.asmx"))
                .inScenario("Authorization recovery").whenScenarioStateIs(Scenario.STARTED)
                .withHeader("SOAPAction", containing("FECAESolicitar"))
                .willSetStateTo("Authorized")
                .willReturn(aResponse().withFixedDelay(1500)));
        ArcaConfig config = new ArcaConfig(Cuit.parse("20-33333333-4"), ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(2), Duration.ofMillis(500));
        KeyStore keyStore = createKeyStore("CN=Test, SERIALNUMBER=20333333334");
        return ArcaClient.builder().config(config).certificate(() -> keyStore).build();
    }

    private static void assertUnknownAuthorization(ArcaClient client) {
        CaeRequest request = new CaeRequest(VoucherType.INVOICE_A, 1, 1, ConceptType.PRODUCTS,
                Cuit.parse("20-33333333-4"), 100, 0, 0, 21, 121, LocalDate.of(2026, 10, 22),
                List.of(new CaeVatLine(VatType.VAT_21, 100, 21)));
        assertThatThrownBy(() -> client.wsfev1().requestCae(request))
                .isInstanceOf(ArcaSoapException.class).hasMessageContaining("outcome is unknown");
    }

    private static String queryResponse(String result) {
        return """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body><FECompConsultarResponse xmlns="http://ar.gov.afip.dif.FEV1/">
                    <FECompConsultarResult>%s</FECompConsultarResult>
                  </FECompConsultarResponse></soap:Body>
                </soap:Envelope>
                """.formatted(result);
    }

    private static void verifySingleAuthorizationAndQueries(int queries) {
        verify(1, postRequestedFor(urlPathEqualTo("/wsfev1/service.asmx"))
                .withHeader("SOAPAction", containing("FECAESolicitar")));
        verify(queries, postRequestedFor(urlPathEqualTo("/wsfev1/service.asmx"))
                .withHeader("SOAPAction", containing("FECompConsultar"))
                .withRequestBody(matchingXPath("//*[local-name()='CbteNro' and text()='1']")));
    }

    private static void setEnvUrl(ArcaEnvironment env, String fieldName, URI value) throws Exception {
        Field field = ArcaEnvironment.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(env, value);
    }

    private static KeyStore createKeyStore(String dn) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        X500Name issuer = new X500Name(dn);
        X500Name subject = new X500Name(dn);
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24);
        Date notAfter = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365);

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, subject, keyPair.getPublic()
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());

        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(builder.build(signer));

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("wsaa", keyPair.getPrivate(), new char[0], new Certificate[]{cert});
        return keyStore;
    }
}
