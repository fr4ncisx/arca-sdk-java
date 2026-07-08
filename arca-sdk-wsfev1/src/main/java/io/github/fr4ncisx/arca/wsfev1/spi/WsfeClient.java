package io.github.fr4ncisx.arca.wsfev1.spi;

import io.github.fr4ncisx.arca.wsfev1.model.CaeRequest;
import io.github.fr4ncisx.arca.wsfev1.model.CaeResponse;
import io.github.fr4ncisx.arca.wsfev1.model.LastVoucherRequest;
import io.github.fr4ncisx.arca.wsfev1.model.LastVoucherResponse;

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
}
