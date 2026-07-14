package io.github.fr4ncisx.arca.wsfev1.model.catalog;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

/**
 * Details of an official commercial activity registered in the ARCA catalog.
 * <p>
 * Exposes the activity ID, order, and description.
 *
 * @param id          the official commercial activity ID
 * @param order       the activity priority/order index
 * @param description the description of the commercial activity
 * @author fr4ncisx
 * @since 0.6.0
 */
public record ActivityInfo(
        long id,
        short order,
        String description
) {

    public ActivityInfo {
        if (id < 0) {
            throw new ArcaValidationException("id must be non-negative");
        }
        if (order < 0) {
            throw new ArcaValidationException("order must be non-negative");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ArcaValidationException("description must not be null or blank");
        }
    }
}
