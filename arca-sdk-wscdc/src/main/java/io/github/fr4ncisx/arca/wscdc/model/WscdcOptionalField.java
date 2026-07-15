package io.github.fr4ncisx.arca.wscdc.model;

/**
 * Represents an optional field key-value pair for the WSCDC service.
 *
 * @param id the identifier of the optional field
 * @param value the value of the optional field
 * @author fr4ncisx
 * @since 0.9.0
 */
public record WscdcOptionalField(
    String id,
    String value
) {}
