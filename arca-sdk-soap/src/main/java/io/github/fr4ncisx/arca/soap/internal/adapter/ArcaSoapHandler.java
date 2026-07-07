package io.github.fr4ncisx.arca.soap.internal.adapter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.logging.LogSanitizer;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal SOAP handler for SDK technical headers and sanitized diagnostics.
 * <p>
 * The handler adds ARCA SDK traceability headers to outbound HTTP transport
 * metadata and sanitizes serialized SOAP messages before any diagnostic output.
 * It never mutates the SOAP envelope body, headers, or faults.
 *
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
public final class ArcaSoapHandler implements SOAPHandler<SOAPMessageContext> {

    static final String HEADER_SOURCE = "X-Arca-Source";
    static final String HEADER_VERSION = "X-Arca-Version";
    static final String SOURCE_VALUE = "arca-sdk-java";
    static final String DEVELOPMENT_VERSION = "development";

    private static final Logger LOGGER = LoggerFactory.getLogger(ArcaSoapHandler.class);
    private static final String NULL_DIAGNOSTIC_SINK = "diagnosticSink must not be null";

    private final Consumer<String> diagnosticSink;

    /**
     * Creates a handler that writes sanitized SOAP diagnostics to the SDK logger
     * when debug logging is enabled.
     */
    public ArcaSoapHandler() {
        this(message -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("SOAP diagnostic: {}", message);
            }
        });
    }

    /**
     * Creates a handler with an explicit diagnostic sink for tests.
     *
     * @param diagnosticSink sink that receives sanitized SOAP diagnostics.
     * @throws ArcaValidationException if diagnosticSink is null.
     */
    ArcaSoapHandler(Consumer<String> diagnosticSink) {
        if (diagnosticSink == null) {
            throw new ArcaValidationException(NULL_DIAGNOSTIC_SINK);
        }
        this.diagnosticSink = diagnosticSink;
    }

    /**
     * Processes a normal SOAP message and keeps the SOAP flow active.
     *
     * @param context SOAP message context supplied by JAX-WS.
     * @return true so processing continues.
     */
    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        addTechnicalHeadersIfOutbound(context);
        writeSanitizedDiagnostic(context);
        return true;
    }

    /**
     * Processes a SOAP fault and keeps the SOAP flow active.
     *
     * @param context SOAP fault context supplied by JAX-WS.
     * @return true so processing continues.
     */
    @Override
    public boolean handleFault(SOAPMessageContext context) {
        writeSanitizedDiagnostic(context);
        return true;
    }

    /**
     * Closes the handler without retaining any per-message resources.
     *
     * @param context message context supplied by JAX-WS.
     */
    @Override
    public void close(MessageContext context) {
    }

    /**
     * Returns SOAP envelope headers processed by this handler.
     * <p>
     * This handler adds HTTP transport headers through {@link MessageContext}
     * rather than SOAP envelope headers.
     *
     * @return an empty header set.
     */
    @Override
    public Set<QName> getHeaders() {
        return Set.of();
    }

    @SuppressWarnings("unchecked")
    private static void addTechnicalHeadersIfOutbound(SOAPMessageContext context) {
        if (context == null || !Boolean.TRUE.equals(context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))) {
            return;
        }

        Object current = context.get(MessageContext.HTTP_REQUEST_HEADERS);
        Map<String, List<String>> headers = current instanceof Map<?, ?>
                ? new LinkedHashMap<>((Map<String, List<String>>) current)
                : new LinkedHashMap<>();
        headers.put(HEADER_SOURCE, List.of(SOURCE_VALUE));
        headers.put(HEADER_VERSION, List.of(resolveVersion()));
        context.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
    }

    private void writeSanitizedDiagnostic(SOAPMessageContext context) {
        String diagnostic = serializeMessage(context);
        if (diagnostic != null) {
            diagnosticSink.accept(LogSanitizer.sanitize(diagnostic));
        }
    }

    private static String serializeMessage(SOAPMessageContext context) {
        if (context == null || context.getMessage() == null) {
            return null;
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            context.getMessage().writeTo(output);
            return output.toString(StandardCharsets.UTF_8);
        } catch (SOAPException | IOException exception) {
            return exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }
    }

    private static String resolveVersion() {
        Package handlerPackage = ArcaSoapHandler.class.getPackage();
        String version = handlerPackage == null ? null : handlerPackage.getImplementationVersion();
        return version == null || version.isBlank() ? DEVELOPMENT_VERSION : version;
    }
}
