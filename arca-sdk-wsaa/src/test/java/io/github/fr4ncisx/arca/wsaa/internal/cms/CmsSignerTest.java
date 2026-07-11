package io.github.fr4ncisx.arca.wsaa.internal.cms;

import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.wsaa.spi.Pkcs12CertificateSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.Security;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.jspecify.annotations.NullMarked;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CmsSigner}.
 * <p>
 * Verifies that XML payloads can be signed into valid Base64-encoded CMS SignedData
 * envelopes, and that cryptographic errors are correctly mapped.
 *
 * @author fr4ncisx
 * @since 0.1.0-M4
 */
@NullMarked
class CmsSignerTest {

    private static final String KEY_STORE_TYPE = "PKCS12";
    private static final String KEY_ALIAS = "wsaa";
    private static final String VALID_PASSWORD = "changeit";

    @TempDir
    private Path tempDir;

    private Pkcs12CertificateSource certificateSource;

    @BeforeEach
    void setUp() throws Exception {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        Path p12Path = createPkcs12Fixture();
        certificateSource = Pkcs12CertificateSource.fromPath(p12Path, VALID_PASSWORD.toCharArray());
    }

    /**
     * Verifies that {@link CmsSigner#sign(String)} rejects a null XML payload.
     */
    @Test
    void signShouldRejectNullXml() {
        CmsSigner signer = new CmsSigner(certificateSource, VALID_PASSWORD.toCharArray());

        assertThatThrownBy(() -> signer.sign(null))
                .isInstanceOf(ArcaAuthException.class)
                .hasMessage("The XML payload to sign cannot be null.");
    }

    /**
     * Verifies that passing a null {@link io.github.fr4ncisx.arca.wsaa.spi.CertificateSource}
     * to the constructor throws {@link io.github.fr4ncisx.arca.core.exception.ArcaValidationException}.
     */
    @Test
    void constructorShouldRejectNullCertificateSource() {
        assertThatThrownBy(() -> new CmsSigner(null, VALID_PASSWORD.toCharArray()))
                .isInstanceOf(io.github.fr4ncisx.arca.core.exception.ArcaValidationException.class)
                .hasMessageContaining("certificateSource must not be null");
    }

    /**
     * Verifies that passing a null key password to the constructor
     * throws {@link io.github.fr4ncisx.arca.core.exception.ArcaValidationException}.
     */
    @Test
    void constructorShouldRejectNullKeyPassword() {
        assertThatThrownBy(() -> new CmsSigner(certificateSource, null))
                .isInstanceOf(io.github.fr4ncisx.arca.core.exception.ArcaValidationException.class)
                .hasMessageContaining("keyPassword must not be null");
    }

    /**
     * Verifies that {@link CmsSigner#sign(String)} signs a valid XML payload
     * producing a valid Base64 CMS Envelope that can be parsed and verified.
     */
    @Test
    void signShouldProduceValidCmsSignedData() throws Exception {
        CmsSigner signer = new CmsSigner(certificateSource, VALID_PASSWORD.toCharArray());
        String xml = "<test>content</test>";

        String cmsBase64 = signer.sign(xml);

        assertThat(cmsBase64).isNotBlank();

        byte[] decodedCms = Base64.getDecoder().decode(cmsBase64);
        CMSSignedData signedData = new CMSSignedData(decodedCms);

        assertThat(signedData).isNotNull();
        assertThat(signedData.getSignedContent()).isNotNull();

        byte[] signedContent = (byte[]) signedData.getSignedContent().getContent();
        String contentString = new String(signedContent, StandardCharsets.UTF_8);

        assertThat(contentString).isEqualTo(xml);
    }

    /**
     * Verifies that using an incorrect private key password throws {@link ArcaAuthException}.
     */
    @Test
    void signShouldThrowOnIncorrectPassword() {
        CmsSigner signer = new CmsSigner(certificateSource, "wrong-pass".toCharArray());
        String xml = "<test>content</test>";

        assertThatThrownBy(() -> signer.sign(xml))
                .isInstanceOf(ArcaAuthException.class)
                .hasMessageContaining("Failed to generate PKCS#7 (CMS) signature");
    }

    private Path createPkcs12Fixture() throws Exception {
        Path path = tempDir.resolve("test-signer.p12");
        List<String> command = List.of(
                keytoolExecutable().toString(),
                "-genkeypair",
                "-alias",
                KEY_ALIAS,
                "-keyalg",
                "RSA",
                "-keysize",
                "2048",
                "-sigalg",
                "SHA256withRSA",
                "-dname",
                "CN=ARCA SDK Test Signer",
                "-keystore",
                path.toString(),
                "-storetype",
                KEY_STORE_TYPE,
                "-storepass",
                VALID_PASSWORD,
                "-keypass",
                VALID_PASSWORD,
                "-validity",
                "365",
                "-noprompt"
        );

        runKeytool(command);
        return path;
    }

    private static void runKeytool(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();

        assertThat(exitCode)
                .as("keytool output:%n%s", output)
                .isZero();
    }

    private static Path keytoolExecutable() {
        String executableName = isWindows() ? "keytool.exe" : "keytool";
        Path executable = Path.of(System.getProperty("java.home"), "bin", executableName);

        assertThat(executable)
                .as("keytool executable")
                .exists()
                .isRegularFile();

        return executable;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }
}
