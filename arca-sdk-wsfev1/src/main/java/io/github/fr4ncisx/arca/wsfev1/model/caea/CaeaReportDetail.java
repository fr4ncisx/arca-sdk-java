package io.github.fr4ncisx.arca.wsfev1.model.caea;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeVatLine;
import io.github.fr4ncisx.arca.wsfev1.model.common.ConceptType;

import java.time.LocalDate;
import java.util.List;

/**
 * Domain record representing a single electronic voucher detail reported under a CAEA.
 * <p>
 * This contains all individual voucher fields like amounts, customer, tax details, and VAT lines.
 *
 * @param concept      the billing concept type (e.g. products, services)
 * @param customerCuit the customer CUIT identifier
 * @param number       the voucher number
 * @param date         the voucher issuance date
 * @param total        the total amount of the voucher
 * @param netTaxed     the net amount subject to VAT
 * @param exempted     the amount exempted from VAT
 * @param vatTotal     the total VAT amount
 * @param vatLines     the list of individual VAT lines
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record CaeaReportDetail(
        ConceptType concept,
        Cuit customerCuit,
        long number,
        LocalDate date,
        double total,
        double netTaxed,
        double exempted,
        double vatTotal,
        List<CaeVatLine> vatLines
) {

    /**
     * Standard compact constructor validating individual voucher details.
     */
    public CaeaReportDetail {
        if (concept == null) {
            throw new ArcaValidationException("concept must not be null");
        }
        if (customerCuit == null) {
            throw new ArcaValidationException("customerCuit must not be null");
        }
        if (number <= 0) {
            throw new ArcaValidationException("voucher number must be strictly positive");
        }
        if (date == null) {
            throw new ArcaValidationException("date must not be null");
        }
        if (total < 0) {
            throw new ArcaValidationException("total amount cannot be negative");
        }
        vatLines = vatLines != null ? List.copyOf(vatLines) : List.of();
    }
}
