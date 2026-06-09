package io.github.fr4ncisx.arca.soap.internal.adapter;

import java.lang.reflect.Proxy;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.xml.namespace.QName;

import com.sun.xml.ws.developer.JAXWSProperties;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.internal.config.SoapConfig;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.ws.Binding;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ArcaSoapClient}.
 * <p>
 * Verifies that the internal adapter applies Metro timeouts and translates SOAP
 * runtime failures without exposing concrete transport types through the public
 * port contract.
 *
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
class ArcaSoapClientTest {

    private static final TestRequest REQUEST = new TestRequest("LoginCms");
    private static final TestResponse RESPONSE = new TestResponse("accepted");
    private static final SoapConfig CONFIG = new SoapConfig(
            Duration.ofSeconds(2),
            Duration.ofSeconds(5));

    /**
     * Verifies that ArcaSoapClient implements the generic SOAP port contract.
     */
    @Test
    void implementsArcaSoapPort() {
        ArcaSoapClient<TestRequest, TestResponse> client = client(request -> RESPONSE);

        assertThat(client).isInstanceOf(ArcaSoapPort.class);
    }

    /**
     * Verifies that constructor dependencies are explicit and cannot be null.
     */
    @Test
    void rejectsNullConstructorArguments() {
        BindingProvider bindingProvider = bindingProvider(new HashMap<>());
        Function<TestRequest, TestResponse> operation = request -> RESPONSE;

        assertThatThrownBy(() -> new ArcaSoapClient<TestRequest, TestResponse>(
                null,
                operation,
                CONFIG))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("bindingProvider must not be null");

        assertThatThrownBy(() -> new ArcaSoapClient<TestRequest, TestResponse>(
                bindingProvider,
                null,
                CONFIG))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("operation must not be null");

        assertThatThrownBy(() -> new ArcaSoapClient<TestRequest, TestResponse>(
                bindingProvider,
                operation,
                null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("config must not be null");
    }

    /**
     * Verifies that a null request is rejected before the SOAP operation runs.
     */
    @Test
    void rejectsNullRequestBeforeInvokingOperation() {
        AtomicInteger invocations = new AtomicInteger();
        ArcaSoapClient<TestRequest, TestResponse> client = client(request -> {
            invocations.incrementAndGet();
            return RESPONSE;
        });

        assertThatThrownBy(() -> client.invoke(null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("request must not be null");
        assertThat(invocations).hasValue(0);
    }

    /**
     * Verifies that timeouts are applied to the JAX-WS request context before the operation runs.
     */
    @Test
    void appliesTimeoutsAndReturnsOperationResponse() {
        Map<String, Object> requestContext = new HashMap<>();
        AtomicReference<TestRequest> receivedRequest = new AtomicReference<>();
        ArcaSoapClient<TestRequest, TestResponse> client = new ArcaSoapClient<>(
                bindingProvider(requestContext),
                request -> {
                    receivedRequest.set(request);
                    return RESPONSE;
                },
                CONFIG);

        TestResponse response = client.invoke(REQUEST);

        assertThat(response).isSameAs(RESPONSE);
        assertThat(receivedRequest).hasValue(REQUEST);
        assertThat(requestContext)
                .containsEntry(JAXWSProperties.CONNECT_TIMEOUT, 2_000)
                .containsEntry(JAXWSProperties.REQUEST_TIMEOUT, 5_000);
    }

    /**
     * Verifies that the SOAP handler is installed while preserving existing handlers.
     */
    @Test
    void installsSoapHandlerAndPreservesExistingHandlers() {
        TestHandler existingHandler = new TestHandler();
        TestBinding binding = new TestBinding(List.of(existingHandler));

        new ArcaSoapClient<>(
                bindingProvider(new HashMap<>(), binding),
                request -> RESPONSE,
                CONFIG);

        assertThat(binding.getHandlerChain())
                .hasSize(2)
                .contains(existingHandler)
                .anyMatch(ArcaSoapHandler.class::isInstance);
    }

    /**
     * Verifies that reusing the same binding does not install duplicate SDK handlers.
     */
    @Test
    void doesNotInstallDuplicateSoapHandler() {
        TestBinding binding = new TestBinding(List.of());
        BindingProvider bindingProvider = bindingProvider(new HashMap<>(), binding);

        new ArcaSoapClient<>(bindingProvider, request -> RESPONSE, CONFIG);
        new ArcaSoapClient<>(bindingProvider, request -> RESPONSE, CONFIG);

        assertThat(binding.getHandlerChain())
                .filteredOn(ArcaSoapHandler.class::isInstance)
                .hasSize(1);
    }

    /**
     * Verifies that a SOAP fault is translated to the SDK SOAP exception.
     *
     * @throws SOAPException if the test SOAP fault cannot be created.
     */
    @Test
    void translatesSoapFaultToArcaSoapException() throws SOAPException {
        SOAPFaultException fault = soapFaultException();
        ArcaSoapClient<TestRequest, TestResponse> client = client(request -> {
            throw fault;
        });

        assertThatThrownBy(() -> client.invoke(REQUEST))
                .isInstanceOf(ArcaSoapException.class)
                .hasMessage("SOAP fault received")
                .satisfies(exception -> assertThat(exception.getCause()).isSameAs(fault));
    }

    /**
     * Verifies that a runtime timeout from JAX-WS is translated to the SDK SOAP exception.
     */
    @Test
    void translatesTimeoutToArcaSoapException() {
        WebServiceException timeout = new WebServiceException(
                "transport failed",
                new SocketTimeoutException("read timed out"));
        ArcaSoapClient<TestRequest, TestResponse> client = client(request -> {
            throw timeout;
        });

        assertThatThrownBy(() -> client.invoke(REQUEST))
                .isInstanceOf(ArcaSoapException.class)
                .hasMessage("SOAP invocation timed out")
                .satisfies(exception -> assertThat(exception.getCause()).isSameAs(timeout));
    }

    /**
     * Verifies that a non-timeout JAX-WS runtime failure is translated to the SDK SOAP exception.
     */
    @Test
    void translatesWebServiceFailureToArcaSoapException() {
        WebServiceException failure = new WebServiceException("connection reset");
        ArcaSoapClient<TestRequest, TestResponse> client = client(request -> {
            throw failure;
        });

        assertThatThrownBy(() -> client.invoke(REQUEST))
                .isInstanceOf(ArcaSoapException.class)
                .hasMessage("SOAP invocation failed")
                .satisfies(exception -> assertThat(exception.getCause()).isSameAs(failure));
    }

    /**
     * Verifies that an already translated SDK SOAP exception is not wrapped again.
     */
    @Test
    void preservesAlreadyTranslatedArcaSoapException() {
        ArcaSoapException translated = new ArcaSoapException("already translated");
        ArcaSoapClient<TestRequest, TestResponse> client = client(request -> {
            throw translated;
        });

        assertThatThrownBy(() -> client.invoke(REQUEST))
                .isSameAs(translated);
    }

    /**
     * Verifies that an unexpected runtime failure is translated to the SDK SOAP exception.
     */
    @Test
    void translatesRuntimeFailureToArcaSoapException() {
        IllegalStateException failure = new IllegalStateException("broken proxy");
        ArcaSoapClient<TestRequest, TestResponse> client = client(request -> {
            throw failure;
        });

        assertThatThrownBy(() -> client.invoke(REQUEST))
                .isInstanceOf(ArcaSoapException.class)
                .hasMessage("SOAP invocation failed")
                .satisfies(exception -> assertThat(exception.getCause()).isSameAs(failure));
    }

    private static ArcaSoapClient<TestRequest, TestResponse> client(
            Function<TestRequest, TestResponse> operation) {
        return new ArcaSoapClient<>(bindingProvider(new HashMap<>()), operation, CONFIG);
    }

    private static BindingProvider bindingProvider(Map<String, Object> requestContext) {
        return bindingProvider(requestContext, new TestBinding(List.of()));
    }

    private static BindingProvider bindingProvider(Map<String, Object> requestContext, Binding binding) {
        return (BindingProvider) Proxy.newProxyInstance(
                BindingProvider.class.getClassLoader(),
                new Class<?>[] { BindingProvider.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getRequestContext" -> requestContext;
                    case "getResponseContext" -> new HashMap<String, Object>();
                    case "getBinding" -> binding;
                    case "getEndpointReference" -> null;
                    case "toString" -> "FakeBindingProvider";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static SOAPFaultException soapFaultException() throws SOAPException {
        var fault = SOAPFactory.newInstance().createFault(
                "remote rejected",
                new QName("urn:test", "Client"));
        return new SOAPFaultException(fault);
    }

    private record TestRequest(String operation) {
    }

    private record TestResponse(String result) {
    }

    private static final class TestBinding implements Binding {

        private List<Handler> handlerChain;

        private TestBinding(List<Handler> handlerChain) {
            this.handlerChain = new ArrayList<>(handlerChain);
        }

        /**
         * Returns the current handler chain.
         *
         * @return configured handler chain.
         */
        @Override
        public List<Handler> getHandlerChain() {
            return handlerChain;
        }

        /**
         * Replaces the current handler chain.
         *
         * @param chain replacement handler chain.
         */
        @Override
        public void setHandlerChain(List<Handler> chain) {
            handlerChain = new ArrayList<>(chain);
        }

        /**
         * Returns the synthetic binding identifier used by tests.
         *
         * @return test binding identifier.
         */
        @Override
        public String getBindingID() {
            return "test-binding";
        }
    }

    private static final class TestHandler implements Handler<SOAPMessageContext> {

        /**
         * Keeps normal message processing active.
         *
         * @param context SOAP message context.
         * @return true so processing continues.
         */
        @Override
        public boolean handleMessage(SOAPMessageContext context) {
            return true;
        }

        /**
         * Keeps fault processing active.
         *
         * @param context SOAP fault context.
         * @return true so processing continues.
         */
        @Override
        public boolean handleFault(SOAPMessageContext context) {
            return true;
        }

        /**
         * Closes the test handler without retaining resources.
         *
         * @param context message context.
         */
        @Override
        public void close(MessageContext context) {
        }
    }
}
