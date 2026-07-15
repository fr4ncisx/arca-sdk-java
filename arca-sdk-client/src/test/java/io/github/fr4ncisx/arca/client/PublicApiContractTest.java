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
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Public API contract test verifying unified facade isolation and stub encapsulation.
 * <p>
 * Ensures consumers can construct the main facade using only public types and make
 * complete end-to-end SOAP requests against the mock integration server.
 *
 * @author fr4ncisx
 * @since 0.6.0
 */
class PublicApiContractTest {

    private static final Logger log = LoggerFactory.getLogger(PublicApiContractTest.class);
    private static ArcaMockServer server;

    private static URI originalWsaaUrl;
    private static URI originalWsfev1Url;
    private static URI originalRegistryUrl;
    private static URI originalWsfexv1Url;
    private static URI originalWsmtxcaUrl;

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

        URI mockBase = server.baseUrl();
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "wsaaUrl", mockBase.resolve("/ws/services/LoginCms"));
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "wsfev1Url", mockBase.resolve("/wsfev1/service.asmx"));
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "registryUrl", mockBase.resolve("/sr-padron/webservices/personaServiceA4"));
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "wsfexv1Url", mockBase.resolve("/wsfexv1/service.asmx"));
        setEnvUrl(ArcaEnvironment.HOMOLOGACION, "wsmtxcaUrl", mockBase.resolve("/wsmtxca/services/MTXCAService"));
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
    }

    @Test
    void executeLastVoucherFlowUsingPublicApiOnly() throws Exception {
        server.stubLoginCmsSuccess();

        com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", server.baseUrl().getPort());
        stubLastVoucherSoapSuccess();

        ArcaConfig config = new ArcaConfig(
                Cuit.parse("20-33333333-4"),
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(5),
                Duration.ofSeconds(5)
        );

        KeyStore keyStore = createKeyStore("CN=Test, SERIALNUMBER=20333333334");
        CertificateSource certificate = () -> keyStore;

        ArcaClient client = ArcaClient.builder()
                .config(config)
                .certificate(certificate)
                .build();

        LastVoucherRequest request = new LastVoucherRequest(1, VoucherType.INVOICE_B);
        LastVoucherResponse response = client.wsfev1().getLastVoucher(request);

        assertThat(response).isNotNull();
        assertThat(response.lastNumber()).isEqualTo(15);
        log.info("PublicApiContractTest succeeded: last voucher is {}", response.lastNumber());
    }

    @Test
    void executeLastExportVoucherFlowUsingPublicApiOnly() throws Exception {
        server.stubLoginCmsSuccess();

        com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", server.baseUrl().getPort());
        server.stubWsfexLastVoucherSuccess();

        ArcaConfig config = new ArcaConfig(
                Cuit.parse("20-33333333-4"),
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(5),
                Duration.ofSeconds(5)
        );

        KeyStore keyStore = createKeyStore("CN=Test, SERIALNUMBER=20333333334");
        CertificateSource certificate = () -> keyStore;

        ArcaClient client = ArcaClient.builder()
                .config(config)
                .certificate(certificate)
                .build();

        var request = new io.github.fr4ncisx.arca.wsfexv1.model.LastExportVoucherRequest(1, (short) 19);
        var response = client.wsfexv1().getLastVoucher(request);

        assertThat(response).isNotNull();
        assertThat(response.lastNumber()).isEqualTo(42);
        log.info("PublicApiContractTest WSFEXv1 succeeded: last export voucher is {}", response.lastNumber());
    }

    @Test
    void executeLastWsmtxcaVoucherFlowUsingPublicApiOnly() throws Exception {
        server.stubLoginCmsSuccess();

        com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", server.baseUrl().getPort());
        server.stubWsmtxcaLastVoucherSuccess();

        ArcaConfig config = new ArcaConfig(
                Cuit.parse("20-33333333-4"),
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(5),
                Duration.ofSeconds(5)
        );

        KeyStore keyStore = createKeyStore("CN=Test, SERIALNUMBER=20333333334");
        CertificateSource certificate = () -> keyStore;

        ArcaClient client = ArcaClient.builder()
                .config(config)
                .certificate(certificate)
                .build();

        var request = new io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaLastVoucherRequest(1, (short) 1);
        var response = client.wsmtxca().getLastVoucher(request);

        assertThat(response).isNotNull();
        assertThat(response.lastVoucherNumber()).isEqualTo(123L);
        log.info("PublicApiContractTest WSMTXCA succeeded: last WSMTXCA voucher is {}", response.lastVoucherNumber());
    }


    private static void setEnvUrl(ArcaEnvironment env, String fieldName, URI value) throws Exception {
        java.lang.reflect.Field field = ArcaEnvironment.class.getDeclaredField(fieldName);
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

    private static void stubLastVoucherSoapSuccess() {
        String soapEnvelope = """
            <?xml version="1.0" encoding="utf-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fev="http://ar.gov.afip.dif.FEV1/">
               <soapenv:Body>
                  <fev:FECompUltimoAutorizadoResponse>
                     <fev:FECompUltimoAutorizadoResult>
                        <fev:PtoVta>1</fev:PtoVta>
                        <fev:CbteTipo>6</fev:CbteTipo>
                        <fev:CbteNro>15</fev:CbteNro>
                     </fev:FECompUltimoAutorizadoResult>
                  </fev:FECompUltimoAutorizadoResponse>
               </soapenv:Body>
            </soapenv:Envelope>
            """;

        com.github.tomakehurst.wiremock.client.WireMock.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.post(
                com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo("/wsfev1/service.asmx")
            )
            .withHeader("SOAPAction", com.github.tomakehurst.wiremock.client.WireMock.containing("FECompUltimoAutorizado"))
            .willReturn(
                com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml; charset=utf-8")
                    .withBody(soapEnvelope)
            )
        );
    }
}
