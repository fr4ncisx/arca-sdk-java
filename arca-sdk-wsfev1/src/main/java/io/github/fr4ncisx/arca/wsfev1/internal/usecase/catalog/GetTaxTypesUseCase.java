package io.github.fr4ncisx.arca.wsfev1.internal.usecase.catalog;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEParamGetTiposTributos;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FETributoResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CatalogMapper;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.catalog.TaxTypeInfo;

import java.util.List;

/**
 * Use case to retrieve the catalog of official secondary tax types from ARCA.
 *
 * @author fr4ncisx
 * @since 0.6.0
 */
public final class GetTaxTypesUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FEParamGetTiposTributos, FETributoResponse> soapPort;

    /**
     * Creates a new instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     */
    public GetTaxTypesUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FEParamGetTiposTributos, FETributoResponse> soapPort) {
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
     * Executes the query to retrieve secondary tax type details.
     *
     * @return the list of tax type details
     */
    public List<TaxTypeInfo> execute() {
        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, config.cuit());

        FEParamGetTiposTributos soapRequest = new FEParamGetTiposTributos();
        soapRequest.setAuth(auth);

        FETributoResponse soapResponse = soapPort.invoke(soapRequest);
        return CatalogMapper.toTaxTypes(soapResponse);
    }
}
