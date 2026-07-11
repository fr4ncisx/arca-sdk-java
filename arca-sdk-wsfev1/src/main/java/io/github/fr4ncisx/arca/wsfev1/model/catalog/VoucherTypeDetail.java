package io.github.fr4ncisx.arca.wsfev1.model.catalog;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Details of an official voucher type registered in the ARCA catalog.
 * <p>
 * This record exposes the code, description, and validity date range of
 * voucher types (e.g. Factura A, Nota de Crédito B) supported by ARCA.
 *
 * @param code        the official numeric voucher type code
 * @param description the description of the voucher type
 * @param since       the start date of validity, if defined
 * @param until       the end date of validity, if defined
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public record VoucherTypeDetail(
        int code,
        String description,
        Optional<LocalDate> since,
        Optional<LocalDate> until
) {

    public VoucherTypeDetail {
        if (code <= 0) {
            throw new ArcaValidationException("code must be greater than 0");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ArcaValidationException("description must not be null or blank");
        }
        if (since == null) {
            throw new ArcaValidationException("since must not be null");
        }
        if (until == null) {
            throw new ArcaValidationException("until must not be null");
        }
    }
}
