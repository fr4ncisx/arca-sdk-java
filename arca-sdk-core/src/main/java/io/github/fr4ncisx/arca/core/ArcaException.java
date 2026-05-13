package io.github.fr4ncisx.arca.core;

/**
 * Base exception for all ARCA SDK errors.
 * Sealed hierarchy restricts subclasses to {@link ArcaAuthException},
 * {@link ArcaSoapException} and {@link ArcaValidationException}.
 * Extends RuntimeException so callers are not forced to catch.
 *
 * @see ArcaAuthException
 * @see ArcaSoapException
 * @see ArcaValidationException
 */
public sealed class ArcaException extends RuntimeException
        permits ArcaAuthException, ArcaSoapException, ArcaValidationException {

    /**
     * Creates a new exception with the given detail message.
     *
     * @param message the detail message.
     */
    public ArcaException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given detail message and cause.
     *
     * @param message the detail message.
     * @param cause the underlying cause.
     */
    public ArcaException(String message, Throwable cause) {
        super(message, cause);
    }
}
