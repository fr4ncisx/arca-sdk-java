package io.github.fr4ncisx.arca.wsfev1.model;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;

import java.time.LocalDate;
import java.util.List;

/**
 * Request containing all payload details required to authorize a voucher and obtain a CAE.
 *
 * @param voucherType  the type of voucher
 * @param salesPoint   the issuing sales point
 * @param number       the voucher number
 * @param concept      the billing concept
 * @param customerCuit the taxpayer CUIT of the buyer
 * @param netTaxed     the net taxable amount
 * @param netUntaxed   the net non-taxable amount
 * @param exempted     the exempt amount
 * @param vatTotal     the total VAT tax amount
 * @param total        the total invoice amount
 * @param date         the date of the voucher
 * @param vatLines     the list of VAT breakdowns
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public record CaeRequest(
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
        List<CaeVatLine> vatLines
) {

    public CaeRequest {
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
        if (date == null) {
            throw new ArcaValidationException("date must not be null");
        }
        if (vatLines == null) {
            throw new ArcaValidationException("vatLines list must not be null");
        }
        
        double expectedTotal = netTaxed + netUntaxed + exempted + vatTotal;
        if (Math.abs(total - expectedTotal) > 0.011) {
            throw new ArcaValidationException("Invoice total amount mismatch: expected " 
                    + expectedTotal + " but was " + total);
        }
    }
}
