package io.github.fr4ncisx.arca.wsfexv1.model;

/**
 * Represents a voucher associated with an export transaction.
 *
 * @param type       the voucher type code
 * @param salesPoint the sales point number
 * @param number     the voucher number
 * @param cuit       the taxpayer identifier (cuit) associated with the voucher
 * @author fr4ncisx
 * @since 0.7.0
 */
public record AssociatedVoucher(short type, int salesPoint, long number, long cuit) {
}
