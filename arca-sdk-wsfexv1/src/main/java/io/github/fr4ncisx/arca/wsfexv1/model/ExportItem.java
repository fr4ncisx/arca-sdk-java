package io.github.fr4ncisx.arca.wsfexv1.model;

import java.math.BigDecimal;

/**
 * Represents a single item line in an export invoice.
 *
 * @param code          the product or service code
 * @param description   the item description
 * @param quantity      the quantity
 * @param unitOfMeasure the unit of measure code
 * @param unitPrice     the unit price
 * @param discount      the discount amount
 * @param totalAmount   the total amount for this line item
 * @author fr4ncisx
 * @since 0.7.0
 */
public record ExportItem(
        String code,
        String description,
        BigDecimal quantity,
        int unitOfMeasure,
        BigDecimal unitPrice,
        BigDecimal discount,
        BigDecimal totalAmount
) {
}
