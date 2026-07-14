package io.github.fr4ncisx.arca.wsfexv1.internal.usecase;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXLastCMP;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXResponseLastCMP;
import io.github.fr4ncisx.arca.wsfexv1.model.LastExportVoucherRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.LastExportVoucherResponse;

public final class GetLastExportVoucherUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<ClsFEXLastCMP, FEXResponseLastCMP> soapPort;

    public GetLastExportVoucherUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<ClsFEXLastCMP, FEXResponseLastCMP> soapPort) {
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

    public LastExportVoucherResponse execute(LastExportVoucherRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        ArcaAccessTicket ticket = authProvider.authenticate("wsfex");
        ClsFEXLastCMP soapReq = WsfexMapper.toLastCmpRequest(ticket, config.cuit(), request);
        FEXResponseLastCMP soapResp = soapPort.invoke(soapReq);
        return WsfexMapper.toLastExportVoucherResponse(soapResp);
    }
}
