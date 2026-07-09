package io.github.fr4ncisx.arca.wsfev1.internal.assembler;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.internal.adapter.ArcaSoapClient;
import io.github.fr4ncisx.arca.soap.internal.config.SoapConfig;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsfev1.internal.client.DefaultWsfeClient;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECompUltimoAutorizado;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FERecuperaLastCbteResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECAESolicitar;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECAEResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECompConsultar;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECompConsultaResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEParamGetPtosVenta;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEPtoVentaResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.ServiceSoap;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.Service;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.*;
import io.github.fr4ncisx.arca.wsfev1.spi.WsfeClient;
import jakarta.xml.ws.BindingProvider;

/**
 * Assembler to construct and wire internal dependencies of the WSFEv1 client.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public final class WsfeClientAssembler {

    private WsfeClientAssembler() {
    }

    /**
     * Assembles a WSFEv1 client using official environment endpoints.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @return the fully wired WsfeClient instance
     */
    public static WsfeClient assemble(ArcaConfig config, AuthProvider authProvider) {
        if (config == null) {
            throw new ArcaValidationException("config must not be null");
        }
        return assemble(config, authProvider, config.environment().getWsfev1Url().toString());
    }

    /**
     * Assembles a WSFEv1 client using a custom endpoint URL.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param endpointUrl  the target WSFEv1 SOAP endpoint URL
     * @return the fully wired WsfeClient instance
     */
    public static WsfeClient assemble(ArcaConfig config, AuthProvider authProvider, String endpointUrl) {
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

        ArcaSoapPort<FECompUltimoAutorizado, FERecuperaLastCbteResponse> lastVoucherSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feCompUltimoAutorizado(req.getAuth(), req.getPtoVta(), req.getCbteTipo()), soapConfig);

        ArcaSoapPort<FECAESolicitar, FECAEResponse> requestCaeSoapPort =
                new ArcaSoapClient<>(bp, req -> port.fecaeSolicitar(req.getAuth(), req.getFeCAEReq()), soapConfig);

        ArcaSoapPort<FECompConsultar, FECompConsultaResponse> getVoucherSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feCompConsultar(req.getAuth(), req.getFeCompConsReq()), soapConfig);

        ArcaSoapPort<FEParamGetPtosVenta, FEPtoVentaResponse> getSalesPointsSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feParamGetPtosVenta(req.getAuth()), soapConfig);

        GetLastVoucherUseCase getLastVoucherUseCase =
                new GetLastVoucherUseCase(config, authProvider, lastVoucherSoapPort);

        RequestCaeUseCase requestCaeUseCase =
                new RequestCaeUseCase(config, authProvider, requestCaeSoapPort);

        GetSalesPointsUseCase getSalesPointsUseCase =
                new GetSalesPointsUseCase(config, authProvider, getSalesPointsSoapPort);

        GetVoucherUseCase getVoucherUseCase =
                new GetVoucherUseCase(config, authProvider, getVoucherSoapPort);

        java.util.concurrent.ExecutorService executorService = java.util.concurrent.Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("arca-wsfev1-batch-thread");
            return t;
        });

        BatchProcessUseCase batchProcessUseCase =
                new BatchProcessUseCase(requestCaeUseCase, executorService);

        io.github.fr4ncisx.arca.wsfev1.internal.client.FedummyClient fedummyClient =
                new io.github.fr4ncisx.arca.wsfev1.internal.client.FedummyClient(port);

        return new DefaultWsfeClient(
                config,
                getLastVoucherUseCase,
                requestCaeUseCase,
                getSalesPointsUseCase,
                getVoucherUseCase,
                batchProcessUseCase,
                fedummyClient
        );
    }
}
