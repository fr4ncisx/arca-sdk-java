package io.github.fr4ncisx.arca.wsaa.spi;

import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;

import java.security.KeyStore;

/**
 * Source of cryptographic material used by the WSAA authentication flow.
 * <p>
 * This port abstracts how certificate material is obtained. Implementations may
 * load it from disk, memory, secrets managers, hardware devices, or other
 * infrastructure-specific sources.
 * <p>
 * The contract intentionally exposes only a {@link KeyStore}, avoiding any
 * dependency on a concrete storage mechanism or certificate format.
 *
 * @author fr4ncisx
 * @since 0.1.0-M2
 */
@FunctionalInterface
public interface CertificateSource {
    /**
     * Loads the cryptographic material required by WSAA.
     * <p>
     * Each implementation decides how the material is resolved, but failures
     * that make the material unusable for authentication must be reported as
     * {@link ArcaAuthException}.
     *
     * @return loaded key store containing usable cryptographic material.
     * @throws ArcaAuthException if the material cannot be loaded or cannot be
     *         used for authentication.
     */
    KeyStore load() throws ArcaAuthException;
}
