package io.github.fr4ncisx.arca.wsfev1.model.lastvoucher;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;

/**
 * Request parameter to query the last authorized voucher number.
 *
 * @param salesPoint  the target sales point (must be positive)
 * @param voucherType the type of voucher being queried
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public record LastVoucherRequest(int salesPoint, VoucherType voucherType) {

    public LastVoucherRequest {
        if (salesPoint <= 0) {
            throw new ArcaValidationException("salesPoint must be greater than 0");
        }
        if (voucherType == null) {
            throw new ArcaValidationException("voucherType must not be null");
        }
    }
}
