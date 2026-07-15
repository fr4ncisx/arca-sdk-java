package io.github.fr4ncisx.arca.wscdc.model;

/**
 * Represents a service error returned by the WSCDC service.
 *
 * @param code the error code
 * @param message the error message
 * @author fr4ncisx
 * @since 0.9.0
 */
public record WscdcError(
    int code,
    String message
) {}
