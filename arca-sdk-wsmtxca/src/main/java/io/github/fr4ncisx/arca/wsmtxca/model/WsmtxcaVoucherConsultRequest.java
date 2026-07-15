package io.github.fr4ncisx.arca.wsmtxca.model;

/**
 * Request parameters to consult a previously authorized voucher in WSMTXCA.
 *
 * @param voucherType the type code of the voucher
 * @param salesPoint the sales point of the voucher
 * @param voucherNumber the number of the voucher to query
 * @author fr4ncisx
 * @since 0.7.0
 */
public record WsmtxcaVoucherConsultRequest(
    short voucherType,
    int salesPoint,
    long voucherNumber
) {}
