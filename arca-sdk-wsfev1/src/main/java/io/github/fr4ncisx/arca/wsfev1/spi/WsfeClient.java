package io.github.fr4ncisx.arca.wsfev1.spi;

import io.github.fr4ncisx.arca.wsfev1.model.cae.*;
import io.github.fr4ncisx.arca.wsfev1.model.lastvoucher.*;
import io.github.fr4ncisx.arca.wsfev1.model.voucher.*;
import io.github.fr4ncisx.arca.wsfev1.model.salespoint.*;
import io.github.fr4ncisx.arca.wsfev1.model.batch.*;
import io.github.fr4ncisx.arca.wsfev1.model.caea.*;
import io.github.fr4ncisx.arca.wsfev1.model.catalog.*;

import java.util.List;

/**
 * Public client interface for the ARCA WSFEv1 SOAP service.
 * <p>
 * This service contract exposes core electronic invoicing operations,
 * decoupling consumer modules from remote JAX-WS transport adapters and
 * JAXB mapping structures.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public interface WsfeClient {

    /**
     * Retrieves the last authorized voucher number for the given sales point and voucher type.
     *
     * @param request the query parameters containing sales point and type
     * @return the last authorized voucher response
     */
    LastVoucherResponse getLastVoucher(LastVoucherRequest request);

    /**
     * Requests authorization (CAE) for a single electronic voucher.
     *
     * @param request the voucher details to be authorized
     * @return the result containing CAE details or business errors
     */
    CaeResponse requestCae(CaeRequest request);

    /**
     * Executes a fast connectivity check (ping) against ARCA's dummy endpoint.
     *
     * @return true if the ARCA server responds successfully, false if timed out or unreachable
     */
    boolean ping();

    /**
     * Retrieves the list of authorized sales points registered for the company.
     *
     * @return the list of sales points
     */
    List<SalesPoint> getSalesPoints();

    /**
     * Queries the complete details and authorization status of a previously authorized voucher.
     *
     * @param request the query parameters containing sales point, type, and number
     * @return the consultation response containing details or errors
     */
    VoucherConsultResponse getVoucher(VoucherConsultRequest request);

    /**
     * Orchestrates batch invoice authorization requests applying concurrent or fail-fast strategies.
     *
     * @param request the batch request configuration
     * @return the processed results response
     */
    BatchResponse processBatch(BatchRequest request);

    /**
     * Retrieves the official catalog of voucher types from ARCA.
     *
     * @return the list of voucher types details
     */
    List<VoucherTypeDetail> getVoucherTypes();

    /**
     * Retrieves the official catalog of buyer document types from ARCA.
     *
     * @return the list of document types info
     */
    List<DocumentTypeInfo> getDocumentTypes();

    /**
     * Retrieves the official catalog of VAT tax rate categories from ARCA.
     *
     * @return the list of VAT type info
     */
    List<VatTypeInfo> getVatTypes();

    /**
     * Retrieves the official catalog of currencies from ARCA.
     *
     * @return the list of currencies info
     */
    List<CurrencyInfo> getCurrencies();

    /**
     * Retrieves the current exchange rate for a given currency compared to Argentine Pesos (ARS).
     *
     * @param currencyId the official currency identifier (e.g. "DOL")
     * @return the exchange rate information
     */
    ExchangeRate getExchangeRate(String currencyId);

    /**
     * Retrieves the maximum number of voucher records allowed in a single batch request by ARCA.
     *
     * @return the maximum number of records
     */
    int getMaxRecordsPerRequest();

    /**
     * Retrieves the official catalog of concept types from ARCA.
     *
     * @return the list of concept types info
     */
    List<ConceptTypeInfo> getConceptTypes();

    /**
     * Retrieves the official catalog of optional fields from ARCA.
     *
     * @return the list of optional field types info
     */
    List<OptionalFieldTypeInfo> getOptionalFieldTypes();

    /**
     * Retrieves the official catalog of countries from ARCA.
     *
     * @return the list of countries info
     */
    List<CountryInfo> getCountries();

    /**
     * Retrieves the official catalog of other tax types from ARCA.
     *
     * @return the list of tax types info
     */
    List<TaxTypeInfo> getTaxTypes();

    /**
     * Retrieves the official catalog of commercial activities from ARCA.
     *
     * @return the list of activities info
     */
    List<ActivityInfo> getActivities();

    /**
     * Retrieves the official catalog of receiver VAT conditions from ARCA.
     *
     * @param voucherClass optional voucher class to filter the results (e.g. "A", "B"), can be null or blank
     * @return the list of receiver VAT conditions info
     */
    List<VatConditionInfo> getReceiverVatConditions(@org.jspecify.annotations.Nullable String voucherClass);

    /**
     * Requests a new Anticipated Electronic Authorization code (CAEA) from ARCA.
     *
     * @param request the CAEA request details
     * @return the CAEA response containing details or errors
     */
    CaeaResponse requestCaea(CaeaRequest request);

    /**
     * Reports electronic vouchers issued under a previously authorized CAEA.
     *
     * @param request the batch details to report
     * @return the reporting response containing outcomes for each voucher
     */
    CaeaReportResponse reportCaea(CaeaReportRequest request);

    /**
     * Queries details of a previously assigned CAEA code.
     *
     * @param query the query details
     * @return the CAEA details response
     */
    CaeaResponse queryCaea(CaeaQuery query);

    /**
     * Informs ARCA that a sales point has no movements under a CAEA code.
     *
     * @param request the request details
     */
    void reportCaeaNoMovement(CaeaNoMovementRequest request);

    /**
     * Consults if a sales point has registered a no-movement declaration for a CAEA.
     *
     * @param query the query details
     * @return true if a no-movement declaration exists, false otherwise
     */
    boolean queryCaeaNoMovement(CaeaNoMovementQuery query);
}
