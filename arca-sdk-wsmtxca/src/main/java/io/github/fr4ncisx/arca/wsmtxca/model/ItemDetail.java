package io.github.fr4ncisx.arca.wsmtxca.model;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * Represents an itemized line detail of a voucher in the WSMTXCA service.
 *
 * @param unitsMtx units indicator or matrix code units
 * @param gtin the GTIN/barcode of the product (maps to codigoMtx)
 * @param internalCode the internal code of the product (maps to codigo)
 * @param description the description of the item
 * @param quantity the quantity of the item
 * @param unitOfMeasureCode the code of the unit of measure
 * @param unitPrice the unit price of the item
 * @param discountAmount the discount amount for this item
 * @param vatConditionCode the VAT rate condition code
 * @param vatAmount the calculated VAT amount for this item
 * @param itemAmount the total item amount (price * quantity - discount + vat)
 * @author fr4ncisx
 * @since 0.7.0
 */
public record ItemDetail(
    @Nullable Integer unitsMtx,
    @Nullable String gtin,
    @Nullable String internalCode,
    String description,
    @Nullable BigDecimal quantity,
    short unitOfMeasureCode,
    @Nullable BigDecimal unitPrice,
    @Nullable BigDecimal discountAmount,
    short vatConditionCode,
    @Nullable BigDecimal vatAmount,
    BigDecimal itemAmount
) {}
