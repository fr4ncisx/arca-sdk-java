package io.github.fr4ncisx.arca.wsfev1.model;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;

import java.time.LocalDate;
import java.util.List;

/**
 * Detailed information of a previously authorized voucher retrieved from ARCA.
 *
 * @param voucherType    the type of voucher
 * @param salesPoint     the sales point number
 * @param number         the voucher number
 * @param concept        the billing concept
 * @param customerCuit   the taxpayer CUIT of the buyer
 * @param netTaxed       the net taxable amount
 * @param netUntaxed     the net non-taxable amount
 * @param exempted       the exempt amount
 * @param vatTotal       the total VAT tax amount
 * @param total          the total invoice amount
 * @param date           the date of the voucher
 * @param cae            the CAE authorization code
 * @param expirationDate the CAE expiration date
 * @param result         the authorization result status
 * @param vatLines       the list of VAT breakdowns
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record VoucherDetail(
        VoucherType voucherType,
        int salesPoint,
        long number,
        ConceptType concept,
        Cuit customerCuit,
        double netTaxed,
        double netUntaxed,
        double exempted,
        double vatTotal,
        double total,
        LocalDate date,
        String cae,
        LocalDate expirationDate,
        String result,
        List<CaeVatLine> vatLines
) {

    public VoucherDetail {
        if (voucherType == null) {
            throw new ArcaValidationException("voucherType must not be null");
        }
        if (salesPoint <= 0) {
            throw new ArcaValidationException("salesPoint must be greater than 0");
        }
        if (number <= 0) {
            throw new ArcaValidationException("number must be greater than 0");
        }
        if (concept == null) {
            throw new ArcaValidationException("concept must not be null");
        }
        if (customerCuit == null) {
            throw new ArcaValidationException("customerCuit must not be null");
        }
        if (cae == null || cae.trim().isEmpty()) {
            throw new ArcaValidationException("cae must not be null or blank");
        }
        if (expirationDate == null) {
            throw new ArcaValidationException("expirationDate must not be null");
        }
        if (result == null || result.trim().isEmpty()) {
            throw new ArcaValidationException("result must not be null or blank");
        }
        if (vatLines == null) {
            throw new ArcaValidationException("vatLines must not be null");
        }
    }
}
