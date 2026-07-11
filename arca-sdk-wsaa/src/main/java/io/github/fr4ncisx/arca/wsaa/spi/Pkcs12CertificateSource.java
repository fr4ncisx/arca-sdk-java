package io.github.fr4ncisx.arca.wsaa.spi;

import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * PKCS#12-backed implementation of {@link CertificateSource}.
 * <p>
 * This source loads cryptographic material from a PKCS#12 file, usually with a
 * {@code .p12} or {@code .pfx} extension, and validates that the contained
 * X.509 certificate can be used by the WSAA authentication flow.
 * <p>
 * Instances are created through {@link #fromPath(Path, char[])}. The factory
 * only validates local construction arguments and captures the provided
 * references. It does not access the file system, does not inspect the
 * certificate content, does not clone the password array, and does not clear
 * the password array.
 * <p>
 * Every call to {@link #load()} opens the PKCS#12 file again, loads a new
 * {@link KeyStore}, and validates the certificate again. The loaded
 * {@link KeyStore} is not cached between invocations.
 * <p>
 * Authentication failures are reported with stable messages that do not expose
 * the certificate path, password, or key store bytes.
 *
 * @author fr4ncisx
 * @since 0.1.0-M2
 */
public final class Pkcs12CertificateSource implements CertificateSource {

    private static final String KEY_STORE_TYPE = "PKCS12";

    private final Path path;
    private final char[] password;

    private Pkcs12CertificateSource(Path path, char[] password) {
        this.path = path;
        this.password = password;
    }

    /**
     * Creates a PKCS#12 certificate source from a file system path and password.
     * <p>
     * This factory validates only construction arguments. It does not open the
     * file, does not validate the PKCS#12 format, and does not check certificate
     * validity. Those operations are performed by {@link #load()}.
     * <p>
     * The password reference is captured exactly as received. It is not cloned
     * or cleared by this factory.
     *
     * @param path     the path to the PKCS#12 file.
     * @param password the password used to load the PKCS#12 key store.
     * @return a PKCS#12-backed certificate source.
     * @throws ArcaValidationException if the path is null, the password is null,
     *                                 or the password is empty.
     */
    public static Pkcs12CertificateSource fromPath(Path path, char[] password) {
        if (path == null) {
            throw new ArcaValidationException("the path must not be null");
        }
        if (password == null) {
            throw new ArcaValidationException("the password must not be null");
        }
        if (password.length == 0) {
            throw new ArcaValidationException("the password must not be empty");
        }
        return new Pkcs12CertificateSource(path, password);
    }

    /**
     * Returns the password used to load the PKCS#12 key store.
     *
     * @return the key store password
     */
    public char[] getPassword() {
        return password;
    }

    /**
     * Loads and validates the PKCS#12 key store.
     * <p>
     * This method opens the configured file on every invocation, loads a fresh
     * {@link KeyStore}, and verifies that at least one private-key entry has an
     * X.509 certificate that is currently valid.
     * <p>
     * File system errors, incorrect passwords, invalid PKCS#12 content, and
     * unusable certificates are translated to {@link ArcaAuthException}.
     *
     * @return a newly loaded PKCS#12 key store.
     * @throws ArcaAuthException if the PKCS#12 file does not exist, cannot be
     *                           loaded, has an invalid format, has an incorrect password, or does
     *                           not contain a currently valid X.509 certificate usable for
     *                           authentication.
     */
    @Override
    public KeyStore load() throws ArcaAuthException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);

            try (InputStream inputStream = Files.newInputStream(path)) {
                keyStore.load(inputStream, password);
            }

            validateUsableCertificate(keyStore);

            return keyStore;
        } catch (NoSuchFileException exception) {
            throw new ArcaAuthException("PKCS#12 certificate file does not exist");
        } catch (IOException | GeneralSecurityException exception) {
            throw new ArcaAuthException("Unable to load PKCS#12 certificate material");
        }
    }

    private static void validateUsableCertificate(KeyStore keyStore)
            throws KeyStoreException, ArcaAuthException {
        CertificateStatus status = resolveCertificateStatus(keyStore);

        if (status != CertificateStatus.VALID) {
            throw new ArcaAuthException(authenticationErrorMessageFor(status));
        }
    }

    private static String authenticationErrorMessageFor(CertificateStatus status) {
        return switch (status) {
            case EXPIRED -> "PKCS#12 certificate is expired";
            case NOT_YET_VALID -> "PKCS#12 certificate is not valid yet";
            case MISSING_OR_UNUSABLE ->
                    "PKCS#12 key store does not contain a usable X.509 certificate";
            case VALID -> throw new IllegalArgumentException("VALID status has no authentication error");
        };
    }

    private static CertificateStatus resolveCertificateStatus(KeyStore keyStore)
            throws KeyStoreException {
        Enumeration<String> aliases = keyStore.aliases();
        CertificateStatus status = CertificateStatus.MISSING_OR_UNUSABLE;

        while (aliases.hasMoreElements() && status != CertificateStatus.VALID) {
            String alias = aliases.nextElement();

            if (keyStore.isKeyEntry(alias)) {
                Certificate certificate = keyStore.getCertificate(alias);

                if (certificate instanceof X509Certificate x509Certificate) {
                    status = resolveCertificateStatus(x509Certificate, status);
                }
            }
        }

        return status;
    }

    private static CertificateStatus resolveCertificateStatus(
            X509Certificate certificate,
            CertificateStatus currentStatus
    ) {
        try {
            certificate.checkValidity();
            return CertificateStatus.VALID;
        } catch (CertificateExpiredException exception) {
            return CertificateStatus.EXPIRED;
        } catch (CertificateNotYetValidException exception) {
            if (currentStatus == CertificateStatus.EXPIRED) {
                return CertificateStatus.EXPIRED;
            }
            return CertificateStatus.NOT_YET_VALID;
        }
    }

    /**
     * Internal validation status for certificate material discovered inside the
     * PKCS#12 key store.
     */
    private enum CertificateStatus {

        /**
         * A currently valid X.509 certificate was found.
         */
        VALID,

        /**
         * At least one X.509 certificate was found, but it is expired.
         */
        EXPIRED,

        /**
         * At least one X.509 certificate was found, but it is not valid yet.
         */
        NOT_YET_VALID,

        /**
         * No private-key entry with a usable X.509 certificate was found.
         */
        MISSING_OR_UNUSABLE
    }
}