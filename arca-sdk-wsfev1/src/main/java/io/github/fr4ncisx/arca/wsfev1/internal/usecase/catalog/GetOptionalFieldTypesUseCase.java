package io.github.fr4ncisx.arca.wsfev1.internal.usecase.catalog;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEParamGetTiposOpcional;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.OpcionalTipoResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CatalogMapper;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.catalog.OptionalFieldTypeInfo;

import java.util.List;

/**
 * Use case to retrieve the catalog of official optional fields from ARCA.
 *
 * @author fr4ncisx
 * @since 0.6.0
 */
public final class GetOptionalFieldTypesUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FEParamGetTiposOpcional, OpcionalTipoResponse> soapPort;

    /**
     * Creates a new instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     */
    public GetOptionalFieldTypesUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FEParamGetTiposOpcional, OpcionalTipoResponse> soapPort) {
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
     * Executes the query to retrieve optional field types.
     *
     * @return the list of optional field details
     */
    public List<OptionalFieldTypeInfo> execute() {
        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, config.cuit());

        FEParamGetTiposOpcional soapRequest = new FEParamGetTiposOpcional();
        soapRequest.setAuth(auth);

        OpcionalTipoResponse soapResponse = soapPort.invoke(soapRequest);
        return CatalogMapper.toOptionalFieldTypes(soapResponse);
    }
}
