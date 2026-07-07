package io.github.fr4ncisx.arca.core.exception;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Input validation error.
 * <p>
 * Thrown when a method argument or configuration value fails validation
 * (null, out of range, wrong format).
 *
 * @author fr4ncisx
 * @since 0.1.0-M1
 */
public non-sealed class ArcaValidationException extends ArcaException {

    /**
     * Creates a new exception with the given detail message.
     *
     * @param message the detail message.
     */
    public ArcaValidationException(String message) {
        super(ArcaErrorCode.VALIDATIONERROR, message);
    }

    /**
     * Creates a new exception with the given detail message and cause.
     *
     * @param message the detail message.
     * @param cause the underlying cause.
     */
    public ArcaValidationException(String message, Throwable cause) {
        super(ArcaErrorCode.VALIDATIONERROR, message, cause);
    }

    /**
     * Creates a new exception with the given detail message and metadata.
     *
     * @param message the detail message.
     * @param metadata the contextual metadata.
     * @since 0.1.0-M4
     */
    public ArcaValidationException(String message, @Nullable Map<String, String> metadata) {
        super(ArcaErrorCode.VALIDATIONERROR, message, metadata);
    }

    /**
     * Creates a new exception with the given detail message, metadata, and cause.
     *
     * @param message the detail message.
     * @param metadata the contextual metadata.
     * @param cause the underlying cause.
     * @since 0.1.0-M4
     */
    public ArcaValidationException(String message, @Nullable Map<String, String> metadata, Throwable cause) {
        super(ArcaErrorCode.VALIDATIONERROR, message, metadata, cause);
    }
}
