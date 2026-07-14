package io.github.fr4ncisx.arca.wsfexv1.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detailed representation of an authorized export voucher.
 *
 * @param id                    the request identifier
 * @param voucherDate           the voucher date (yyyyMMdd)
 * @param voucherType           the voucher type code (e.g. 19)
 * @param salesPoint            the sales point number
 * @param voucherNumber         the voucher number
 * @param exportType            the export type code
 * @param permitExists          whether customs permits exist (S/N)
 * @param permits               customs permits associated
 * @param destinationCountry    the destination country code
 * @param clientName            the client's name or company description
 * @param clientCountryCuit     the client's tax identifier
 * @param clientAddress         the client's address
 * @param clientTaxId           the foreign client tax id
 * @param currencyId            the currency identifier (e.g. "DOL")
 * @param currencyExchangeRate  the exchange rate of the currency to pesos
 * @param otherCurrencies       indicates if other currencies can be used (S/N)
 * @param commercialObservations observations for commercial use
 * @param totalAmount           the total amount of the invoice
 * @param observations          general observations
 * @param associatedVouchers    associated vouchers
 * @param paymentTerms          payment method/terms details
 * @param incoterms             the Incoterm code (e.g. "FOB")
 * @param incotermsDescription  the Incoterm description
 * @param language              the language code
 * @param items                 item lines list
 * @param cae                   the authorized CAE code
 * @param caeExpirationDate     the expiration date of the CAE code (yyyyMMdd)
 * @param caeDate               the date when CAE was generated (yyyyMMdd)
 * @param status                the processing status (e.g. "A" for approved, "R" for rejected)
 * @param reasons               the observations or reasons for status from ARCA
 * @param optionals             custom optional fields metadata
 * @param paymentDate           expected payment date (yyyyMMdd)
 * @param activities            commercial activities associated
 * @author fr4ncisx
 * @since 0.7.0
 */
public record ExportVoucherDetail(
        long id,
        String voucherDate,
        short voucherType,
        int salesPoint,
        long voucherNumber,
        short exportType,
        String permitExists,
        List<ExportPermit> permits,
        short destinationCountry,
        String clientName,
        long clientCountryCuit,
        String clientAddress,
        String clientTaxId,
        String currencyId,
        BigDecimal currencyExchangeRate,
        String otherCurrencies,
        String commercialObservations,
        BigDecimal totalAmount,
        String observations,
        List<AssociatedVoucher> associatedVouchers,
        String paymentTerms,
        String incoterms,
        String incotermsDescription,
        short language,
        List<ExportItem> items,
        String cae,
        String caeExpirationDate,
        String caeDate,
        String status,
        String reasons,
        List<ExportOptionalField> optionals,
        String paymentDate,
        List<ExportActivity> activities
) {

    public ExportVoucherDetail {
        permits = permits == null ? List.of() : List.copyOf(permits);
        associatedVouchers = associatedVouchers == null ? List.of() : List.copyOf(associatedVouchers);
        items = items == null ? List.of() : List.copyOf(items);
        optionals = optionals == null ? List.of() : List.copyOf(optionals);
        activities = activities == null ? List.of() : List.copyOf(activities);
    }
}
