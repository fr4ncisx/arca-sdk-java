package io.github.fr4ncisx.arca.core.exception;

import io.github.fr4ncisx.arca.core.logging.LogSanitizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Base exception for all ARCA SDK errors.
 * <p>
 * Sealed hierarchy restricts subclasses to {@link ArcaAuthException},
 * {@link ArcaSoapException} and {@link ArcaValidationException}.
 * Extends RuntimeException so callers are not forced to catch.
 *
 * @author fr4ncisx
 * @since 0.1.0-M1
 */
public sealed class ArcaException extends RuntimeException
        permits ArcaAuthException, ArcaSoapException, ArcaValidationException {

    private final ArcaErrorCode errorCode;
    private final Map<String, String> metadata;

    /**
     * Creates a new exception with the given error code and detail message.
     *
     * @param errorCode the structured error code.
     * @param message the detail message.
     * @since 0.1.0-M4
     */
    public ArcaException(ArcaErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "The ArcaErrorCode cannot be null.");
        this.metadata = Map.of();
    }

    /**
     * Creates a new exception with the given error code, detail message, and cause.
     *
     * @param errorCode the structured error code.
     * @param message the detail message.
     * @param cause the underlying cause.
     * @since 0.1.0-M4
     */
    public ArcaException(ArcaErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "The ArcaErrorCode cannot be null.");
        this.metadata = Map.of();
    }

    /**
     * Creates a new exception with the given error code, detail message, and metadata.
     *
     * @param errorCode the structured error code.
     * @param message the detail message.
     * @param metadata the contextual metadata.
     * @since 0.1.0-M4
     */
    public ArcaException(ArcaErrorCode errorCode, String message, @Nullable Map<String, String> metadata) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "The ArcaErrorCode cannot be null.");
        this.metadata = validateAndSanitizeMetadata(metadata);
    }

    /**
     * Creates a new exception with the given error code, detail message, metadata, and cause.
     *
     * @param errorCode the structured error code.
     * @param message the detail message.
     * @param metadata the contextual metadata.
     * @param cause the underlying cause.
     * @since 0.1.0-M4
     */
    public ArcaException(ArcaErrorCode errorCode, String message, @Nullable Map<String, String> metadata, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "The ArcaErrorCode cannot be null.");
        this.metadata = validateAndSanitizeMetadata(metadata);
    }

    /**
     * Compatibility constructor with a default error code.
     *
     * @param message the detail message.
     */
    protected ArcaException(String message) {
        super(message);
        this.errorCode = ArcaErrorCode.VALIDATIONERROR;
        this.metadata = Map.of();
    }

    /**
     * Compatibility constructor with a default error code and cause.
     *
     * @param message the detail message.
     * @param cause the underlying cause.
     */
    protected ArcaException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ArcaErrorCode.VALIDATIONERROR;
        this.metadata = Map.of();
    }

    /**
     * Returns the structured error code for this exception.
     *
     * @return the error code.
     * @since 0.1.0-M4
     */
    public ArcaErrorCode errorCode() {
        return errorCode;
    }

    /**
     * Returns the sanitized metadata map associated with this exception.
     *
     * @return the metadata map.
     * @since 0.1.0-M4
     */
    public Map<String, String> metadata() {
        return metadata;
    }

    private static Map<String, String> validateAndSanitizeMetadata(@Nullable Map<String, String> rawMetadata) {
        if (rawMetadata == null)
            return Map.of();
        Map<String, String> sanitized = new HashMap<>();
        for (Map.Entry<String, String> entry : rawMetadata.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.trim().isEmpty())
                throw new IllegalArgumentException("Exception metadata key cannot be null, empty, or blank.");
            if (value == null)
                throw new IllegalArgumentException("Exception metadata value for key '" + key + "' cannot be null.");
            sanitized.put(key, LogSanitizer.sanitizeKeyValue(key, value));
        }
        return Collections.unmodifiableMap(sanitized);
    }
}
