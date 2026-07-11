package io.github.fr4ncisx.arca.registry.model;

import org.jspecify.annotations.Nullable;

/**
 * Represents an economic activity carried out by a taxpayer.
 *
 * @param activityId the unique identifier of the activity
 * @param activityDescription the description of the activity
 * @param nomenclator the nomenclator identifier
 * @param order the sequence order of the activity
 * @param period the registration period of the activity
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public record TaxpayerActivity(
    @Nullable Long activityId,
    @Nullable String activityDescription,
    @Nullable Integer nomenclator,
    @Nullable Integer order,
    @Nullable Integer period
) {}
