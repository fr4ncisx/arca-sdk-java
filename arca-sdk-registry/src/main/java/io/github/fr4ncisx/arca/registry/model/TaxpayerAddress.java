package io.github.fr4ncisx.arca.registry.model;

import org.jspecify.annotations.Nullable;

/**
 * Represents a taxpayer's physical address.
 * <p>
 * This record encapsulates postal code, street address, city, and province information.
 *
 * @param postalCode the postal code
 * @param address the street address
 * @param provinceDescription the name of the province
 * @param provinceId the identifier of the province
 * @param city the city name
 * @param addressType the classification of the address (e.g., fiscal, real)
 * @param additionalData additional address metadata
 * @param additionalDataType classification type for the additional data
 * @param order the order sequence index
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public record TaxpayerAddress(
    @Nullable String postalCode,
    @Nullable String address,
    @Nullable String provinceDescription,
    @Nullable Integer provinceId,
    @Nullable String city,
    @Nullable String addressType,
    @Nullable String additionalData,
    @Nullable String additionalDataType,
    @Nullable Integer order
) {}
