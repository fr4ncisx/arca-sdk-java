package io.github.fr4ncisx.arca.wsfev1.model.catalog;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Details of an official optional field type registered in the ARCA catalog.
 * <p>
 * Exposes the code (id), description, and validity date range of optional fields
 * supported by ARCA.
 *
 * @param id          the official alphanumeric ID of the optional field
 * @param description the description of the optional field
 * @param since       the start date of validity, if defined
 * @param until       the end date of validity, if defined
 * @author fr4ncisx
 * @since 0.6.0
 */
public record OptionalFieldTypeInfo(
        String id,
        String description,
        Optional<LocalDate> since,
        Optional<LocalDate> until
) {

    public OptionalFieldTypeInfo {
        if (id == null || id.trim().isEmpty()) {
            throw new ArcaValidationException("id must not be null or blank");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ArcaValidationException("description must not be null or blank");
        }
        if (since == null) {
            throw new ArcaValidationException("since must not be null");
        }
        if (until == null) {
            throw new ArcaValidationException("until must not be null");
        }
    }
}
