package io.github.fr4ncisx.arca.wsmtxca.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Represents a request to authorize a voucher with itemized details (WSMTXCA).
 *
 * @param voucherType the type code of the voucher
 * @param salesPoint the sales point of the voucher
 * @param voucherNumber the number of the voucher (or 0 if requesting next)
 * @param issueDate the issue date of the voucher
 * @param docType the document type of the receiver
 * @param docNumber the document number of the receiver
 * @param receiverVatCondition the VAT condition code of the receiver
 * @param taxableAmount the taxable amount (importeGravado)
 * @param nonTaxableAmount the non-taxable amount (importeNoGravado)
 * @param exemptAmount the exempt amount (importeExento)
 * @param subtotalAmount the subtotal amount (importeSubtotal)
 * @param otherTaxesAmount the other taxes amount (importeOtrosTributos)
 * @param totalAmount the total amount of the voucher (importeTotal)
 * @param currencyId the currency ID (codigoMoneda)
 * @param exchangeRate the exchange rate of the currency (cotizacionMoneda)
 * @param cancellationSameCurrency cancel in same foreign currency option
 * @param comments comments or observations
 * @param concept the concept type code of the voucher
 * @param serviceStartDate the service start date (for services/products)
 * @param serviceEndDate the service end date
 * @param paymentDueDate the payment due date
 * @param items the itemized lines of the voucher
 * @param vatSubtotals the VAT subtotal lines
 * @param otherTaxes other taxes detail lines
 * @param associatedVouchers associated vouchers list
 * @author fr4ncisx
 * @since 0.7.0
 */
public record WsmtxcaVoucherRequest(
    short voucherType,
    int salesPoint,
    long voucherNumber,
    @Nullable LocalDate issueDate,
    @Nullable Short docType,
    @Nullable Long docNumber,
    @Nullable Short receiverVatCondition,
    @Nullable BigDecimal taxableAmount,
    @Nullable BigDecimal nonTaxableAmount,
    @Nullable BigDecimal exemptAmount,
    BigDecimal subtotalAmount,
    @Nullable BigDecimal otherTaxesAmount,
    BigDecimal totalAmount,
    String currencyId,
    @Nullable BigDecimal exchangeRate,
    @Nullable String cancellationSameCurrency,
    @Nullable String comments,
    short concept,
    @Nullable LocalDate serviceStartDate,
    @Nullable LocalDate serviceEndDate,
    @Nullable LocalDate paymentDueDate,
    List<ItemDetail> items,
    List<TaxDetail> vatSubtotals,
    List<OtherTaxDetail> otherTaxes,
    List<AssociatedVoucher> associatedVouchers
) {}
