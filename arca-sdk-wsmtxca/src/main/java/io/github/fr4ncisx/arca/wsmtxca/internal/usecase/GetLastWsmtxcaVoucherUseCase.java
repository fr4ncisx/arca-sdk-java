package io.github.fr4ncisx.arca.wsmtxca.internal.usecase;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarUltimoComprobanteAutorizadoRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarUltimoComprobanteAutorizadoResponseType;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaLastVoucherRequest;
import io.github.fr4ncisx.arca.wsmtxca.internal.adapter.WsmtxcaMapper;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaLastVoucherResponse;

public final class GetLastWsmtxcaVoucherUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<ConsultarUltimoComprobanteAutorizadoRequestType, ConsultarUltimoComprobanteAutorizadoResponseType> soapPort;

    public GetLastWsmtxcaVoucherUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<ConsultarUltimoComprobanteAutorizadoRequestType, ConsultarUltimoComprobanteAutorizadoResponseType> soapPort) {
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

    public WsmtxcaLastVoucherResponse execute(WsmtxcaLastVoucherRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        ArcaAccessTicket ticket = authProvider.authenticate("wsmtxca");
        ConsultarUltimoComprobanteAutorizadoRequestType soapReq = WsmtxcaMapper.toLastVoucherRequest(ticket, config.cuit(), request);
        ConsultarUltimoComprobanteAutorizadoResponseType soapResp = soapPort.invoke(soapReq);
        WsmtxcaLastVoucherResponse mapped = WsmtxcaMapper.toLastVoucherResponse(soapResp);
        return new WsmtxcaLastVoucherResponse(
                request.salesPoint(),
                request.voucherType(),
                mapped.lastVoucherNumber(),
                mapped.errors()
        );
    }
}
