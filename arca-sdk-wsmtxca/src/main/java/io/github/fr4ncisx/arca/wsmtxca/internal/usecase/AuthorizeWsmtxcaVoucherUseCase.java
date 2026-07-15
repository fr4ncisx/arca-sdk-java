package io.github.fr4ncisx.arca.wsmtxca.internal.usecase;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.AutorizarComprobanteRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.AutorizarComprobanteResponseType;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherRequest;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherResponse;

public final class AuthorizeWsmtxcaVoucherUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<AutorizarComprobanteRequestType, AutorizarComprobanteResponseType> soapPort;

    public AuthorizeWsmtxcaVoucherUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<AutorizarComprobanteRequestType, AutorizarComprobanteResponseType> soapPort) {
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

    public WsmtxcaVoucherResponse execute(WsmtxcaVoucherRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        ArcaAccessTicket ticket = authProvider.authenticate("wsmtxca");
        AutorizarComprobanteRequestType soapReq = WsmtxcaMapper.toAuthorizeRequest(ticket, config.cuit(), request);
        AutorizarComprobanteResponseType soapResp = soapPort.invoke(soapReq);
        return WsmtxcaMapper.toVoucherResponse(soapResp);
    }
}
