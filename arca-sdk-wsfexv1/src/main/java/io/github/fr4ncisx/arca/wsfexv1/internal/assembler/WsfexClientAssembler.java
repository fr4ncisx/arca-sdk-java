package io.github.fr4ncisx.arca.wsfexv1.internal.assembler;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.internal.adapter.ArcaSoapClient;
import io.github.fr4ncisx.arca.soap.internal.config.SoapConfig;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsfexv1.internal.client.DefaultWsfexClient;
import io.github.fr4ncisx.arca.wsfexv1.internal.client.FexdummyClient;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXLastCMP;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXAuthorize;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXGetCMP;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXGetCMPResponseDataType;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXResponseAuthorize;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXResponseLastCMP;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.Service;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ServiceSoap;
import io.github.fr4ncisx.arca.wsfexv1.internal.usecase.AuthorizeExportVoucherUseCase;
import io.github.fr4ncisx.arca.wsfexv1.internal.usecase.GetExportVoucherUseCase;
import io.github.fr4ncisx.arca.wsfexv1.internal.usecase.GetLastExportVoucherUseCase;
import io.github.fr4ncisx.arca.wsfexv1.spi.WsfexClient;
import jakarta.xml.ws.BindingProvider;

public final class WsfexClientAssembler {

    private WsfexClientAssembler() {
    }

    public static WsfexClient assemble(ArcaConfig config, AuthProvider authProvider) {
        if (config == null) {
            throw new ArcaValidationException("config must not be null");
        }
        return assemble(config, authProvider, config.environment().getWsfexv1Url().toString());
    }

    public static WsfexClient assemble(ArcaConfig config, AuthProvider authProvider, String endpointUrl) {
        if (config == null) {
            throw new ArcaValidationException("config must not be null");
        }
        if (authProvider == null) {
            throw new ArcaValidationException("authProvider must not be null");
        }
        if (endpointUrl == null || endpointUrl.trim().isEmpty()) {
            throw new ArcaValidationException("endpointUrl must not be null or blank");
        }

        SoapConfig soapConfig = SoapConfig.from(config);
        ServiceSoap port = new Service().getServiceSoap();

        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

        ArcaSoapPort<ClsFEXLastCMP, FEXResponseLastCMP> lastVoucherSoapPort =
                new ArcaSoapClient<>(bp, port::fexGetLastCMP, soapConfig);

        ArcaSoapPort<FEXAuthorize, FEXResponseAuthorize> authorizeSoapPort =
                new ArcaSoapClient<>(bp, req -> port.fexAuthorize(req.getAuth(), req.getCmp()), soapConfig);

        ArcaSoapPort<FEXGetCMP, FEXGetCMPResponseDataType> getVoucherSoapPort =
                new ArcaSoapClient<>(bp, req -> port.fexGetCMP(req.getAuth(), req.getCmp()), soapConfig);

        GetLastExportVoucherUseCase getLastExportVoucherUseCase =
                new GetLastExportVoucherUseCase(config, authProvider, lastVoucherSoapPort);

        AuthorizeExportVoucherUseCase authorizeExportVoucherUseCase =
                new AuthorizeExportVoucherUseCase(config, authProvider, authorizeSoapPort);

        GetExportVoucherUseCase getExportVoucherUseCase =
                new GetExportVoucherUseCase(config, authProvider, getVoucherSoapPort);

        FexdummyClient fexdummyClient = new FexdummyClient(port);

        return new DefaultWsfexClient(
                config,
                getLastExportVoucherUseCase,
                authorizeExportVoucherUseCase,
                getExportVoucherUseCase,
                fexdummyClient
        );
    }
}
