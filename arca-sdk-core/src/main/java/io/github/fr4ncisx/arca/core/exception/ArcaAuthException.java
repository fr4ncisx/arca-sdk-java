package io.github.fr4ncisx.arca.core.exception;

import java.util.Map;

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
        super(ArcaErrorCode.AUTHFAILED, message);
    }

    /**
     * Creates a new exception with the given detail message and cause.
     *
     * @param message the detail message.
     * @param cause the underlying cause.
     */
    public ArcaAuthException(String message, Throwable cause) {
        super(ArcaErrorCode.AUTHFAILED, message, cause);
    }

    /**
     * Creates a new exception with the given detail message and metadata.
     *
     * @param message the detail message.
     * @param metadata the contextual metadata.
     * @since 0.1.0-M4
     */
    public ArcaAuthException(String message, Map<String, String> metadata) {
        super(ArcaErrorCode.AUTHFAILED, message, metadata);
    }

    /**
     * Creates a new exception with the given detail message, metadata, and cause.
     *
     * @param message the detail message.
     * @param metadata the contextual metadata.
     * @param cause the underlying cause.
     * @since 0.1.0-M4
     */
    public ArcaAuthException(String message, Map<String, String> metadata, Throwable cause) {
        super(ArcaErrorCode.AUTHFAILED, message, metadata, cause);
    }
}
