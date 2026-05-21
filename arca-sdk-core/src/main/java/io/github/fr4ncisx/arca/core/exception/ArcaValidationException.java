package io.github.fr4ncisx.arca.core.exception;

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
