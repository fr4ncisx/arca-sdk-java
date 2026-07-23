package io.github.fr4ncisx.arca.wscdc.internal.client;

import com.sun.xml.ws.developer.JAXWSProperties;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wscdc.internal.adapter.WscdcMapper;
import io.github.fr4ncisx.arca.wscdc.internal.generated.DummyResponse;
import io.github.fr4ncisx.arca.wscdc.internal.generated.ServiceSoap;
import io.github.fr4ncisx.arca.wscdc.model.WscdcDummyResponse;
import jakarta.xml.ws.BindingProvider;

import java.time.Duration;

/**
 * Technical client for executing connectivity checks against ARCA's WSCDC ComprobanteDummy endpoint.
 * <p>
 * This client runs independently of WSAA authorization and does not require CMS signatures.
 *
 * @author fr4ncisx
 * @since 0.9.0
 */
public final class WscdcDummyClient {

    private final ServiceSoap port;
    private final ArcaEnvironment env;
    private final Duration timeout;

    /**
     * Creates a new WscdcDummyClient using the provided JAX-WS ServiceSoap port.
     *
     * @param port    the JAX-WS client port
     * @param env     the target ARCA environment
     * @param timeout the maximum time allowed for the network call to complete
     * @throws ArcaValidationException if any parameter is null or timeout is negative
     */
    public WscdcDummyClient(ServiceSoap port, ArcaEnvironment env, Duration timeout) {
        if (port == null) {
            throw new ArcaValidationException("port must not be null");
        }
        if (env == null) {
            throw new ArcaValidationException("env must not be null");
        }
        if (timeout == null || timeout.isNegative()) {
            throw new ArcaValidationException("timeout must not be null or negative");
        }
        this.port = port;
        this.env = env;
        this.timeout = timeout;
    }

    /**
     * Executes a ping call to ARCA's WSCDC ComprobanteDummy service.
     *
     * @return the mapped dummy response with server status details
     */
    @SuppressWarnings("null")
    public WscdcDummyResponse ping() {
        try {
            BindingProvider bp = (BindingProvider) port;
            var context = bp.getRequestContext();
            context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, env.getWscdcUrl().toString());

            long timeoutMillis = timeout.toMillis();
            int intTimeout = (int) Math.min(timeoutMillis, Integer.MAX_VALUE);
            context.put(JAXWSProperties.CONNECT_TIMEOUT, intTimeout);
            context.put(JAXWSProperties.REQUEST_TIMEOUT, intTimeout);

            DummyResponse response = port.comprobanteDummy();
            return WscdcMapper.mapDummy(response);
        } catch (Exception e) {
            return new WscdcDummyResponse("ERROR", "ERROR", "ERROR");
        }
    }
}
