package io.github.fr4ncisx.arca.core.exception;

/**
 * Catalog of structured error codes for the ARCA SDK.
 *
 * @author fr4ncisx
 * @since 0.1.0-M4
 */
public enum ArcaErrorCode {
    /**
     * Access ticket has expired.
     */
    TAEXPIRED,

    /**
     * Authentication failed (invalid credentials, signature failure, etc.).
     */
    AUTHFAILED,

    /**
     * SOAP communication timed out.
     */
    SOAPTIMEOUT,

    /**
     * SOAP service returned a fault response.
     */
    SOAPFAULT,

    /**
     * Taxpayer ID (CUIT) is invalid.
     */
    CUITINVALID,

    /**
     * Client certificate has expired or is invalid.
     */
    CERTIFICATEEXPIRED,

    /**
     * Local request validation failed.
     */
    VALIDATIONERROR,

    /**
     * XML parsing or mapping error.
     */
    PARSEERROR
}
