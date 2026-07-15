package io.github.fr4ncisx.arca.wsmtxca.model;

/**
 * Request parameters to obtain the last authorized voucher number in WSMTXCA.
 *
 * @param salesPoint the sales point to query
 * @param voucherType the type code of the voucher
 * @author fr4ncisx
 * @since 0.7.0
 */
public record WsmtxcaLastVoucherRequest(
    int salesPoint,
    short voucherType
) {}
