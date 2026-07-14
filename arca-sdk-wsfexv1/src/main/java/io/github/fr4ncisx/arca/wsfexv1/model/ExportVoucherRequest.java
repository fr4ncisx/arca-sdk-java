package io.github.fr4ncisx.arca.wsfexv1.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Details representing an export electronic voucher to be authorized.
 *
 * @param id                    the unique request identifier (long)
 * @param voucherDate           the voucher date (format: yyyyMMdd)
 * @param voucherType           the voucher type (e.g. 19 for export invoices)
 * @param salesPoint            the sales point number
 * @param voucherNumber         the voucher number
 * @param exportType            the type of export (1: goods, 2: services, 4: other)
 * @param permitExists          whether customs permits exist (S: yes, N: no, empty: default)
 * @param permits               customs permits associated with the voucher
 * @param destinationCountry    the destination country code
 * @param clientName            the client's name or corporate description
 * @param clientCountryCuit     the client's tax identifier in their country (cuit)
 * @param clientAddress         the client's physical address
 * @param clientTaxId           the foreign client tax id
 * @param currencyId            the currency identifier (e.g. "DOL" for USD)
 * @param currencyExchangeRate  the exchange rate of the currency to pesos
 * @param otherCurrencies       indicates if other currencies can be used (S/N)
 * @param commercialObservations observations for commercial use
 * @param totalAmount           the total amount of the invoice
 * @param observations          general observations
 * @param associatedVouchers    any previously linked or associated vouchers
 * @param paymentTerms          payment method/terms details
 * @param incoterms             the Incoterm code (e.g. "FOB")
 * @param incotermsDescription  the Incoterm descriptive text
 * @param language              the invoice print language code (1: ES, 2: EN, 3: PT)
 * @param items                 the item lines list
 * @param optionals             custom optional fields metadata
 * @param paymentDate           expected payment date (yyyyMMdd)
 * @param activities            commercial activities associated
 * @author fr4ncisx
 * @since 0.7.0
 */
public record ExportVoucherRequest(
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
        List<ExportOptionalField> optionals,
        String paymentDate,
        List<ExportActivity> activities
) {

    public ExportVoucherRequest {
        permits = permits == null ? List.of() : List.copyOf(permits);
        associatedVouchers = associatedVouchers == null ? List.of() : List.copyOf(associatedVouchers);
        items = items == null ? List.of() : List.copyOf(items);
        optionals = optionals == null ? List.of() : List.copyOf(optionals);
        activities = activities == null ? List.of() : List.copyOf(activities);
    }
}
