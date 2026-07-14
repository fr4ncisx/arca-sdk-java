package io.github.fr4ncisx.arca.wsfev1.model.catalog;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

/**
 * Details of an official receiver VAT condition registered in the ARCA catalog.
 * <p>
 * Exposes the condition ID, description, and the associated voucher class (e.g. 'A', 'B').
 *
 * @param id           the official receiver VAT condition numeric ID
 * @param description  the name/description of the VAT condition
 * @param voucherClass the associated voucher class (e.g. 'A', 'B')
 * @author fr4ncisx
 * @since 0.6.0
 */
public record VatConditionInfo(
        int id,
        String description,
        String voucherClass
) {

    public VatConditionInfo {
        if (id < 0) {
            throw new ArcaValidationException("id must be non-negative");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ArcaValidationException("description must not be null or blank");
        }
        if (voucherClass == null || voucherClass.trim().isEmpty()) {
            throw new ArcaValidationException("voucherClass must not be null or blank");
        }
    }
}
