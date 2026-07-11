package io.github.fr4ncisx.arca.wsfev1.spi;

import io.github.fr4ncisx.arca.wsfev1.model.cae.*;
import io.github.fr4ncisx.arca.wsfev1.model.lastvoucher.*;
import io.github.fr4ncisx.arca.wsfev1.model.voucher.*;
import io.github.fr4ncisx.arca.wsfev1.model.salespoint.*;
import io.github.fr4ncisx.arca.wsfev1.model.batch.*;
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
}
