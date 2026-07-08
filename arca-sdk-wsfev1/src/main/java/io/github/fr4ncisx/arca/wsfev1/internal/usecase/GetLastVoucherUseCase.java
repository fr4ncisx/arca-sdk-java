package io.github.fr4ncisx.arca.wsfev1.internal.usecase;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECompUltimoAutorizado;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FERecuperaLastCbteResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.mapper.WsfeRequestMapper;
import io.github.fr4ncisx.arca.wsfev1.internal.mapper.WsfeResponseMapper;
import io.github.fr4ncisx.arca.wsfev1.model.LastVoucherRequest;
import io.github.fr4ncisx.arca.wsfev1.model.LastVoucherResponse;

/**
 * Internal use case to query the last authorized voucher number from ARCA.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public final class GetLastVoucherUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FECompUltimoAutorizado, FERecuperaLastCbteResponse> soapPort;

    /**
     * Creates a new use case instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     */
    public GetLastVoucherUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FECompUltimoAutorizado, FERecuperaLastCbteResponse> soapPort) {
        if (config == null) {
            throw new ArcaValidationException("config must not be null");
        }
        if (authProvider == null) {
            throw new ArcaValidationException("authProvider must not be null");
        }
        if (soapPort == null) {
            throw new ArcaValidationException("soapPort must not be null");
        }
        this.config = config;
        this.authProvider = authProvider;
        this.soapPort = soapPort;
    }

    /**
     * Executes the use case to fetch the last authorized voucher number.
     *
     * @param request the public query parameters
     * @return the query result from ARCA
     */
    public LastVoucherResponse execute(LastVoucherRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = WsfeRequestMapper.toAuthRequest(ticket, config.cuit());
        FECompUltimoAutorizado soapReq = WsfeRequestMapper.toSoapRequest(auth, request);
        FERecuperaLastCbteResponse soapResp = soapPort.invoke(soapReq);
        return WsfeResponseMapper.toDomainResponse(soapResp, request);
    }
}
