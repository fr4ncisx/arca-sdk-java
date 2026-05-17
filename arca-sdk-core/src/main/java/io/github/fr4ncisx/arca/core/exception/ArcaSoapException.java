package io.github.fr4ncisx.arca.core.exception;

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
     * Creates a new exception with the given detail message.
     *
     * @param message the detail message.
     */
    public ArcaSoapException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given detail message and cause.
     *
     * @param message the detail message.
     * @param cause the underlying cause.
     */
    public ArcaSoapException(String message, Throwable cause) {
        super(message, cause);
    }
}
