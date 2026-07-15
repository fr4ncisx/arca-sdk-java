package io.github.fr4ncisx.arca.wsmtxca.internal.assembler;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.internal.adapter.ArcaSoapClient;
import io.github.fr4ncisx.arca.soap.internal.config.SoapConfig;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsmtxca.internal.client.DefaultWsmtxcaClient;
import io.github.fr4ncisx.arca.wsmtxca.internal.client.WsmtxcaDummyClient;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.AutorizarComprobanteRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.AutorizarComprobanteResponseType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarComprobanteRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarComprobanteResponseType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarUltimoComprobanteAutorizadoRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarUltimoComprobanteAutorizadoResponseType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.MTXCAService;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.MTXCAServicePortType;
import io.github.fr4ncisx.arca.wsmtxca.internal.usecase.AuthorizeWsmtxcaVoucherUseCase;
import io.github.fr4ncisx.arca.wsmtxca.internal.usecase.GetLastWsmtxcaVoucherUseCase;
import io.github.fr4ncisx.arca.wsmtxca.internal.usecase.GetWsmtxcaVoucherUseCase;
import io.github.fr4ncisx.arca.wsmtxca.spi.WsmtxcaClient;
import jakarta.xml.ws.BindingProvider;

public final class WsmtxcaClientAssembler {

    private WsmtxcaClientAssembler() {
    }

    public static WsmtxcaClient assemble(ArcaConfig config, AuthProvider authProvider) {
        if (config == null) {
            throw new ArcaValidationException("config must not be null");
        }
        return assemble(config, authProvider, config.environment().getWsmtxcaUrl().toString());
    }

    public static WsmtxcaClient assemble(ArcaConfig config, AuthProvider authProvider, String endpointUrl) {
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
        MTXCAServicePortType port = new MTXCAService().getMTXCAServiceHttpSoap11Endpoint();

        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

        ArcaSoapPort<ConsultarUltimoComprobanteAutorizadoRequestType, ConsultarUltimoComprobanteAutorizadoResponseType> lastVoucherSoapPort =
                new ArcaSoapClient<>(bp, req -> {
                    try {
                        return port.consultarUltimoComprobanteAutorizado(req);
                    } catch (Exception e) {
                        throw new jakarta.xml.ws.WebServiceException(e);
                    }
                }, soapConfig);

        ArcaSoapPort<AutorizarComprobanteRequestType, AutorizarComprobanteResponseType> authorizeSoapPort =
                new ArcaSoapClient<>(bp, req -> {
                    try {
                        return port.autorizarComprobante(req);
                    } catch (Exception e) {
                        throw new jakarta.xml.ws.WebServiceException(e);
                    }
                }, soapConfig);

        ArcaSoapPort<ConsultarComprobanteRequestType, ConsultarComprobanteResponseType> getVoucherSoapPort =
                new ArcaSoapClient<>(bp, req -> {
                    try {
                        return port.consultarComprobante(req);
                    } catch (Exception e) {
                        throw new jakarta.xml.ws.WebServiceException(e);
                    }
                }, soapConfig);

        GetLastWsmtxcaVoucherUseCase getLastWsmtxcaVoucherUseCase =
                new GetLastWsmtxcaVoucherUseCase(config, authProvider, lastVoucherSoapPort);

        AuthorizeWsmtxcaVoucherUseCase authorizeWsmtxcaVoucherUseCase =
                new AuthorizeWsmtxcaVoucherUseCase(config, authProvider, authorizeSoapPort);

        GetWsmtxcaVoucherUseCase getWsmtxcaVoucherUseCase =
                new GetWsmtxcaVoucherUseCase(config, authProvider, getVoucherSoapPort);

        WsmtxcaDummyClient wsmtxcaDummyClient = new WsmtxcaDummyClient(port);

        return new DefaultWsmtxcaClient(
                config,
                getLastWsmtxcaVoucherUseCase,
                authorizeWsmtxcaVoucherUseCase,
                getWsmtxcaVoucherUseCase,
                wsmtxcaDummyClient
        );
    }
}
