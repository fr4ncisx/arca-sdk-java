package io.github.fr4ncisx.arca.soap.internal.adapter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for {@link ArcaSoapHandler}.
 * <p>
 * Verifies sanitized diagnostics, technical HTTP headers, and non-interference
 * with the SOAP message envelope.
 *
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
class ArcaSoapHandlerTest {

    private static final String TOKEN = "secret-token-value";
    private static final String SIGN = "secret-sign-value";
    private static final String PASSWORD = "secret-password-value";

    /**
     * Verifies that outbound SOAP messages are sanitized before diagnostics and receive technical headers.
     *
     * @throws Exception if the SOAP message cannot be created.
     */
    @Test
    void sanitizesOutboundMessageAndAddsOnlyTechnicalHeaders() throws Exception {
        List<String> diagnostics = new ArrayList<>();
        var handler = new ArcaSoapHandler(diagnostics::add);
        var context = new TestSoapMessageContext(sensitiveMessage());
        context.put(MessageContext.MESSAGE_OUTBOUND_PROPERTY, Boolean.TRUE);

        boolean shouldContinue = handler.handleMessage(context);

        assertThat(shouldContinue).isTrue();
        assertThat(diagnostics).hasSize(1);
        assertThat(diagnostics.getFirst())
                .doesNotContain(TOKEN)
                .doesNotContain(SIGN)
                .doesNotContain(PASSWORD)
                .contains("[REDACTED]");
        assertThat(httpHeaders(context))
                .containsOnlyKeys(ArcaSoapHandler.HEADER_SOURCE, ArcaSoapHandler.HEADER_VERSION)
                .containsEntry(ArcaSoapHandler.HEADER_SOURCE, List.of(ArcaSoapHandler.SOURCE_VALUE))
                .containsEntry(ArcaSoapHandler.HEADER_VERSION, List.of(ArcaSoapHandler.DEVELOPMENT_VERSION));
    }

    /**
     * Verifies that SOAP faults are sanitized before diagnostics are emitted.
     *
     * @throws Exception if the SOAP fault cannot be created.
     */
    @Test
    void sanitizesFaultMessage() throws Exception {
        List<String> diagnostics = new ArrayList<>();
        var handler = new ArcaSoapHandler(diagnostics::add);
        var context = new TestSoapMessageContext(faultMessage());

        boolean shouldContinue = handler.handleFault(context);

        assertThat(shouldContinue).isTrue();
        assertThat(diagnostics).hasSize(1);
        assertThat(diagnostics.getFirst())
                .doesNotContain("Bearer sensitive-token")
                .contains("Authorization: [REDACTED]");
    }

    /**
     * Verifies that handler diagnostics do not mutate the SOAP message content.
     *
     * @throws Exception if the SOAP message cannot be serialized.
     */
    @Test
    void doesNotMutateSoapMessage() throws Exception {
        var message = sensitiveMessage();
        String before = serialize(message);
        var handler = new ArcaSoapHandler(ignored -> {
        });
        var context = new TestSoapMessageContext(message);
        context.put(MessageContext.MESSAGE_OUTBOUND_PROPERTY, Boolean.TRUE);

        handler.handleMessage(context);

        assertThat(serialize(message)).isEqualTo(before);
    }

    /**
     * Verifies that the handler does not claim SOAP envelope headers.
     */
    @Test
    void getHeadersReturnsEmptySet() {
        var handler = new ArcaSoapHandler(ignored -> {
        });

        assertThat(handler.getHeaders()).isEmpty();
    }

    /**
     * Verifies that close is a no-op and does not interrupt processing.
     */
    @Test
    void closeDoesNotThrow() {
        var handler = new ArcaSoapHandler(ignored -> {
        });

        assertThatCode(() -> handler.close(new TestSoapMessageContext(null))).doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> httpHeaders(MessageContext context) {
        return (Map<String, List<String>>) context.get(MessageContext.HTTP_REQUEST_HEADERS);
    }

    private static SOAPMessage sensitiveMessage() throws SOAPException {
        SOAPMessage message = MessageFactory.newInstance().createMessage();
        var envelope = message.getSOAPPart().getEnvelope();
        var body = envelope.getBody();
        var operation = body.addChildElement("loginCms", "wsaa", "urn:test");
        operation.addChildElement("token").addTextNode(TOKEN);
        operation.addChildElement("sign").addTextNode(SIGN);
        operation.addChildElement("password").addTextNode(PASSWORD);
        message.saveChanges();
        return message;
    }

    private static SOAPMessage faultMessage() throws SOAPException {
        SOAPMessage message = MessageFactory.newInstance().createMessage();
        message.getSOAPBody().addFault(
                new QName("urn:test", "Client"),
                "Authorization: Bearer sensitive-token");
        message.saveChanges();
        return message;
    }

    private static String serialize(SOAPMessage message) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeTo(output);
        return output.toString(StandardCharsets.UTF_8);
    }

    private static final class TestSoapMessageContext
            extends HashMap<String, Object>
            implements SOAPMessageContext {

        private SOAPMessage message;
        private final Map<String, Scope> scopes = new HashMap<>();

        private TestSoapMessageContext(SOAPMessage message) {
            this.message = message;
        }

        /**
         * Returns the SOAP message associated with this test context.
         *
         * @return SOAP message used by the handler.
         */
        @Override
        public SOAPMessage getMessage() {
            return message;
        }

        /**
         * Replaces the SOAP message associated with this test context.
         *
         * @param message replacement SOAP message.
         */
        @Override
        public void setMessage(SOAPMessage message) {
            this.message = message;
        }

        /**
         * Returns no decoded SOAP headers because these tests inspect raw messages.
         *
         * @param header requested header name.
         * @param context JAXB context used by JAX-WS.
         * @param allRoles whether all roles should be considered.
         * @return empty header array.
         */
        @Override
        public Object[] getHeaders(QName header, JAXBContext context, boolean allRoles) {
            return new Object[0];
        }

        /**
         * Returns no SOAP roles for this test context.
         *
         * @return empty role set.
         */
        @Override
        public Set<String> getRoles() {
            return Set.of();
        }

        /**
         * Stores the scope for a context property.
         *
         * @param name property name.
         * @param scope property scope.
         */
        @Override
        public void setScope(String name, Scope scope) {
            scopes.put(name, scope);
        }

        /**
         * Returns the scope associated with a context property.
         *
         * @param name property name.
         * @return stored scope or application scope when absent.
         */
        @Override
        public Scope getScope(String name) {
            return scopes.getOrDefault(name, Scope.APPLICATION);
        }
    }
}
