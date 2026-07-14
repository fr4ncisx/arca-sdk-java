package io.github.fr4ncisx.arca.wsfev1.model.catalog;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

/**
 * Details of an official country registered in the ARCA catalog.
 * <p>
 * Exposes the numeric ID and description of countries recognized by ARCA.
 *
 * @param id          the official country numeric code
 * @param description the name of the country
 * @author fr4ncisx
 * @since 0.6.0
 */
public record CountryInfo(
        short id,
        String description
) {

    public CountryInfo {
        if (id < 0) {
            throw new ArcaValidationException("id must be non-negative");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ArcaValidationException("description must not be null or blank");
        }
    }
}
