package io.github.fr4ncisx.arca.core;

/**
 * Input validation error.
 * Thrown when a method argument or configuration value fails validation
 * (null, out of range, wrong format).
 */
public non-sealed class ArcaValidationException extends ArcaException {

    /**
     * Creates a new exception with the given detail message.
     *
     * @param message the detail message.
     */
    public ArcaValidationException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given detail message and cause.
     *
     * @param message the detail message.
     * @param cause the underlying cause.
     */
    public ArcaValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
