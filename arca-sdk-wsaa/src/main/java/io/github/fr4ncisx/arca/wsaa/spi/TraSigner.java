package io.github.fr4ncisx.arca.wsaa.spi;

import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;

/**
 * Service Provider Interface (SPI) for signing Ticket de Requerimiento de Acceso (TRA) payloads.
 * <p>
 * Implementations of this interface are responsible for generating PKCS#7 (CMS)
 * signatures for XML payloads required by the WSAA authentication flow. This allows
 * the SDK to support hardware security modules (HSM), cloud key management services (KMS),
 * or other secure signing mechanisms.
 *
 * @author fr4ncisx
 * @since 1.1.0
 */
public interface TraSigner {

    /**
     * Signs the given XML string using PKCS#7 (CMS) SignedData.
     * <p>
     * The generated signature must embed the original XML payload (attached data)
     * and must be Base64 encoded, matching the structure expected by the ARCA WSAA service.
     *
     * @param xml the XML string payload to sign.
     * @return the Base64-encoded CMS SignedData signature.
     * @throws ArcaAuthException if the signing process fails.
     */
    String sign(String xml) throws ArcaAuthException;
}
