package io.github.fr4ncisx.arca.wsmtxca.internal.client;

import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.DummyResponseType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.MTXCAServicePortType;
import jakarta.xml.ws.BindingProvider;
import com.sun.xml.ws.developer.JAXWSProperties;

import java.time.Duration;

public final class WsmtxcaDummyClient {

    private final MTXCAServicePortType port;

    public WsmtxcaDummyClient(MTXCAServicePortType port) {
        if (port == null) {
            throw new ArcaValidationException("port must not be null");
        }
        this.port = port;
    }

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
