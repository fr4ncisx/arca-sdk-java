package io.github.fr4ncisx.arca.wsmtxca.model;

import java.math.BigDecimal;

/**
 * Represents other taxes detail (otros tributos) for the WSMTXCA service.
 *
 * @param taxId the code of the tax type
 * @param description the description of the tax
 * @param baseAmount the taxable base amount
 * @param rate the tax rate / percentage
 * @param taxAmount the calculated tax amount
 * @author fr4ncisx
 * @since 0.7.0
 */
public record OtherTaxDetail(
    short taxId,
    String description,
    BigDecimal baseAmount,
    BigDecimal rate,
    BigDecimal taxAmount
) {}
