package io.github.fr4ncisx.arca.wsfexv1.internal.usecase;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXAuthRequest;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXRequest;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXAuthorize;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXResponseAuthorize;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherResponse;

public final class AuthorizeExportVoucherUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FEXAuthorize, FEXResponseAuthorize> soapPort;

    public AuthorizeExportVoucherUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FEXAuthorize, FEXResponseAuthorize> soapPort) {
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

    public ExportVoucherResponse execute(ExportVoucherRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        ArcaAccessTicket ticket = authProvider.authenticate("wsfex");
        ClsFEXAuthRequest auth = WsfexMapper.toAuthRequest(ticket, config.cuit());
        ClsFEXRequest cmp = WsfexMapper.toFexRequest(request);

        FEXAuthorize soapReq = new FEXAuthorize();
        soapReq.setAuth(auth);
        soapReq.setCmp(cmp);

        FEXResponseAuthorize soapResp = soapPort.invoke(soapReq);
        return WsfexMapper.toExportVoucherResponse(soapResp);
    }
}
