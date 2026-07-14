package io.github.fr4ncisx.arca.wsfev1.internal.usecase.catalog;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEActividadesResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEParamGetActividades;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.common.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.catalog.ActivityInfo;

import java.util.List;

/**
 * Use case to retrieve the catalog of official commercial activities from ARCA.
 *
 * @author fr4ncisx
 * @since 0.6.0
 */
public final class GetActivitiesUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FEParamGetActividades, FEActividadesResponse> soapPort;

    /**
     * Creates a new instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     */
    public GetActivitiesUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FEParamGetActividades, FEActividadesResponse> soapPort) {
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
     * Executes the query to retrieve commercial activity details.
     *
     * @return the list of activity details
     */
    public List<ActivityInfo> execute() {
        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, config.cuit());

        FEParamGetActividades soapRequest = new FEParamGetActividades();
        soapRequest.setAuth(auth);

        FEActividadesResponse soapResponse = soapPort.invoke(soapRequest);
        return CatalogMapper.toActivities(soapResponse);
    }
}
