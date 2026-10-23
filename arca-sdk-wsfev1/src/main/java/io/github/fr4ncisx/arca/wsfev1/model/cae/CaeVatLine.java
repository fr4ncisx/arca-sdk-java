package io.github.fr4ncisx.arca.wsfev1.model.cae;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.model.common.VatType;

/**
 * Represents a VAT line item in a CAE authorization request.
 *
 * @param vatType   the VAT category type
 * @param taxBase   the net amount subject to tax
 * @param vatAmount the calculated VAT tax amount
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public record CaeVatLine(VatType vatType, double taxBase, double vatAmount) {

    /**
     * Creates a VAT line with finite non-negative amounts.
     *
     * @param vatType the VAT category type
     * @param taxBase the net amount subject to tax
     * @param vatAmount the calculated VAT tax amount
     * @throws ArcaValidationException if the VAT type is null or an amount is negative or non-finite.
     */
    public CaeVatLine {
        if (vatType == null) {
            throw new ArcaValidationException("vatType must not be null");
        }
        if (!Double.isFinite(taxBase) || taxBase < 0) {
            throw new ArcaValidationException("taxBase must be finite and non-negative");
        }
        if (!Double.isFinite(vatAmount) || vatAmount < 0) {
            throw new ArcaValidationException("vatAmount must be finite and non-negative");
        }
    }
}
