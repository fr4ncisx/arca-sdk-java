package io.github.fr4ncisx.arca.wsfexv1.model;

/**
 * Request parameter for retrieving the last authorized export voucher.
 *
 * @param salesPoint  the sales point number
 * @param voucherType the voucher type (e.g. 19 for export invoices)
 * @author fr4ncisx
 * @since 0.7.0
 */
public record LastExportVoucherRequest(int salesPoint, short voucherType) {
}
