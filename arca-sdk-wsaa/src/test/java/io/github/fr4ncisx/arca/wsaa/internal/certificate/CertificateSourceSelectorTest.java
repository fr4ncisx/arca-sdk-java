package io.github.fr4ncisx.arca.wsaa.internal.certificate;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.spi.CertificateSource;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CertificateSourceSelector}.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
@SuppressWarnings("null")
class CertificateSourceSelectorTest {

    private static final String PASSWORD = "password";
    private static final Cuit CUIT_A = Cuit.parse("20-33333333-4");
    private static final Cuit CUIT_B = Cuit.parse("27-44444444-9");

    @BeforeAll
    static void setUp() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
    }

    @Test
    void constructorRejectsNullCandidates() {
        assertThatThrownBy(() -> new CertificateSourceSelector(null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("candidates list must not be null");
    }

    @Test
    void selectRejectsNullConfig() {
        var selector = new CertificateSourceSelector(List.of());
        assertThatThrownBy(() -> selector.select(null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("config must not be null");
    }

    @Test
    void selectsSingleMatchingCertificateSourceBySerialNumber() throws Exception {
        var sourceA = new TestCertificateSource(createKeyStore("CN=Test A, SERIALNUMBER=20333333334"));
        var sourceB = new TestCertificateSource(createKeyStore("CN=Test B, SERIALNUMBER=27444444449"));

        var selector = new CertificateSourceSelector(List.of(sourceA, sourceB));

        var configA = new ArcaConfig(CUIT_A, ArcaEnvironment.HOMOLOGACION, Duration.ofSeconds(5), Duration.ofSeconds(5));
        var resolved = selector.select(configA);

        assertThat(resolved).isSameAs(sourceA);
    }

    @Test
    void selectsSingleMatchingCertificateSourceByOid() throws Exception {
        // OID 2.5.4.5 is the serialNumber attribute
        var sourceA = new TestCertificateSource(createKeyStore("CN=Test A, 2.5.4.5=20333333334"));
        var selector = new CertificateSourceSelector(List.of(sourceA));

        var configA = new ArcaConfig(CUIT_A, ArcaEnvironment.HOMOLOGACION, Duration.ofSeconds(5), Duration.ofSeconds(5));
        var resolved = selector.select(configA);

        assertThat(resolved).isSameAs(sourceA);
    }

    @Test
    void selectsSingleMatchingCertificateSourceWithCuitKeyword() throws Exception {
        var sourceA = new TestCertificateSource(createKeyStore("CN=Test A, SERIALNUMBER=CUIT 20333333334"));
        var selector = new CertificateSourceSelector(List.of(sourceA));

        var configA = new ArcaConfig(CUIT_A, ArcaEnvironment.HOMOLOGACION, Duration.ofSeconds(5), Duration.ofSeconds(5));
        var resolved = selector.select(configA);

        assertThat(resolved).isSameAs(sourceA);
    }

    @Test
    void selectThrowsExceptionWhenNoMatchFound() throws Exception {
        var sourceB = new TestCertificateSource(createKeyStore("CN=Test B, SERIALNUMBER=27444444449"));
        var selector = new CertificateSourceSelector(List.of(sourceB));

        var configA = new ArcaConfig(CUIT_A, ArcaEnvironment.HOMOLOGACION, Duration.ofSeconds(5), Duration.ofSeconds(5));

        assertThatThrownBy(() -> selector.select(configA))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("No registered CertificateSource matches target CUIT");
    }

    @Test
    void selectThrowsExceptionWhenAmbiguityDetected() throws Exception {
        // Two sources with the same CUIT
        var sourceA1 = new TestCertificateSource(createKeyStore("CN=Test A1, SERIALNUMBER=20333333334"));
        var sourceA2 = new TestCertificateSource(createKeyStore("CN=Test A2, SERIALNUMBER=20333333334"));

        var selector = new CertificateSourceSelector(List.of(sourceA1, sourceA2));

        var configA = new ArcaConfig(CUIT_A, ArcaEnvironment.HOMOLOGACION, Duration.ofSeconds(5), Duration.ofSeconds(5));

        assertThatThrownBy(() -> selector.select(configA))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("Ambiguity detected: multiple compatible CertificateSources found");
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
        keyStore.setKeyEntry("wsaa", keyPair.getPrivate(), PASSWORD.toCharArray(), new Certificate[]{cert});
        return keyStore;
    }

    private static class TestCertificateSource implements CertificateSource {
        private final KeyStore keyStore;

        TestCertificateSource(KeyStore keyStore) {
            this.keyStore = keyStore;
        }

        @Override
        public KeyStore load() {
            return keyStore;
        }
    }
}
