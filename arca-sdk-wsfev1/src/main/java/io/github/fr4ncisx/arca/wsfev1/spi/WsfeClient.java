package io.github.fr4ncisx.arca.wsfev1.spi;

import io.github.fr4ncisx.arca.wsfev1.model.*;

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
}
