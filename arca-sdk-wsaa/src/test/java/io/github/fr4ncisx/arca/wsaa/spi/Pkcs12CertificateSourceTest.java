package io.github.fr4ncisx.arca.wsaa.spi;

import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Pkcs12CertificateSource}.
 * <p>
 * These tests verify factory validation, deferred file-system access, valid
 * PKCS#12 loading, authentication error translation, expired certificate
 * rejection, and non-cached loading semantics.
 *
 * @author fr4ncisx
 * @since 0.1.0-M2
 */
class Pkcs12CertificateSourceTest {

    private static final String KEY_STORE_TYPE = "PKCS12";
    private static final String KEY_ALIAS = "wsaa";
    private static final String VALID_PASSWORD = "changeit";
    private static final String WRONG_PASSWORD = "wrong-password";
    private static final String EXPIRED_CERTIFICATE_START_DATE = "2020/01/01 00:00:00";

    @TempDir
    private Path tempDir;

    /**
     * Verifies that {@link Pkcs12CertificateSource#fromPath(Path, char[])}
     * rejects a null path as a local validation error.
     */
    @Test
    void fromPathShouldRejectNullPath() {
        char[] password = validPassword();

        assertThatThrownBy(() -> Pkcs12CertificateSource.fromPath(null, password))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("the path must not be null");
    }

    /**
     * Verifies that {@link Pkcs12CertificateSource#fromPath(Path, char[])}
     * rejects a null password as a local validation error.
     */
    @Test
    void fromPathShouldRejectNullPassword() {
        Path path = tempDir.resolve("certificate.p12");

        assertThatThrownBy(() -> Pkcs12CertificateSource.fromPath(path, null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("the password must not be null");
    }

    /**
     * Verifies that {@link Pkcs12CertificateSource#fromPath(Path, char[])}
     * rejects an empty password as a local validation error.
     */
    @Test
    void fromPathShouldRejectEmptyPassword() {
        Path path = tempDir.resolve("certificate.p12");
        char[] password = new char[0];

        assertThatThrownBy(() -> Pkcs12CertificateSource.fromPath(path, password))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("the password must not be empty");
    }

    /**
     * Verifies that {@link Pkcs12CertificateSource#fromPath(Path, char[])}
     * validates only local construction arguments and does not access the file
     * system during construction.
     */
    @Test
    void fromPathShouldNotTouchFileSystemOrValidateCertificateContent() {
        Path missingPath = tempDir.resolve("missing-at-construction.p12");
        char[] password = validPassword();

        Pkcs12CertificateSource source =
                Pkcs12CertificateSource.fromPath(missingPath, password);

        assertThat(source).isNotNull();
    }

    /**
     * Verifies that a consumer can use the implementation through the
     * {@link CertificateSource} port and load a valid PKCS#12 key store.
     *
     * @throws Exception if the test PKCS#12 fixture cannot be generated or
     *                   loaded.
     */
    @Test
    void shouldBuildCertificateSourceFromPathAndLoadValidPkcs12() throws Exception {
        Path path = createValidPkcs12("valid-source.p12");
        char[] password = validPassword();

        CertificateSource source = Pkcs12CertificateSource.fromPath(path, password);

        KeyStore keyStore = source.load();

        assertThat(keyStore).isNotNull();
        assertThat(keyStore.getType()).isEqualTo(KEY_STORE_TYPE);
        assertThat(keyStore.isKeyEntry(KEY_ALIAS)).isTrue();
        assertThat(keyStore.getCertificate(KEY_ALIAS))
                .isInstanceOf(X509Certificate.class);
    }

    /**
     * Verifies that every {@link Pkcs12CertificateSource#load()} invocation
     * opens and validates the configured file again instead of returning a
     * cached {@link KeyStore}.
     *
     * @throws Exception if the test PKCS#12 fixture cannot be generated or
     *                   loaded.
     */
    @Test
    void loadShouldReloadFileOnEveryInvocation() throws Exception {
        Path path = createValidPkcs12("reloadable-source.p12");
        char[] password = validPassword();

        Pkcs12CertificateSource source =
                Pkcs12CertificateSource.fromPath(path, password);

        KeyStore firstLoad = source.load();
        Files.writeString(path, "not-a-pkcs12-anymore");

        assertThat(firstLoad).isNotNull();

        assertThatThrownBy(source::load)
                .isInstanceOf(ArcaAuthException.class)
                .hasMessage("Unable to load PKCS#12 certificate material");
    }

    /**
     * Verifies that a missing PKCS#12 file is translated to
     * {@link ArcaAuthException} during {@link Pkcs12CertificateSource#load()}.
     */
    @Test
    void loadShouldRejectMissingFile() {
        Path missingPath = tempDir.resolve("missing.p12");
        char[] password = validPassword();

        Pkcs12CertificateSource source =
                Pkcs12CertificateSource.fromPath(missingPath, password);

        assertThatThrownBy(source::load)
                .isInstanceOf(ArcaAuthException.class)
                .hasMessage("PKCS#12 certificate file does not exist")
                .hasMessageNotContaining(missingPath.toString())
                .hasMessageNotContaining(String.valueOf(password));
    }

    /**
     * Verifies that an incorrect PKCS#12 password is translated to
     * {@link ArcaAuthException} without exposing the password or certificate
     * path in the error message.
     *
     * @throws Exception if the test PKCS#12 fixture cannot be generated.
     */
    @Test
    void loadShouldRejectIncorrectPasswordWithoutLeakingSecrets() throws Exception {
        Path path = createValidPkcs12("wrong-password-source.p12");
        char[] wrongPassword = wrongPassword();

        Pkcs12CertificateSource source =
                Pkcs12CertificateSource.fromPath(path, wrongPassword);

        assertThatThrownBy(source::load)
                .isInstanceOf(ArcaAuthException.class)
                .hasMessage("Unable to load PKCS#12 certificate material")
                .hasMessageNotContaining(path.toString())
                .hasMessageNotContaining(String.valueOf(wrongPassword));
    }

    /**
     * Verifies that invalid PKCS#12 content is translated to
     * {@link ArcaAuthException}.
     *
     * @throws Exception if the invalid fixture file cannot be written.
     */
    @Test
    void loadShouldRejectInvalidPkcs12Format() throws Exception {
        Path path = tempDir.resolve("invalid-format.p12");
        char[] password = validPassword();
        Files.writeString(path, "this-is-not-a-valid-pkcs12-file");

        Pkcs12CertificateSource source =
                Pkcs12CertificateSource.fromPath(path, password);

        assertThatThrownBy(source::load)
                .isInstanceOf(ArcaAuthException.class)
                .hasMessage("Unable to load PKCS#12 certificate material")
                .hasMessageNotContaining(path.toString())
                .hasMessageNotContaining(String.valueOf(password));
    }

    /**
     * Verifies that an expired X.509 certificate inside a valid PKCS#12 file is
     * rejected before the authentication flow can use it for signing.
     *
     * @throws Exception if the expired PKCS#12 fixture cannot be generated.
     */
    @Test
    void loadShouldRejectExpiredCertificate() throws Exception {
        Path path = createExpiredPkcs12("expired-source.p12");
        char[] password = validPassword();

        Pkcs12CertificateSource source =
                Pkcs12CertificateSource.fromPath(path, password);

        assertThatThrownBy(source::load)
                .isInstanceOf(ArcaAuthException.class)
                .hasMessage("PKCS#12 certificate is expired")
                .hasMessageNotContaining(path.toString())
                .hasMessageNotContaining(String.valueOf(password));
    }

    /**
     * Creates a valid PKCS#12 fixture using the JDK {@code keytool}
     * executable.
     *
     * @param fileName fixture file name.
     * @return path to the generated PKCS#12 file.
     * @throws Exception if {@code keytool} cannot create the fixture.
     */
    private Path createValidPkcs12(String fileName) throws Exception {
        return createPkcs12(fileName, List.of("-validity", "3650"));
    }

    /**
     * Creates an expired PKCS#12 fixture using the JDK {@code keytool}
     * executable.
     *
     * @param fileName fixture file name.
     * @return path to the generated PKCS#12 file.
     * @throws Exception if {@code keytool} cannot create the fixture.
     */
    private Path createExpiredPkcs12(String fileName) throws Exception {
        return createPkcs12(
                fileName,
                List.of(
                        "-startdate",
                        EXPIRED_CERTIFICATE_START_DATE,
                        "-validity",
                        "1"
                )
        );
    }

    /**
     * Creates a PKCS#12 fixture containing one private-key entry and one X.509
     * certificate.
     *
     * @param fileName          fixture file name.
     * @param validityArguments validity-related arguments passed to
     *                          {@code keytool}.
     * @return path to the generated PKCS#12 file.
     * @throws Exception if {@code keytool} cannot create the fixture.
     */
    private Path createPkcs12(String fileName, List<String> validityArguments)
            throws Exception {
        Path path = tempDir.resolve(fileName);

        List<String> command = new ArrayList<>(List.of(
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
                "CN=ARCA SDK Test",
                "-keystore",
                path.toString(),
                "-storetype",
                KEY_STORE_TYPE,
                "-storepass",
                VALID_PASSWORD,
                "-keypass",
                VALID_PASSWORD,
                "-noprompt"
        ));
        command.addAll(validityArguments);

        runKeytool(command);

        return path;
    }

    /**
     * Runs a {@code keytool} command and fails with diagnostic output if the
     * command exits with an error.
     *
     * @param command complete command line.
     * @throws IOException          if the process cannot be started or its output cannot
     *                              be read.
     * @throws InterruptedException if the current thread is interrupted while
     *                              waiting for the process to finish.
     */
    private static void runKeytool(List<String> command)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        int exitCode = waitFor(process);

        assertThat(exitCode)
                .as("keytool output:%n%s", output)
                .isZero();
    }

    /**
     * Waits for a process to finish and restores the interrupted flag if the
     * current thread is interrupted.
     *
     * @param process process to wait for.
     * @return process exit code.
     * @throws InterruptedException if the current thread is interrupted while
     *                              waiting.
     */
    private static int waitFor(Process process) throws InterruptedException {
        try {
            return process.waitFor();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        }
    }

    /**
     * Resolves the {@code keytool} executable from the active JDK.
     *
     * @return path to the JDK {@code keytool} executable.
     */
    private static Path keytoolExecutable() {
        String executableName = isWindows() ? "keytool.exe" : "keytool";
        Path executable = Path.of(
                System.getProperty("java.home"),
                "bin",
                executableName
        );

        assertThat(executable)
                .as("keytool executable")
                .exists()
                .isRegularFile();

        return executable;
    }

    /**
     * Checks whether the current operating system is Windows.
     *
     * @return true when the current operating system is Windows.
     */
    private static boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    /**
     * Returns the valid password used to create successful PKCS#12 fixtures.
     *
     * @return valid PKCS#12 password.
     */
    private static char[] validPassword() {
        return VALID_PASSWORD.toCharArray();
    }

    /**
     * Returns a password that does not match the generated PKCS#12 fixtures.
     *
     * @return incorrect PKCS#12 password.
     */
    private static char[] wrongPassword() {
        return WRONG_PASSWORD.toCharArray();
    }
}