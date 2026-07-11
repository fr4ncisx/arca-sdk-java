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

    public CaeVatLine {
        if (vatType == null) {
            throw new ArcaValidationException("vatType must not be null");
        }
        if (taxBase < 0) {
            throw new ArcaValidationException("taxBase must be non-negative");
        }
        if (vatAmount < 0) {
            throw new ArcaValidationException("vatAmount must be non-negative");
        }
    }
}
