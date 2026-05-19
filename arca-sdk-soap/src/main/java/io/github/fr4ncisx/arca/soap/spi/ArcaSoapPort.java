package io.github.fr4ncisx.arca.soap.spi;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;

/**
 * Generic SOAP transport port used by the ARCA SDK.
 * <p>
 * This interface models a framework-independent request/response invocation
 * boundary. Application services, use cases, and adapters can depend on this
 * contract without exposing Metro, JAX-WS, JAXB, or any other concrete SOAP
 * runtime type in their public API.
 * <p>
 * Implementations may delegate to a real SOAP client, a generated stub, a test
 * double, or any other transport mechanism, as long as framework-specific
 * details remain encapsulated behind this port.
 *
 * @param <R> request type accepted by the SOAP operation.
 * @param <S> response type returned by the SOAP operation.
 * @author fr4ncisx
 * @since 0.1.0-M2
 */
@FunctionalInterface
public interface ArcaSoapPort<R, S> {
    /**
     * Invokes a SOAP operation using the provided request and returns the
     * corresponding response.
     * <p>
     * The method represents the SDK-level SOAP invocation contract. Concrete
     * implementations are responsible for translating transport, serialization,
     * SOAP fault, and runtime-specific failures into {@link ArcaSoapException}.
     *
     * @param request operation request payload.
     * @return operation response payload.
     * @throws ArcaSoapException if the SOAP invocation fails.
     */
    S invoke(R request) throws ArcaSoapException;
}
