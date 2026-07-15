package io.github.fr4ncisx.arca.wsmtxca.model;

import java.math.BigDecimal;

/**
 * Represents a VAT subtotal detail (alícuota de IVA) for the WSMTXCA service.
 *
 * @param vatConditionId the VAT condition / rate code
 * @param baseAmount the taxable base amount
 * @param taxAmount the calculated tax amount
 * @author fr4ncisx
 * @since 0.7.0
 */
public record TaxDetail(
    short vatConditionId,
    BigDecimal baseAmount,
    BigDecimal taxAmount
) {}
