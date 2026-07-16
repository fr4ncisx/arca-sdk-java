package io.github.fr4ncisx.arca.spring;

import io.github.fr4ncisx.arca.client.spi.ArcaClient;
import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.registry.spi.RegistryClient;
import io.github.fr4ncisx.arca.wsaa.spi.CertificateSource;
import io.github.fr4ncisx.arca.wsaa.spi.Pkcs12CertificateSource;
import io.github.fr4ncisx.arca.wscdc.spi.WscdcClient;
import io.github.fr4ncisx.arca.wsfev1.spi.WsfeClient;
import io.github.fr4ncisx.arca.wsfexv1.spi.WsfexClient;
import io.github.fr4ncisx.arca.wsmtxca.spi.WsmtxcaClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Spring Boot Auto-Configuration for the ARCA SDK.
 * <p>
 * Registers beans for {@link ArcaClient} and its associated sub-clients if not
 * already defined by the user.
 *
 * @author fr4ncisx
 * @since 1.2.0
 */
@AutoConfiguration
@ConditionalOnClass(ArcaClient.class)
@EnableConfigurationProperties(ArcaProperties.class)
final class ArcaAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "arca", name = {"cuit", "certificate-location"})
    @ConditionalOnMissingBean(ArcaClient.class)
    static class ClientConfiguration {

        @Bean
        @ConditionalOnMissingBean
        CertificateSource arcaCertificateSource(ArcaProperties properties) {
            Resource location = properties.certificateLocation();
            String password = properties.certificatePassword();

            if (location == null || password == null) {
                throw new ApplicationContextException(
                    "Mandatory properties 'arca.certificate-location' and 'arca.certificate-password' must be configured."
                );
            }

            try {
                Path certPath = resolveCertificatePath(location);
                return Pkcs12CertificateSource.fromPath(certPath, password.toCharArray());
            } catch (IOException e) {
                throw new ApplicationContextException(
                    "Failed to resolve ARCA certificate from location: " + location, e
                );
            }
        }

        @Bean
        @ConditionalOnMissingBean
        ArcaClient arcaClient(ArcaProperties properties, CertificateSource certificateSource) {
            if (properties.cuit() == null) {
                throw new ApplicationContextException(
                    "Mandatory property 'arca.cuit' is not configured."
                );
            }

            Cuit cuit = Cuit.parse(properties.cuit());
            ArcaConfig config = new ArcaConfig(
                cuit,
                properties.environment(),
                properties.connectTimeout(),
                properties.readTimeout(),
                properties.resilienceEnabled()
            );

            return ArcaClient.builder()
                .config(config)
                .certificate(certificateSource)
                .build();
        }
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ArcaClient.class)
    WsfeClient arcaWsfeClient(ArcaClient arcaClient) {
        return arcaClient.wsfev1();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ArcaClient.class)
    RegistryClient arcaRegistryClient(ArcaClient arcaClient) {
        return arcaClient.registry();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ArcaClient.class)
    WsfexClient arcaWsfexClient(ArcaClient arcaClient) {
        return arcaClient.wsfexv1();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ArcaClient.class)
    WsmtxcaClient arcaWsmtxcaClient(ArcaClient arcaClient) {
        return arcaClient.wsmtxca();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ArcaClient.class)
    WscdcClient arcaWscdcClient(ArcaClient arcaClient) {
        return arcaClient.wscdc();
    }

    private static Path resolveCertificatePath(Resource resource) throws IOException {
        try {
            return resource.getFile().toPath();
        } catch (IOException e) {
            Path tempFile = Files.createTempFile("arca-cert-", ".p12");
            try (InputStream in = resource.getInputStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            tempFile.toFile().deleteOnExit();
            return tempFile;
        }
    }
}
