package io.github.fr4ncisx.arca.core.exception;

/**
 * Exception thrown when a SOAP request is blocked because the Circuit Breaker is OPEN.
 * <p>
 * This prevents thread starvation and cascading failures in client applications when
 * the remote ARCA services are experiencing a severe outage.
 *
 * @author fr4ncisx
 * @since 1.1.0
 */
public final class ArcaCircuitOpenException extends ArcaSoapException {

    /**
     * Creates a new ArcaCircuitOpenException with a default message.
     */
    public ArcaCircuitOpenException() {
        super("Circuit Breaker is OPEN. Request blocked to prevent cascading failures.");
    }

    /**
     * Creates a new ArcaCircuitOpenException with a detailed message.
     *
     * @param message detailed description of the circuit state.
     */
    public ArcaCircuitOpenException(String message) {
        super(message);
    }
}
