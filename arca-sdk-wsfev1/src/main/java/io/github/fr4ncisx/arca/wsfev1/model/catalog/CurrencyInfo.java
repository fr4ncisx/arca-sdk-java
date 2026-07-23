package io.github.fr4ncisx.arca.wsfev1.model.catalog;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/**
 * Details of an official billing currency registered in the ARCA catalog.
 * <p>
 * This record exposes the code, description, and validity date range of
 * currencies (e.g. PES, DOL) supported by ARCA.
 *
 * @param code        the official currency string identifier (e.g. "PES")
 * @param description the description of the currency
 * @param since       the start date of validity, or null if not defined
 * @param until       the end date of validity, or null if not defined
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public record CurrencyInfo(
        String code,
        String description,
        @Nullable LocalDate since,
        @Nullable LocalDate until
) {

    public CurrencyInfo {
        if (code == null || code.trim().isEmpty()) {
            throw new ArcaValidationException("code must not be null or blank");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ArcaValidationException("description must not be null or blank");
        }
    }
}
