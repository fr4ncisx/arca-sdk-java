package io.github.fr4ncisx.arca.wsfev1.model.catalog;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/**
 * Details of an official VAT tax rate category registered in the ARCA catalog.
 * <p>
 * This record exposes the code, description, and validity date range of
 * VAT rates (e.g. 21%, 10.5%) supported by ARCA.
 *
 * @param code        the official numeric VAT type code
 * @param description the description of the VAT rate category
 * @param since       the start date of validity, or null if not defined
 * @param until       the end date of validity, or null if not defined
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public record VatTypeInfo(
        int code,
        String description,
        @Nullable LocalDate since,
        @Nullable LocalDate until
) {

    public VatTypeInfo {
        if (code < 0) {
            throw new ArcaValidationException("code must be non-negative");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ArcaValidationException("description must not be null or blank");
        }
    }
}
