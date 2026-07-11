package io.github.fr4ncisx.arca.registry.model;

import org.jspecify.annotations.Nullable;

/**
 * Represents a special tax regime applied to a taxpayer.
 *
 * @param taxId the unique identifier of the tax associated with this regime
 * @param regimeId the unique identifier of the regime
 * @param regimeDescription the description of the regime
 * @param state the state of the regime (e.g., active, inactive)
 * @param period the registration period of the regime
 * @param regimeType the classification type of the regime
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public record TaxpayerRegime(
    @Nullable Integer taxId,
    @Nullable Integer regimeId,
    @Nullable String regimeDescription,
    @Nullable String state,
    @Nullable Integer period,
    @Nullable String regimeType
) {}
