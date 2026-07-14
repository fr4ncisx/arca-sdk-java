package io.github.fr4ncisx.arca.wsfexv1.internal.usecase;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXAuthRequest;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXGetCMP;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXGetCMP;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXGetCMPResponseDataType;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherConsultRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherConsultResponse;

public final class GetExportVoucherUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FEXGetCMP, FEXGetCMPResponseDataType> soapPort;

    public GetExportVoucherUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FEXGetCMP, FEXGetCMPResponseDataType> soapPort) {
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

    public ExportVoucherConsultResponse execute(ExportVoucherConsultRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        ArcaAccessTicket ticket = authProvider.authenticate("wsfex");
        ClsFEXAuthRequest auth = WsfexMapper.toAuthRequest(ticket, config.cuit());
        ClsFEXGetCMP cmp = WsfexMapper.toGetCmpRequest(request);

        FEXGetCMP soapReq = new FEXGetCMP();
        soapReq.setAuth(auth);
        soapReq.setCmp(cmp);

        FEXGetCMPResponseDataType soapResp = soapPort.invoke(soapReq);
        return WsfexMapper.toExportVoucherConsultResponse(soapResp);
    }
}
