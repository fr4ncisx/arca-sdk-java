package io.github.fr4ncisx.arca.wscdc.internal.client;

import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wscdc.internal.generated.DummyResponse;
import io.github.fr4ncisx.arca.wscdc.internal.generated.ServiceSoap;
import io.github.fr4ncisx.arca.wscdc.model.WscdcDummyResponse;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Unit tests for WscdcDummyClient connectivity checks against ARCA's ComprobanteDummy endpoint.
 *
 * @author fr4ncisx
 * @since 0.9.0
 */
@SuppressWarnings("null")
class WscdcDummyClientTest {

    private ServiceSoap portMock;
    private BindingProvider bindingProviderMock;

    @BeforeEach
    void setUp() {
        portMock = mock(ServiceSoap.class, withSettings().extraInterfaces(BindingProvider.class));
        bindingProviderMock = (BindingProvider) portMock;
        when(bindingProviderMock.getRequestContext()).thenReturn(new HashMap<>());
    }

    /**
     * Verifies that the constructor rejects a null JAX-WS port.
     */
    @Test
    void rejectsNullPort() {
        assertThatThrownBy(() -> new WscdcDummyClient(null, ArcaEnvironment.HOMOLOGACION, Duration.ofSeconds(5)))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("port must not be null");
    }

    /**
     * Verifies that the constructor rejects a null ARCA environment.
     */
    @Test
    void rejectsNullEnv() {
        assertThatThrownBy(() -> new WscdcDummyClient(portMock, null, Duration.ofSeconds(5)))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("env must not be null");
    }

    /**
     * Verifies that the constructor rejects a null timeout.
     */
    @Test
    void rejectsNullTimeout() {
        assertThatThrownBy(() -> new WscdcDummyClient(portMock, ArcaEnvironment.HOMOLOGACION, null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("timeout must not be null");
    }

    /**
     * Verifies that the constructor rejects a negative timeout duration.
     */
    @Test
    void rejectsNegativeTimeout() {
        assertThatThrownBy(() -> new WscdcDummyClient(portMock, ArcaEnvironment.HOMOLOGACION, Duration.ofSeconds(-1)))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("timeout must not be null or negative");
    }

    /**
     * Verifies that a successful ping returns an OK response with all server
     * statuses marked as OK.
     */
    @Test
    void returnsOkWhenAllServersAreOk() {
        DummyResponse response = new DummyResponse();
        response.setAppServer("OK");
        response.setDbServer("OK");
        response.setAuthServer("OK");
        when(portMock.comprobanteDummy()).thenReturn(response);

        WscdcDummyClient client = new WscdcDummyClient(portMock, ArcaEnvironment.HOMOLOGACION, Duration.ofSeconds(5));
        WscdcDummyResponse result = client.ping();

        assertThat(result).isNotNull();
        assertThat(result.isOk()).isTrue();
        assertThat(result.appServer()).isEqualTo("OK");
        assertThat(result.dbServer()).isEqualTo("OK");
        assertThat(result.authServer()).isEqualTo("OK");
        verify(portMock).comprobanteDummy();
    }

    /**
     * Verifies that a ping returns a non-OK response when any server status
     * is not OK.
     */
    @Test
    void returnsErrorWhenAnyServerIsNotOk() {
        DummyResponse response = new DummyResponse();
        response.setAppServer("OK");
        response.setDbServer("FAIL");
        response.setAuthServer("OK");
        when(portMock.comprobanteDummy()).thenReturn(response);

        WscdcDummyClient client = new WscdcDummyClient(portMock, ArcaEnvironment.HOMOLOGACION, Duration.ofSeconds(5));
        WscdcDummyResponse result = client.ping();

        assertThat(result).isNotNull();
        assertThat(result.isOk()).isFalse();
        assertThat(result.dbServer()).isEqualTo("FAIL");
    }

    /**
     * Verifies that a ping returns an ERROR response when the underlying port
     * throws a WebServiceException.
     */
    @Test
    void returnsErrorWhenExceptionIsThrown() {
        when(portMock.comprobanteDummy()).thenThrow(new WebServiceException("Connection refused"));

        WscdcDummyClient client = new WscdcDummyClient(portMock, ArcaEnvironment.HOMOLOGACION, Duration.ofSeconds(5));
        WscdcDummyResponse result = client.ping();

        assertThat(result).isNotNull();
        assertThat(result.isOk()).isFalse();
        assertThat(result.appServer()).isEqualTo("ERROR");
        assertThat(result.dbServer()).isEqualTo("ERROR");
        assertThat(result.authServer()).isEqualTo("ERROR");
    }
}
