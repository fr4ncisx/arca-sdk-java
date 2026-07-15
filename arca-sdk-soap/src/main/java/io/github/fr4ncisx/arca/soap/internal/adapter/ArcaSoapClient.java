package io.github.fr4ncisx.arca.soap.internal.adapter;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import com.sun.xml.ws.developer.JAXWSProperties;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.internal.config.SoapConfig;
import io.github.fr4ncisx.arca.soap.internal.resilience.ResilienceDecorator;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import jakarta.xml.ws.Binding;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.soap.SOAPFaultException;

/**
 * Internal SOAP adapter backed by a concrete JAX-WS binding provider.
 * <p>
 * The client keeps Metro and JAX-WS details inside the SOAP infrastructure
 * module while exposing the generic {@link ArcaSoapPort} contract to callers.
 * The request and response types are technical SOAP types prepared by higher
 * internal layers.
 *
 * @param <R> technical request type accepted by the remote SOAP operation.
 * @param <S> technical response type returned by the remote SOAP operation.
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
public final class ArcaSoapClient<R, S> implements ArcaSoapPort<R, S> {

    private static final String NULL_BINDING_PROVIDER = "bindingProvider must not be null";
    private static final String NULL_OPERATION = "operation must not be null";
    private static final String NULL_CONFIG = "config must not be null";
    private static final String NULL_REQUEST = "request must not be null";
    private static final String SOAP_FAULT_MESSAGE = "SOAP fault received";
    private static final String SOAP_TIMEOUT_MESSAGE = "SOAP invocation timed out";
    private static final String SOAP_FAILURE_MESSAGE = "SOAP invocation failed";

    private final BindingProvider bindingProvider;
    private final Function<R, S> operation;
    private final SoapConfig config;
    private final ArcaSoapPort<R, S> runner;

    /**
     * Creates an internal SOAP client for a concrete generated JAX-WS port.
     *
     * @param bindingProvider generated JAX-WS port used to configure transport context.
     * @param operation remote operation to execute with the technical request.
     * @param config resolved SOAP transport configuration.
     * @throws ArcaValidationException if any argument is null.
     */
    public ArcaSoapClient(BindingProvider bindingProvider,
                          Function<R, S> operation,
                          SoapConfig config) {
        if (bindingProvider == null) {
            throw new ArcaValidationException(NULL_BINDING_PROVIDER);
        }
        if (operation == null) {
            throw new ArcaValidationException(NULL_OPERATION);
        }
        if (config == null) {
            throw new ArcaValidationException(NULL_CONFIG);
        }
        this.bindingProvider = bindingProvider;
        this.operation = operation;
        this.config = config;
        installSoapHandler(bindingProvider);

        ArcaSoapPort<R, S> rawPort = req -> {
            applyTimeouts();
            try {
                return operation.apply(req);
            } catch (SOAPFaultException exception) {
                throw new ArcaSoapException(SOAP_FAULT_MESSAGE, exception);
            } catch (ArcaSoapException exception) {
                throw exception;
            } catch (WebServiceException exception) {
                if (isTimeout(exception)) {
                    throw new ArcaSoapException(SOAP_TIMEOUT_MESSAGE, exception);
                }
                throw new ArcaSoapException(SOAP_FAILURE_MESSAGE, exception);
            } catch (RuntimeException exception) {
                throw new ArcaSoapException(SOAP_FAILURE_MESSAGE, exception);
            }
        };

        if (config.resilienceEnabled()) {
            this.runner = new ResilienceDecorator<>(rawPort);
        } else {
            this.runner = rawPort;
        }
    }

    /**
     * Invokes the configured SOAP operation with the provided technical request.
     * <p>
     * The method applies the configured Metro timeouts before each invocation and
     * translates SOAP faults, timeouts, and runtime transport failures to
     * {@link ArcaSoapException}.
     *
     * @param request technical request payload already prepared by the caller.
     * @return technical response payload returned by the SOAP runtime.
     * @throws ArcaValidationException if request is null.
     * @throws ArcaSoapException if the remote invocation fails.
     */
    @Override
    public S invoke(R request) throws ArcaSoapException {
        if (request == null) {
            throw new ArcaValidationException(NULL_REQUEST);
        }
        return runner.invoke(request);
    }

    private void applyTimeouts() {
        Map<String, Object> requestContext = bindingProvider.getRequestContext();
        requestContext.put(JAXWSProperties.CONNECT_TIMEOUT, config.connectTimeoutMillis());
        requestContext.put(JAXWSProperties.REQUEST_TIMEOUT, config.readTimeoutMillis());
    }

    private static void installSoapHandler(BindingProvider bindingProvider) {
        Binding binding = bindingProvider.getBinding();
        if (binding == null) {
            return;
        }

        List<Handler> handlers = new ArrayList<>(binding.getHandlerChain());
        boolean alreadyInstalled = handlers.stream().anyMatch(ArcaSoapHandler.class::isInstance);
        if (!alreadyInstalled) {
            handlers.add(new ArcaSoapHandler());
            binding.setHandlerChain(handlers);
        }
    }

    private static boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
