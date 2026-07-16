package io.github.fr4ncisx.arca.spring;

import io.github.fr4ncisx.arca.client.spi.ArcaClient;
import io.github.fr4ncisx.arca.registry.spi.RegistryClient;
import io.github.fr4ncisx.arca.wsaa.spi.CertificateSource;
import io.github.fr4ncisx.arca.wscdc.spi.WscdcClient;
import io.github.fr4ncisx.arca.wsfev1.spi.WsfeClient;
import io.github.fr4ncisx.arca.wsfexv1.spi.WsfexClient;
import io.github.fr4ncisx.arca.wsmtxca.spi.WsmtxcaClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ArcaAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ArcaAutoConfiguration.class));

    @TempDir
    private Path tempDir;

    @Test
    void shouldAutoconfigureBeansWhenPropertiesProvided() throws IOException {
        Path tempCert = Files.createTempFile(tempDir, "test-cert", ".p12");
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
    void shouldAutoconfigureFromNonFileResource() {
        org.springframework.core.io.Resource nonFileResource = new org.springframework.core.io.ByteArrayResource(new byte[]{1, 2, 3});
        contextRunner
            .withPropertyValues(
                "arca.cuit=20-33333333-4",
                "arca.certificate-location=dummy"
            )
            .withBean("arca-io.github.fr4ncisx.arca.spring.ArcaProperties", ArcaProperties.class, () -> new ArcaProperties(
                "20-33333333-4",
                io.github.fr4ncisx.arca.core.config.ArcaEnvironment.HOMOLOGACION,
                java.time.Duration.ofSeconds(10),
                java.time.Duration.ofSeconds(30),
                true,
                nonFileResource,
                "changeit"
            ))
            .run(context -> {
                assertThat(context).hasSingleBean(CertificateSource.class);
                assertThat(context).hasSingleBean(ArcaClient.class);
            });
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

    @Configuration
    static class CustomClientConfiguration {
        static final ArcaClient MOCK_CLIENT = Mockito.mock(ArcaClient.class);
        static final WsfeClient MOCK_WSFE = Mockito.mock(WsfeClient.class);
        static final RegistryClient MOCK_REGISTRY = Mockito.mock(RegistryClient.class);
        static final WsfexClient MOCK_WSFEX = Mockito.mock(WsfexClient.class);
        static final WsmtxcaClient MOCK_WSMTXCA = Mockito.mock(WsmtxcaClient.class);
        static final WscdcClient MOCK_WSCDC = Mockito.mock(WscdcClient.class);

        static {
            Mockito.when(MOCK_CLIENT.wsfev1()).thenReturn(MOCK_WSFE);
            Mockito.when(MOCK_CLIENT.registry()).thenReturn(MOCK_REGISTRY);
            Mockito.when(MOCK_CLIENT.wsfexv1()).thenReturn(MOCK_WSFEX);
            Mockito.when(MOCK_CLIENT.wsmtxca()).thenReturn(MOCK_WSMTXCA);
            Mockito.when(MOCK_CLIENT.wscdc()).thenReturn(MOCK_WSCDC);
        }

        @Bean
        ArcaClient arcaClient() {
            return MOCK_CLIENT;
        }
    }

    @Configuration
    static class CustomSubClientConfiguration {
        static final WsfeClient MOCK_WSFE = Mockito.mock(WsfeClient.class);

        @Bean
        WsfeClient customWsfeClient() {
            return MOCK_WSFE;
        }
    }
}
