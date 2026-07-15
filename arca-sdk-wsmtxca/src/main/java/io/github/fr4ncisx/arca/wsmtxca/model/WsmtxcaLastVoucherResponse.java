package io.github.fr4ncisx.arca.wsmtxca.model;

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
) {}
