package io.github.fr4ncisx.arca.wsmtxca.spi;

import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaLastVoucherRequest;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaLastVoucherResponse;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherRequest;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherResponse;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherConsultRequest;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherConsultResponse;

/**
 * Public client interface for the ARCA WSMTXCA SOAP service.
 * <p>
 * Exposes electronic invoicing operations for itemized vouchers,
 * decoupling consumer modules from JAX-WS adapters and generated stubs.
 *
 * @author fr4ncisx
 * @since 0.7.0
 */
public interface WsmtxcaClient {

    /**
     * Retrieves the last authorized voucher number for the given sales point and voucher type.
     *
     * @param request the query parameters containing sales point and type
     * @return the last authorized voucher response
     */
    WsmtxcaLastVoucherResponse getLastVoucher(WsmtxcaLastVoucherRequest request);

    /**
     * Requests authorization (CAE) for a single electronic voucher with itemized details.
     *
     * @param request the voucher details to be authorized
     * @return the result containing CAE details or business errors
     */
    WsmtxcaVoucherResponse authorize(WsmtxcaVoucherRequest request);

    /**
     * Queries the complete details and authorization status of a previously authorized voucher.
     *
     * @param request the query parameters containing sales point, type, and number
     * @return the consultation response containing details or errors
     */
    WsmtxcaVoucherConsultResponse getVoucher(WsmtxcaVoucherConsultRequest request);

    /**
     * Executes a fast connectivity check (ping) against ARCA's dummy endpoint.
     *
     * @return true if the ARCA server responds successfully, false if timed out or unreachable
     */
    boolean ping();
}
