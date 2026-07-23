package io.github.fr4ncisx.arca.wsfev1.model.catalog;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/**
 * Details of an official buyer identification document type registered in the ARCA catalog.
 * <p>
 * This record exposes the code, description, and validity date range of
 * identification document types (e.g. CUIT, DNI) supported by ARCA.
 *
 * @param code        the official numeric document type code
 * @param description the description of the document type
 * @param since       the start date of validity, or null if not defined
 * @param until       the end date of validity, or null if not defined
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public record DocumentTypeInfo(
        int code,
        String description,
        @Nullable LocalDate since,
        @Nullable LocalDate until
) {

    public DocumentTypeInfo {
        if (code <= 0) {
            throw new ArcaValidationException("code must be greater than 0");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ArcaValidationException("description must not be null or blank");
        }
    }
}
