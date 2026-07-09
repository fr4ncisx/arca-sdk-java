package io.github.fr4ncisx.arca.wsfev1.model;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

/**
 * Request containing parameters to query details of a previously authorized voucher.
 *
 * @param salesPoint    the sales point number
 * @param voucherType   the type of voucher
 * @param voucherNumber the voucher number
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record VoucherConsultRequest(
        int salesPoint,
        VoucherType voucherType,
        long voucherNumber
) {

    public VoucherConsultRequest {
        if (salesPoint <= 0) {
            throw new ArcaValidationException("salesPoint must be greater than 0");
        }
        if (voucherType == null) {
            throw new ArcaValidationException("voucherType must not be null");
        }
        if (voucherNumber <= 0) {
            throw new ArcaValidationException("voucherNumber must be greater than 0");
        }
    }
}
