package io.github.fr4ncisx.arca.wsfev1.model.lastvoucher;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;

/**
 * Result of querying the last authorized voucher number.
 *
 * @param salesPoint  the queried sales point
 * @param voucherType the queried voucher type
 * @param lastNumber  the last authorized voucher number (0 if no vouchers exist)
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public record LastVoucherResponse(int salesPoint, VoucherType voucherType, long lastNumber) {

    public LastVoucherResponse {
        if (salesPoint <= 0) {
            throw new ArcaValidationException("salesPoint must be greater than 0");
        }
        if (voucherType == null) {
            throw new ArcaValidationException("voucherType must not be null");
        }
        if (lastNumber < 0) {
            throw new ArcaValidationException("lastNumber must be non-negative");
        }
    }
}
