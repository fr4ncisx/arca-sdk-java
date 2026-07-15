package io.github.fr4ncisx.arca.wscdc.spi;

import io.github.fr4ncisx.arca.wscdc.model.WscdcConstatRequest;
import io.github.fr4ncisx.arca.wscdc.model.WscdcConstatResponse;
import io.github.fr4ncisx.arca.wscdc.model.WscdcDummyResponse;

/**
 * Service port interface for WSCDC voucher constatation web service.
 *
 * @author fr4ncisx
 * @since 0.9.0
 */
public interface WscdcClient {

    /**
     * Performs a health-check (Dummy) check on the WSCDC service.
     *
     * @return true if all WSCDC backend servers are OK, false otherwise
     */
    boolean ping();

    /**
     * Checks WSCDC availability details.
     *
     * @return the dummy response details
     */
    WscdcDummyResponse dummy();

    /**
     * Validates the authenticity of a voucher with ARCA/AFIP.
     *
     * @param request the constatation request details
     * @return the result and logs of the validation
     */
    WscdcConstatResponse checkVoucher(WscdcConstatRequest request);
}
