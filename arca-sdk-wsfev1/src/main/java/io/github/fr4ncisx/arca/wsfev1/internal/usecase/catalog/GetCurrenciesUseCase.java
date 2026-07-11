package io.github.fr4ncisx.arca.wsfev1.internal.usecase.catalog;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEParamGetTiposMonedas;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.MonedaResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.common.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.catalog.CurrencyInfo;

import java.util.List;

/**
 * Use case to retrieve the catalog of official currencies from ARCA.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public final class GetCurrenciesUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FEParamGetTiposMonedas, MonedaResponse> soapPort;

    /**
     * Creates a new instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     */
    public GetCurrenciesUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FEParamGetTiposMonedas, MonedaResponse> soapPort) {
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
     * Executes the query to retrieve currencies.
     *
     * @return the list of currency details
     */
    public List<CurrencyInfo> execute() {
        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, config.cuit());

        FEParamGetTiposMonedas soapRequest = new FEParamGetTiposMonedas();
        soapRequest.setAuth(auth);

        MonedaResponse soapResponse = soapPort.invoke(soapRequest);
        return CatalogMapper.toCurrencies(soapResponse);
    }
}
