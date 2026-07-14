package io.github.fr4ncisx.arca.wsfexv1.model;

/**
 * Represents an optional custom metadata field for export invoicing.
 *
 * @param id    the optional field identifier
 * @param value the custom value
 * @author fr4ncisx
 * @since 0.7.0
 */
public record ExportOptionalField(String id, String value) {
}
