package io.github.fr4ncisx.arca.wsfexv1.model;

/**
 * Request parameter for querying a previously authorized export voucher.
 *
 * @param salesPoint    the sales point number
 * @param voucherType   the voucher type code (e.g. 19)
 * @param voucherNumber the voucher number
 * @author fr4ncisx
 * @since 0.7.0
 */
public record ExportVoucherConsultRequest(
        int salesPoint,
        short voucherType,
        long voucherNumber
) {
}
