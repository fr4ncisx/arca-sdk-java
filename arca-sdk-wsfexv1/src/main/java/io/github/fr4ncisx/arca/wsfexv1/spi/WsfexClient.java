package io.github.fr4ncisx.arca.wsfexv1.spi;

import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherResponse;
import io.github.fr4ncisx.arca.wsfexv1.model.LastExportVoucherRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.LastExportVoucherResponse;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherConsultRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherConsultResponse;

/**
 * Public client interface for the ARCA WSFEXv1 SOAP service.
 * <p>
 * Exposes electronic invoicing operations for export vouchers (type E),
 * decoupling consumer modules from JAX-WS adapters and generated stubs.
 *
 * @author fr4ncisx
 * @since 0.7.0
 */
public interface WsfexClient {

    /**
     * Retrieves the last authorized export voucher number for the given sales point and voucher type.
     *
     * @param request the query parameters containing sales point and type
     * @return the last authorized export voucher response
     */
    LastExportVoucherResponse getLastVoucher(LastExportVoucherRequest request);

    /**
     * Requests authorization (CAE) for a single export electronic voucher (type E).
     *
     * @param request the export voucher details to be authorized
     * @return the result containing CAE details or business errors
     */
    ExportVoucherResponse authorize(ExportVoucherRequest request);

    /**
     * Queries the complete details and authorization status of a previously authorized export voucher.
     *
     * @param request the query parameters containing sales point, type, and number
     * @return the consultation response containing details or errors
     */
    ExportVoucherConsultResponse getVoucher(ExportVoucherConsultRequest request);

    /**
     * Executes a fast connectivity check (ping) against ARCA's dummy endpoint.
     *
     * @return true if the ARCA server responds successfully, false if timed out or unreachable
     */
    boolean ping();
}
