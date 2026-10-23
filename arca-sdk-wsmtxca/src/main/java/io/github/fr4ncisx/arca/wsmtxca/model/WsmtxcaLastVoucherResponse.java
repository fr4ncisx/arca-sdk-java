package io.github.fr4ncisx.arca.wsmtxca.model;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import java.util.List;

/**
 * Response containing the last authorized voucher number in WSMTXCA.
 *
 * @param salesPoint the queried sales point
 * @param voucherType the queried voucher type
 * @param lastVoucherNumber the last authorized voucher number registered by ARCA
 * @param errors the list of errors returned by ARCA
 * @author fr4ncisx
 * @since 0.7.0
 */
public record WsmtxcaLastVoucherResponse(
    int salesPoint,
    short voucherType,
    long lastVoucherNumber,
    List<WsmtxcaError> errors
) {

    /**
     * Creates an immutable snapshot of the supplied collections.
     *
     * @param salesPoint the queried sales point
     * @param voucherType the queried voucher type
     * @param lastVoucherNumber the last authorized voucher number registered by ARCA
     * @param errors the list of errors returned by ARCA
     * @throws ArcaValidationException if a required collection or one of its elements is null.
     */
    public WsmtxcaLastVoucherResponse {
        if (errors == null) {
            throw new ArcaValidationException("errors must not be null");
        }
        try {
            errors = List.copyOf(errors);
        } catch (NullPointerException exception) {
            throw new ArcaValidationException("errors must not contain null entries", exception);
        }
    }
}
