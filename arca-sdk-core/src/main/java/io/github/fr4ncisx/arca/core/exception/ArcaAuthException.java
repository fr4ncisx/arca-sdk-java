package io.github.fr4ncisx.arca.core.exception;

/**
 * Authentication error returned by WSAA.
 * <p>
 * Thrown when the WSAA service rejects a login request
 * (invalid CMS, expired certificate, malformed TRA).
 *
 * @author fr4ncisx
 * @since 0.1.0-M1
 */
public non-sealed class ArcaAuthException extends ArcaException {

    /**
     * Creates a new exception with the given detail message.
     *
     * @param message the detail message.
     */
    public ArcaAuthException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given detail message and cause.
     *
     * @param message the detail message.
     * @param cause the underlying cause.
     */
    public ArcaAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
