package io.github.fr4ncisx.arca.wsmtxca.model;

/**
 * Represents an error or observation returned by the ARCA WSMTXCA service.
 *
 * @param code the error or observation code
 * @param description the detailed error description
 * @author fr4ncisx
 * @since 0.7.0
 */
public record WsmtxcaError(
    int code,
    String description
) {}
