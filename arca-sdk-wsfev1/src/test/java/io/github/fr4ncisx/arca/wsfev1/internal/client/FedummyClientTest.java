package io.github.fr4ncisx.arca.wsfev1.internal.client;

import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.DummyResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.ServiceSoap;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
class FedummyClientTest {

    private ServiceSoap portMock;
    private BindingProvider bindingProviderMock;
    private FedummyClient client;

    @BeforeEach
    void setUp() {
        portMock = mock(ServiceSoap.class, withSettings().extraInterfaces(BindingProvider.class));
        bindingProviderMock = (BindingProvider) portMock;
        when(bindingProviderMock.getRequestContext()).thenReturn(new HashMap<>());
        client = new FedummyClient(portMock);
    }

    @Test
    void rejectsNullPort() {
        assertThatThrownBy(() -> new FedummyClient(null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("port must not be null");
    }

    @Test
    void rejectsInvalidArguments() {
        assertThatThrownBy(() -> client.ping(null, Duration.ofSeconds(1)))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("env must not be null");

        assertThatThrownBy(() -> client.ping(ArcaEnvironment.HOMOLOGACION, null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("timeout must not be null");

        assertThatThrownBy(() -> client.ping(ArcaEnvironment.HOMOLOGACION, Duration.ofSeconds(-1)))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("timeout must not be null or negative");
    }

    @Test
    void returnsTrueWhenAllServersAreOk() {
        DummyResponse response = new DummyResponse();
        response.setAppServer("OK");
        response.setDbServer("OK");
        response.setAuthServer("OK");
        when(portMock.feDummy()).thenReturn(response);

        boolean result = client.ping(ArcaEnvironment.HOMOLOGACION, Duration.ofSeconds(5));

        assertThat(result).isTrue();
        verify(portMock).feDummy();
    }

    @Test
    void returnsFalseWhenAnyServerIsNotOk() {
        DummyResponse response = new DummyResponse();
        response.setAppServer("OK");
        response.setDbServer("FAIL");
        response.setAuthServer("OK");
        when(portMock.feDummy()).thenReturn(response);

        boolean result = client.ping(ArcaEnvironment.HOMOLOGACION, Duration.ofSeconds(5));

        assertThat(result).isFalse();
    }

    @Test
    void returnsFalseWhenExceptionIsThrown() {
        when(portMock.feDummy()).thenThrow(new WebServiceException("Connection refused"));

        boolean result = client.ping(ArcaEnvironment.HOMOLOGACION, Duration.ofSeconds(5));

        assertThat(result).isFalse();
    }
}
