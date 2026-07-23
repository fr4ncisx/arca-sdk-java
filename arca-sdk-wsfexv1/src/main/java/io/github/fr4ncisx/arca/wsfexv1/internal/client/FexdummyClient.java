package io.github.fr4ncisx.arca.wsfexv1.internal.client;

import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.DummyResponse;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ServiceSoap;
import jakarta.xml.ws.BindingProvider;
import com.sun.xml.ws.developer.JAXWSProperties;

import java.time.Duration;

/**
 * Technical client for executing connectivity checks against ARCA's WSFEXv1 FEXDummy endpoint.
 * <p>
 * This client runs independently of WSAA authorization and does not require CMS signatures.
 *
 * @author fr4ncisx
 * @since 1.0.0
 */
public final class FexdummyClient {

    private final ServiceSoap port;

    /**
     * Creates a new FexdummyClient using the provided JAX-WS ServiceSoap port.
     *
     * @param port the JAX-WS client port
     * @throws ArcaValidationException if port is null
     */
    public FexdummyClient(ServiceSoap port) {
        if (port == null) {
            throw new ArcaValidationException("port must not be null");
        }
        this.port = port;
    }

    /**
     * Executes a ping call to ARCA's WSFEXv1 FEXDummy service.
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
            context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, env.getWsfexv1Url().toString());

            long timeoutMillis = timeout.toMillis();
            int intTimeout = (int) Math.min(timeoutMillis, Integer.MAX_VALUE);
            context.put(JAXWSProperties.CONNECT_TIMEOUT, intTimeout);
            context.put(JAXWSProperties.REQUEST_TIMEOUT, intTimeout);

            DummyResponse response = port.fexDummy();
            return response != null
                    && "OK".equalsIgnoreCase(response.getAppServer())
                    && "OK".equalsIgnoreCase(response.getDbServer())
                    && "OK".equalsIgnoreCase(response.getAuthServer());
        } catch (Throwable t) {
            return false;
        }
    }
}
