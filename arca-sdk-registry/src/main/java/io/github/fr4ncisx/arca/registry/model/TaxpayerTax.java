package io.github.fr4ncisx.arca.registry.model;

import org.jspecify.annotations.Nullable;
import java.time.LocalDateTime;

/**
 * Represents a tax inscribed to a taxpayer.
 *
 * @param taxId the unique identifier of the tax
 * @param taxDescription the description of the tax
 * @param state the state of the tax inscription (e.g., active, inactive)
 * @param enrollmentDate the timestamp when the taxpayer enrolled in this tax
 * @param period the period of the tax inscription
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public record TaxpayerTax(
    @Nullable Integer taxId,
    @Nullable String taxDescription,
    @Nullable String state,
    @Nullable LocalDateTime enrollmentDate,
    @Nullable Integer period
) {}
