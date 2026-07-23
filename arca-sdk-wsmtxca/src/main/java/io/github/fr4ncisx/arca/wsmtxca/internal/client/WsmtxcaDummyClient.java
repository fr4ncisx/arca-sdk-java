package io.github.fr4ncisx.arca.wsmtxca.internal.client;

import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.DummyResponseType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.MTXCAServicePortType;
import jakarta.xml.ws.BindingProvider;
import com.sun.xml.ws.developer.JAXWSProperties;

import java.time.Duration;

/**
 * Technical client for executing connectivity checks against ARCA's WSMTXCA Dummy endpoint.
 * <p>
 * This client runs independently of WSAA authorization and does not require CMS signatures.
 *
 * @author fr4ncisx
 * @since 1.0.0
 */
public final class WsmtxcaDummyClient {

    private final MTXCAServicePortType port;

    /**
     * Creates a new WsmtxcaDummyClient using the provided MTXCAServicePortType port.
     *
     * @param port the JAX-WS client port
     * @throws ArcaValidationException if port is null
     */
    public WsmtxcaDummyClient(MTXCAServicePortType port) {
        if (port == null) {
            throw new ArcaValidationException("port must not be null");
        }
        this.port = port;
    }

    /**
     * Executes a ping call to ARCA's WSMTXCA Dummy service.
     *
     * @param env     the target ARCA environment
     * @param timeout the maximum time allowed for the network call to complete
     * @return {@code true} if all server components respond with {@code OK}
     * @throws ArcaValidationException if env is null, or timeout is null or negative
     */
    @SuppressWarnings("null")
    public boolean ping(ArcaEnvironment env, Duration timeout) {
        if (env == null) {
            throw new ArcaValidationException("env must not be null");
        }
        if (timeout == null || timeout.isNegative()) {
            throw new ArcaValidationException("timeout must not be null or negative");
        }

        try {
            BindingProvider bp = (BindingProvider) port;
            var context = bp.getRequestContext();
            context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, env.getWsmtxcaUrl().toString());

            long timeoutMillis = timeout.toMillis();
            int intTimeout = (int) Math.min(timeoutMillis, Integer.MAX_VALUE);
            context.put(JAXWSProperties.CONNECT_TIMEOUT, intTimeout);
            context.put(JAXWSProperties.REQUEST_TIMEOUT, intTimeout);

            DummyResponseType response = port.dummy();
            return response != null
                    && "OK".equalsIgnoreCase(response.getAppserver())
                    && "OK".equalsIgnoreCase(response.getDbserver())
                    && "OK".equalsIgnoreCase(response.getAuthserver());
        } catch (Throwable t) {
            return false;
        }
    }
}
