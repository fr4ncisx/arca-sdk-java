package io.github.fr4ncisx.arca.wscdc.internal.assembler;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.internal.adapter.ArcaSoapClient;
import io.github.fr4ncisx.arca.soap.internal.config.SoapConfig;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wscdc.internal.client.DefaultWscdcClient;
import io.github.fr4ncisx.arca.wscdc.internal.client.WscdcDummyClient;
import io.github.fr4ncisx.arca.wscdc.internal.generated.CmpResponse;
import io.github.fr4ncisx.arca.wscdc.internal.generated.Service;
import io.github.fr4ncisx.arca.wscdc.internal.generated.ServiceSoap;
import io.github.fr4ncisx.arca.wscdc.internal.usecase.ConstatVoucherUseCase;
import io.github.fr4ncisx.arca.wscdc.internal.usecase.WscdcRequestWrapper;
import io.github.fr4ncisx.arca.wscdc.spi.WscdcClient;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;

/**
 * Assembler to configure and build instances of the WscdcClient facade.
 *
 * @author fr4ncisx
 * @since 0.9.0
 */
public final class WscdcClientAssembler {

    private WscdcClientAssembler() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Assembles a new WscdcClient using parameters from the ArcaConfig.
     *
     * @param config the client configuration
     * @param authProvider the authentication provider
     * @return the assembled WscdcClient instance
     */
    public static WscdcClient assemble(ArcaConfig config, AuthProvider authProvider) {
        if (config == null) {
            throw new ArcaValidationException("config must not be null");
        }
        return assemble(config, authProvider, config.environment().getWscdcUrl().toString());
    }

    /**
     * Assembles a new WscdcClient with a custom endpoint URL.
     *
     * @param config the client configuration
     * @param authProvider the authentication provider
     * @param endpointUrl the custom endpoint address
     * @return the assembled WscdcClient instance
     */
    public static WscdcClient assemble(ArcaConfig config, AuthProvider authProvider, String endpointUrl) {
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

        ArcaSoapPort<WscdcRequestWrapper, CmpResponse> constatSoapPort =
                new ArcaSoapClient<>(bp, req -> {
                    try {
                        return port.comprobanteConstatar(req.auth(), req.cmpReq());
                    } catch (Exception e) {
                        throw new WebServiceException(e);
                    }
                }, soapConfig);

        ConstatVoucherUseCase constatUseCase = new ConstatVoucherUseCase(config, authProvider, constatSoapPort);
        WscdcDummyClient dummyClient = new WscdcDummyClient(port, config.environment(), config.readTimeout());

        return new DefaultWscdcClient(dummyClient, constatUseCase);
    }
}
