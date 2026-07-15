package io.github.fr4ncisx.arca.client;

import io.github.fr4ncisx.arca.client.spi.ArcaClient;
import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.test.support.ArcaMockServer;
import io.github.fr4ncisx.arca.wsaa.spi.CertificateSource;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;
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
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
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
