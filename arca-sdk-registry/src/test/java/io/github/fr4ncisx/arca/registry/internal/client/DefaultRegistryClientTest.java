package io.github.fr4ncisx.arca.registry.internal.client;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.registry.internal.generated.*;
import io.github.fr4ncisx.arca.registry.internal.usecase.GetTaxpayerUseCase;
import io.github.fr4ncisx.arca.registry.model.TaxpayerData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link DefaultRegistryClient}.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
class DefaultRegistryClientTest {

    private ArcaConfig config;
    private GetTaxpayerUseCase getTaxpayerUseCase;
    private PersonaServiceA4 port;
    private DefaultRegistryClient client;

    @BeforeEach
    void setUp() {
        config = new ArcaConfig(
                Cuit.parse("20-33333333-4"),
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(2),
                Duration.ofSeconds(2)
        );
        getTaxpayerUseCase = mock(GetTaxpayerUseCase.class);
        port = mock(PersonaServiceA4.class, withSettings().extraInterfaces(jakarta.xml.ws.BindingProvider.class));
        client = new DefaultRegistryClient(config, getTaxpayerUseCase, port);
    }

    @Test
    void getTaxpayerDelegatesToUseCase() {
        TaxpayerData expected = new TaxpayerData(
                123L, "a", "b", "c", "d", "e", "f",
                null, null, null, null, null, null, null, null,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );
        Cuit target = Cuit.parse("20-44444444-5");
        when(getTaxpayerUseCase.execute(target)).thenReturn(expected);

        TaxpayerData actual = client.getTaxpayer(target);

        assertThat(actual).isEqualTo(expected);
        verify(getTaxpayerUseCase).execute(target);
    }

    @Test
    void pingReturnsTrueOnOkServers() {
        DummyReturn value = new DummyReturn();
        value.setAppserver("OK");
        value.setDbserver("OK");
        value.setAuthserver("OK");
        when(port.dummy()).thenReturn(value);

        boolean result = client.ping();

        assertThat(result).isTrue();
    }

    @Test
    void pingReturnsFalseOnFailure() {
        when(port.dummy()).thenThrow(new RuntimeException("Network down"));

        boolean result = client.ping();

        assertThat(result).isFalse();
    }
}
