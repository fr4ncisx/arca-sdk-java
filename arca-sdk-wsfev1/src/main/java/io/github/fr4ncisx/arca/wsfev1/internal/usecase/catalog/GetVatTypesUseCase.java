package io.github.fr4ncisx.arca.wsfev1.internal.usecase.catalog;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEParamGetTiposIva;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.IvaTipoResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.common.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.catalog.VatTypeInfo;

import java.util.List;

/**
 * Use case to retrieve the catalog of official VAT rate types from ARCA.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public final class GetVatTypesUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FEParamGetTiposIva, IvaTipoResponse> soapPort;

    /**
     * Creates a new instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     */
    public GetVatTypesUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FEParamGetTiposIva, IvaTipoResponse> soapPort) {
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
     * Executes the query to retrieve VAT rate types.
     *
     * @return the list of VAT type details
     */
    public List<VatTypeInfo> execute() {
        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, config.cuit());

        FEParamGetTiposIva soapRequest = new FEParamGetTiposIva();
        soapRequest.setAuth(auth);

        IvaTipoResponse soapResponse = soapPort.invoke(soapRequest);
        return CatalogMapper.toVatTypes(soapResponse);
    }
}
