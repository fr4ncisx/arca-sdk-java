package io.github.fr4ncisx.arca.wsmtxca.internal.usecase;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarComprobanteRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarComprobanteResponseType;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherConsultRequest;
import io.github.fr4ncisx.arca.wsmtxca.internal.adapter.WsmtxcaMapper;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherConsultResponse;

public final class GetWsmtxcaVoucherUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<ConsultarComprobanteRequestType, ConsultarComprobanteResponseType> soapPort;

    public GetWsmtxcaVoucherUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<ConsultarComprobanteRequestType, ConsultarComprobanteResponseType> soapPort) {
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

    public WsmtxcaVoucherConsultResponse execute(WsmtxcaVoucherConsultRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        ArcaAccessTicket ticket = authProvider.authenticate("wsmtxca");
        ConsultarComprobanteRequestType soapReq = WsmtxcaMapper.toVoucherConsultRequest(ticket, config.cuit(), request);
        ConsultarComprobanteResponseType soapResp = soapPort.invoke(soapReq);
        return WsmtxcaMapper.toVoucherConsultResponse(soapResp);
    }
}
