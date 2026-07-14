package io.github.fr4ncisx.arca.wsfexv1.model;

/**
 * Represents a customs export permit (permiso de embarque).
 *
 * @param id                 the permit identification code
 * @param destinationCountry the destination country code
 * @author fr4ncisx
 * @since 0.7.0
 */
public record ExportPermit(String id, int destinationCountry) {
}
