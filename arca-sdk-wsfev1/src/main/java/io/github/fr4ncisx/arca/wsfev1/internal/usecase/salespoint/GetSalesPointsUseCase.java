package io.github.fr4ncisx.arca.wsfev1.internal.usecase.salespoint;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEParamGetPtosVenta;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEPtoVentaResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.common.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.salespoint.SalesPoint;

import java.util.List;

/**
 * Use case to retrieve the list of authorized sales points registered in ARCA.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public final class GetSalesPointsUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FEParamGetPtosVenta, FEPtoVentaResponse> soapPort;

    /**
     * Creates a new GetSalesPointsUseCase instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     * @throws ArcaValidationException if any parameter is null
     */
    public GetSalesPointsUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FEParamGetPtosVenta, FEPtoVentaResponse> soapPort
    ) {
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

    /**
     * Executes the consult to retrieve the list of sales points.
     *
     * @return the list of authorized sales points
     */
    public List<SalesPoint> execute() {
        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, config.cuit());

        FEParamGetPtosVenta soapRequest = new FEParamGetPtosVenta();
        soapRequest.setAuth(auth);

        FEPtoVentaResponse soapResponse = soapPort.invoke(soapRequest);
        return SalesPointMapper.toDomainResponse(soapResponse);
    }
}
