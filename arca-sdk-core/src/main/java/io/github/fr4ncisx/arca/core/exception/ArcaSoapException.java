package io.github.fr4ncisx.arca.core.exception;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * SOAP communication failure.
 * <p>
 * Thrown when a SOAP call fails due to timeouts, network errors,
 * or SOAPFault responses from the ARCA service.
 *
 * @author fr4ncisx
 * @since 0.1.0-M1
 */
public non-sealed class ArcaSoapException extends ArcaException {

    /**
     * Creates a new exception with the default SOAPFAULT error code.
     *
     * @param message the detail message.
     */
    public ArcaSoapException(String message) {
        super(ArcaErrorCode.SOAPFAULT, message);
    }

    /**
     * Creates a new exception with the default SOAPFAULT error code and cause.
     *
     * @param message the detail message.
     * @param cause the underlying cause.
     */
    public ArcaSoapException(String message, Throwable cause) {
        super(ArcaErrorCode.SOAPFAULT, message, cause);
    }

    /**
     * Creates a new exception with the default SOAPFAULT error code and metadata.
     *
     * @param message the detail message.
     * @param metadata the contextual metadata.
     * @since 0.1.0-M4
     */
    public ArcaSoapException(String message, @Nullable Map<String, String> metadata) {
        super(ArcaErrorCode.SOAPFAULT, message, metadata);
    }

    /**
     * Creates a new exception with the default SOAPFAULT error code, metadata, and cause.
     *
     * @param message the detail message.
     * @param metadata the contextual metadata.
     * @param cause the underlying cause.
     * @since 0.1.0-M4
     */
    public ArcaSoapException(String message, @Nullable Map<String, String> metadata, Throwable cause) {
        super(ArcaErrorCode.SOAPFAULT, message, metadata, cause);
    }

    /**
     * Creates a new exception with an explicit error code and detail message.
     *
     * @param errorCode the structured error code.
     * @param message the detail message.
     * @since 0.1.0-M4
     */
    public ArcaSoapException(ArcaErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Creates a new exception with an explicit error code, detail message, and cause.
     *
     * @param errorCode the structured error code.
     * @param message the detail message.
     * @param cause the underlying cause.
     * @since 0.1.0-M4
     */
    public ArcaSoapException(ArcaErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * Creates a new exception with an explicit error code, detail message, and metadata.
     *
     * @param errorCode the structured error code.
     * @param message the detail message.
     * @param metadata the contextual metadata.
     * @since 0.1.0-M4
     */
    public ArcaSoapException(ArcaErrorCode errorCode, String message, @Nullable Map<String, String> metadata) {
        super(errorCode, message, metadata);
    }

    /**
     * Creates a new exception with an explicit error code, detail message, metadata, and cause.
     *
     * @param errorCode the structured error code.
     * @param message the detail message.
     * @param metadata the contextual metadata.
     * @param cause the underlying cause.
     * @since 0.1.0-M4
     */
    public ArcaSoapException(ArcaErrorCode errorCode, String message, @Nullable Map<String, String> metadata, Throwable cause) {
        super(errorCode, message, metadata, cause);
    }
}
