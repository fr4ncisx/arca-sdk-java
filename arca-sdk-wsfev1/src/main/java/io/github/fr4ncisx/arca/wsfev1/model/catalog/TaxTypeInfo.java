package io.github.fr4ncisx.arca.wsfev1.model.catalog;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/**
 * Details of an official secondary tax or rate category registered in the ARCA catalog.
 * <p>
 * Exposes the numeric ID, description, and validity date range of taxes
 * recognized by ARCA.
 *
 * @param id          the official tax numeric code
 * @param description the description of the tax type
 * @param since       the start date of validity, or null if not defined
 * @param until       the end date of validity, or null if not defined
 * @author fr4ncisx
 * @since 0.6.0
 */
public record TaxTypeInfo(
        short id,
        String description,
        @Nullable LocalDate since,
        @Nullable LocalDate until
) {

    public TaxTypeInfo {
        if (id < 0) {
            throw new ArcaValidationException("id must be non-negative");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ArcaValidationException("description must not be null or blank");
        }
    }
}
