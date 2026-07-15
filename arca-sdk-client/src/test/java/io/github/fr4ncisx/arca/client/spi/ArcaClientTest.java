package io.github.fr4ncisx.arca.client.spi;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.spi.CertificateSource;
import io.github.fr4ncisx.arca.wsfev1.spi.WsfeClient;
import io.github.fr4ncisx.arca.wsfexv1.spi.WsfexClient;
import io.github.fr4ncisx.arca.wsmtxca.spi.WsmtxcaClient;
import io.github.fr4ncisx.arca.wscdc.spi.WscdcClient;
import io.github.fr4ncisx.arca.registry.spi.RegistryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link ArcaClient} facade and its builder.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
class ArcaClientTest {

    private ArcaConfig config;
    private CertificateSource certificate;

    @BeforeEach
    void setUp() {
        config = new ArcaConfig(
                Cuit.parse("20-33333333-4"),
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(2),
                Duration.ofSeconds(2)
        );
        certificate = mock(CertificateSource.class);
    }

    @Test
    void builderFailsWithoutConfig() {
        var builder = ArcaClient.builder().certificate(certificate);
        assertThatThrownBy(builder::build)
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("config must not be null");
    }

    @Test
    void builderFailsWithoutCertificate() {
        var builder = ArcaClient.builder().config(config);
        assertThatThrownBy(builder::build)
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("certificate must not be null");
    }

    @Test
    void pingReturnsTrueOnlyWhenAllAreOk() {
        WsfeClient wsfe = mock(WsfeClient.class);
        RegistryClient reg = mock(RegistryClient.class);
        WsfexClient wsfex = mock(WsfexClient.class);
        WsmtxcaClient wsmtxca = mock(WsmtxcaClient.class);
        WscdcClient wscdc = mock(WscdcClient.class);
        ArcaClient client = new ArcaClient(wsfe, reg, wsfex, wsmtxca, wscdc);

        when(wsfe.ping()).thenReturn(true);
        when(reg.ping()).thenReturn(true);
        when(wsfex.ping()).thenReturn(true);
        when(wsmtxca.ping()).thenReturn(true);
        when(wscdc.ping()).thenReturn(true);
        assertThat(client.ping()).isTrue();

        when(wsfe.ping()).thenReturn(false);
        when(reg.ping()).thenReturn(true);
        when(wsfex.ping()).thenReturn(true);
        when(wsmtxca.ping()).thenReturn(true);
        when(wscdc.ping()).thenReturn(true);
        assertThat(client.ping()).isFalse();

        when(wsfe.ping()).thenReturn(true);
        when(reg.ping()).thenReturn(false);
        when(wsfex.ping()).thenReturn(true);
        when(wsmtxca.ping()).thenReturn(true);
        when(wscdc.ping()).thenReturn(true);
        assertThat(client.ping()).isFalse();

        when(wsfe.ping()).thenReturn(true);
        when(reg.ping()).thenReturn(true);
        when(wsfex.ping()).thenReturn(false);
        when(wsmtxca.ping()).thenReturn(true);
        when(wscdc.ping()).thenReturn(true);
        assertThat(client.ping()).isFalse();

        when(wsfe.ping()).thenReturn(true);
        when(reg.ping()).thenReturn(true);
        when(wsfex.ping()).thenReturn(true);
        when(wsmtxca.ping()).thenReturn(false);
        when(wscdc.ping()).thenReturn(true);
        assertThat(client.ping()).isFalse();

        when(wsfe.ping()).thenReturn(true);
        when(reg.ping()).thenReturn(true);
        when(wsfex.ping()).thenReturn(true);
        when(wsmtxca.ping()).thenReturn(true);
        when(wscdc.ping()).thenReturn(false);
        assertThat(client.ping()).isFalse();
    }
}
