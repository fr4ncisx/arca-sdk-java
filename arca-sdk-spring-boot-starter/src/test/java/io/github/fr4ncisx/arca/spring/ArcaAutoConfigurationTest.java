package io.github.fr4ncisx.arca.spring;

import io.github.fr4ncisx.arca.client.spi.ArcaClient;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.registry.spi.RegistryClient;
import io.github.fr4ncisx.arca.wsaa.spi.CertificateSource;
import io.github.fr4ncisx.arca.wscdc.spi.WscdcClient;
import io.github.fr4ncisx.arca.wsfev1.spi.WsfeClient;
import io.github.fr4ncisx.arca.wsfexv1.spi.WsfexClient;
import io.github.fr4ncisx.arca.wsmtxca.spi.WsmtxcaClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ArcaAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ArcaAutoConfiguration.class));

    @TempDir
    private Path tempDir;

    @Test
    void shouldAutoconfigureBeansWhenPropertiesProvided() throws IOException {
        Path tempCert = Files.createTempFile(tempDir, "test-cert", ".p12");
        Files.writeString(tempCert, "dummy-pkcs12-bytes");
        String certPath = tempCert.toAbsolutePath().toString();

        contextRunner
            .withPropertyValues(
                "arca.cuit=20-33333333-4",
                "arca.certificate-location=file:" + certPath,
                "arca.certificate-password=changeit",
                "arca.environment=HOMOLOGACION",
                "arca.resilience-enabled=false"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(CertificateSource.class);
                assertThat(context).hasSingleBean(ArcaClient.class);
                assertThat(context).hasSingleBean(WsfeClient.class);
                assertThat(context).hasSingleBean(RegistryClient.class);
                assertThat(context).hasSingleBean(WsfexClient.class);
                assertThat(context).hasSingleBean(WsmtxcaClient.class);
                assertThat(context).hasSingleBean(WscdcClient.class);
            });
    }

    @Test
    void shouldConfigureCustomResilienceParametersFromProperties() throws IOException {
        Path tempCert = Files.createTempFile(tempDir, "test-cert", ".p12");
        Files.writeString(tempCert, "dummy-pkcs12-bytes");
        String certPath = tempCert.toAbsolutePath().toString();

        contextRunner
            .withPropertyValues(
                "arca.cuit=20-33333333-4",
                "arca.certificate-location=file:" + certPath,
                "arca.certificate-password=changeit",
                "arca.resilience.max-attempts=5",
                "arca.resilience.base-delay=2s",
                "arca.resilience.max-consecutive-failures=10",
                "arca.resilience.reset-timeout=45s"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(ArcaProperties.class);
                ArcaProperties properties = context.getBean(ArcaProperties.class);
                assertThat(properties.resilience()).isNotNull();
                assertThat(properties.resilience().maxAttempts()).isEqualTo(5);
                assertThat(properties.resilience().baseDelay()).isEqualTo(Duration.ofSeconds(2));
                assertThat(properties.resilience().maxConsecutiveFailures()).isEqualTo(10);
                assertThat(properties.resilience().resetTimeout()).isEqualTo(Duration.ofSeconds(45));
            });
    }

    @Test
    void shouldAutoconfigureFromNonFileResource() {
        Resource nonFileResource = new ByteArrayResource(new byte[]{1, 2, 3});
        contextRunner
            .withPropertyValues(
                "arca.cuit=20-33333333-4",
                "arca.certificate-location=dummy"
            )
            .withBean("arca-io.github.fr4ncisx.arca.spring.ArcaProperties", ArcaProperties.class, () -> new ArcaProperties(
                "20-33333333-4",
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                true,
                nonFileResource,
                "changeit"
            ))
            .run(context -> {
                assertThat(context).hasSingleBean(CertificateSource.class);
                assertThat(context).hasSingleBean(ArcaClient.class);
            });
    }

    /**
     * Verifies that wiring the certificate source from a non-file resource
     * never writes private key material to a temporary file on disk.
     */
    @Test
    void shouldNotWriteCertificateMaterialToTempFiles() {
        Resource nonFileResource = new ByteArrayResource(new byte[]{1, 2, 3});
        long leakedFilesBefore = countLeakedCertTempFiles();

        contextRunner
            .withPropertyValues(
                "arca.cuit=20-33333333-4",
                "arca.certificate-location=dummy"
            )
            .withBean("arca-io.github.fr4ncisx.arca.spring.ArcaProperties", ArcaProperties.class, () -> new ArcaProperties(
                "20-33333333-4",
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                true,
                nonFileResource,
                "changeit"
            ))
            .run(context -> assertThat(context).hasSingleBean(CertificateSource.class));

        assertThat(countLeakedCertTempFiles()).isEqualTo(leakedFilesBefore);
    }

    private static long countLeakedCertTempFiles() {
        try (var stream = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return stream
                .map(path -> path.getFileName().toString())
                .filter(name -> name.startsWith("arca-cert-"))
                .count();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to inspect temp directory", e);
        }
    }

    @Test
    void shouldNotConfigureBeansWhenPropertiesAreMissing() {
        contextRunner
            .run(context -> {
                assertThat(context).doesNotHaveBean(ArcaClient.class);
                assertThat(context).doesNotHaveBean(CertificateSource.class);
                assertThat(context).doesNotHaveBean(WsfeClient.class);
            });
    }

    @Test
    void shouldBackOffWhenCustomArcaClientBeanProvided() {
        contextRunner
            .withUserConfiguration(CustomClientConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(ArcaClient.class);
                assertThat(context).getBean(ArcaClient.class).isSameAs(CustomClientConfiguration.MOCK_CLIENT);

                assertThat(context).hasSingleBean(WsfeClient.class);
                assertThat(context).hasSingleBean(RegistryClient.class);
            });
    }

    @Test
    void shouldBackOffWhenCustomSubClientBeanProvided() throws IOException {
        Path tempCert = Files.createTempFile(tempDir, "test-cert", ".p12");
        Files.writeString(tempCert, "dummy-pkcs12-bytes");
        String certPath = tempCert.toAbsolutePath().toString();

        contextRunner
            .withUserConfiguration(CustomSubClientConfiguration.class)
            .withPropertyValues(
                "arca.cuit=20-33333333-4",
                "arca.certificate-location=file:" + certPath,
                "arca.certificate-password=changeit"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(WsfeClient.class);
                assertThat(context).getBean(WsfeClient.class).isSameAs(CustomSubClientConfiguration.MOCK_WSFE);

                assertThat(context).hasSingleBean(RegistryClient.class);
                assertThat(context).getBean(RegistryClient.class).isNotSameAs(CustomSubClientConfiguration.MOCK_WSFE);
            });
    }

    @SuppressWarnings("unchecked")
    private static <T> T dummy(Class<T> type) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[]{type},
            (proxy, method, args) -> null
        );
    }

    @Configuration
    static class CustomClientConfiguration {
        static final WsfeClient MOCK_WSFE = dummy(WsfeClient.class);
        static final RegistryClient MOCK_REGISTRY = dummy(RegistryClient.class);
        static final WsfexClient MOCK_WSFEX = dummy(WsfexClient.class);
        static final WsmtxcaClient MOCK_WSMTXCA = dummy(WsmtxcaClient.class);
        static final WscdcClient MOCK_WSCDC = dummy(WscdcClient.class);
        static final ArcaClient MOCK_CLIENT = new ArcaClient(
                MOCK_WSFE, MOCK_REGISTRY, MOCK_WSFEX, MOCK_WSMTXCA, MOCK_WSCDC);

        @Bean
        ArcaClient arcaClient() {
            return MOCK_CLIENT;
        }
    }

    @Configuration
    static class CustomSubClientConfiguration {
        static final WsfeClient MOCK_WSFE = dummy(WsfeClient.class);

        @Bean
        WsfeClient customWsfeClient() {
            return MOCK_WSFE;
        }
    }
}
