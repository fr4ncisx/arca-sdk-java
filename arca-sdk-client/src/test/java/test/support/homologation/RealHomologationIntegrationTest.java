package test.support.homologation;

import io.github.fr4ncisx.arca.client.spi.ArcaClient;
import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.spi.CertificateSource;
import io.github.fr4ncisx.arca.wsaa.spi.Pkcs12CertificateSource;
import io.github.fr4ncisx.arca.wsfev1.model.salespoint.SalesPoint;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real integration test suite that communicates directly with AFIP/ARCA homologation servers.
 * <p>
 * This test is opt-in and is excluded by default in unit test runs. It requires real
 * taxpayer certificates and configurations provided via environment variables.
 * If credentials are not present, it will skip execution gracefully using assumptions.
 *
 * @author fr4ncisx
 * @since 0.9.0
 */
class RealHomologationIntegrationTest {

    /**
     * Executes real homologation checks verifying WSAA login and sales point retrieval.
     */
    @Test
    void executeRealHomologationChecks() {
        String certPathStr = System.getenv("ARCA_TEST_CERT_PATH");
        String certPassword = System.getenv("ARCA_TEST_CERT_PASSWORD");
        String cuitStr = System.getenv("ARCA_TEST_CUIT");

        Assumptions.assumeTrue(certPathStr != null && !certPathStr.isBlank(),
                "ARCA_TEST_CERT_PATH environment variable is missing");
        Assumptions.assumeTrue(certPassword != null,
                "ARCA_TEST_CERT_PASSWORD environment variable is missing");
        Assumptions.assumeTrue(cuitStr != null && !cuitStr.isBlank(),
                "ARCA_TEST_CUIT environment variable is missing");

        Cuit cuit = Cuit.parse(cuitStr);
        ArcaConfig config = new ArcaConfig(
                cuit,
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(15),
                Duration.ofSeconds(15)
        );

        CertificateSource certificate = Pkcs12CertificateSource.fromPath(
                Path.of(certPathStr),
                certPassword.toCharArray()
        );

        ArcaClient client = ArcaClient.builder()
                .config(config)
                .certificate(certificate)
                .build();

        boolean pingOk = client.ping();
        assertThat(pingOk).isTrue();

        List<SalesPoint> salesPoints = client.wsfev1().getSalesPoints();
        assertThat(salesPoints).isNotNull();
    }
}
