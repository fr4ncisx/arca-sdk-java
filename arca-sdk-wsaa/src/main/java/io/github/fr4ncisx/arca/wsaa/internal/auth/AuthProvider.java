package io.github.fr4ncisx.arca.wsaa.internal.auth;

import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;

/**
 * Internal authentication provider port for WSAA services.
 * <p>
 * This port is responsible for obtaining and renewing access tickets
 * required to communicate with ARCA business web services.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public interface AuthProvider {

    /**
     * Resolves a valid access ticket for the requested service.
     * <p>
     * Implementations should handle caching, expiration checks, and
     * concurrent token renewal to prevent duplicate remote requests.
     *
     * @param service the ARCA service identifier (e.g., "wsfe")
     * @return a valid access ticket containing the token and signature
     * @throws ArcaAuthException if authentication fails due to invalid credentials,
     *                           cryptographic errors, or rejection from WSAA
     * @throws ArcaSoapException if a network or SOAP transport error occurs
     */
    ArcaAccessTicket authenticate(String service) throws ArcaAuthException, ArcaSoapException;
}
