package io.github.fr4ncisx.arca.soap.internal.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.Security;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SoapHardeningUtil}.
 * <p>
 * Validates XML safety properties and TLS certificate revocation settings in an isolated JVM.
 *
 * @author fr4ncisx
 * @since 1.2.4
 */
class SoapHardeningUtilTest {

    private static final String ACCESS_EXTERNAL_DTD = "javax.xml.accessExternalDTD";
    private static final String ACCESS_EXTERNAL_SCHEMA = "javax.xml.accessExternalSchema";
    private static final String ACCESS_EXTERNAL_STYLESHEET = "javax.xml.accessExternalStylesheet";
    private static final String ENABLE_OCSP = "ocsp.enable";
    private @Nullable String originalAccessExternalDtd;
    private @Nullable String originalAccessExternalSchema;
    private @Nullable String originalAccessExternalStylesheet;

    /**
     * Captures the current JVM-wide settings before the test changes them.
     */
    @BeforeEach
    void preserveSecurityProperties() {
        originalAccessExternalDtd = System.getProperty(ACCESS_EXTERNAL_DTD);
        originalAccessExternalSchema = System.getProperty(ACCESS_EXTERNAL_SCHEMA);
        originalAccessExternalStylesheet = System.getProperty(ACCESS_EXTERNAL_STYLESHEET);
    }

    /**
     * Restores the prior JVM-wide settings after the test.
     */
    @AfterEach
    void tearDown() {
        restoreSystemProperty(ACCESS_EXTERNAL_DTD, originalAccessExternalDtd);
        restoreSystemProperty(ACCESS_EXTERNAL_SCHEMA, originalAccessExternalSchema);
        restoreSystemProperty(ACCESS_EXTERNAL_STYLESHEET, originalAccessExternalStylesheet);
    }

    /**
     * Verifies that applying XML hardening sets the expected restriction properties.
     *
     * @since 1.2.4
     */
    @Test
    void appliesXmlHardeningProperties() {
        SoapHardeningUtil.apply();

        assertThat(System.getProperty(ACCESS_EXTERNAL_DTD)).isEmpty();
        assertThat(System.getProperty(ACCESS_EXTERNAL_SCHEMA)).isEmpty();
        assertThat(System.getProperty(ACCESS_EXTERNAL_STYLESHEET)).isEmpty();
    }

    /**
     * Verifies that TLS revocation properties are changed only in an isolated JVM.
     * <p>
     * The property-only probe uses a small bounded heap instead of reserving
     * an application-sized heap. Crash diagnostics remain available on failure.
     *
     * @param diagnosticsDirectory temporary directory retained if the probe fails.
     * @throws IOException if the probe process cannot be started or read.
     * @throws InterruptedException if waiting for the probe process is interrupted.
     */
    @Test
    void enablesTlsRevocationCheckingPropertiesInIsolatedJvm(
            @TempDir(cleanup = CleanupMode.ON_SUCCESS) Path diagnosticsDirectory)
            throws IOException, InterruptedException {
        @Nullable String originalOcspProperty = Security.getProperty(ENABLE_OCSP);
        String executableName = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", executableName);
        Process process = new ProcessBuilder(
                javaExecutable.toString(),
                "-Xms16m",
                "-Xmx64m",
                "-XX:ErrorFile=" + diagnosticsDirectory.resolve("hs_err_pid%p.log"),
                "-cp",
                System.getProperty("java.class.path"),
                TlsRevocationPropertyProbe.class.getName())
                .redirectErrorStream(true)
                .start();
        boolean exited = process.waitFor(10, TimeUnit.SECONDS);
        if (!exited) {
            process.destroyForcibly();
        }
        assertThat(exited).isTrue();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();

        assertThat(process.exitValue()).as("Isolated TLS JVM output: %s", output).isZero();
        assertThat(output).isEqualTo("true|true|true");
        assertThat(Security.getProperty(ENABLE_OCSP)).isEqualTo(originalOcspProperty);
    }

    private static void restoreSystemProperty(String name, @Nullable String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
