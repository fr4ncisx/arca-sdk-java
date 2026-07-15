package io.github.fr4ncisx.arca.wsmtxca.model;

import org.jspecify.annotations.Nullable;

/**
 * Represents an associated voucher (e.g., in a credit or debit note) for the WSMTXCA service.
 *
 * @param voucherType the type code of the associated voucher
 * @param salesPoint the sales point of the associated voucher
 * @param voucherNumber the number of the associated voucher
 * @param cuit the CUIT of the issuer if different, or null
 * @author fr4ncisx
 * @since 0.7.0
 */
public record AssociatedVoucher(
    short voucherType,
    int salesPoint,
    long voucherNumber,
    @Nullable Long cuit
) {}
