package io.github.fr4ncisx.arca.wsfev1.internal.usecase.catalog;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECotizacionResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEParamGetCotizacion;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CatalogMapper;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.catalog.ExchangeRate;

/**
 * Use case to retrieve the exchange rate for a given currency compared to Argentine Pesos (ARS).
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public final class GetExchangeRateUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FEParamGetCotizacion, FECotizacionResponse> soapPort;

    /**
     * Creates a new instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     */
    public GetExchangeRateUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FEParamGetCotizacion, FECotizacionResponse> soapPort) {
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
     * Executes the query to retrieve the exchange rate for the specified currency.
     *
     * @param currencyId the official currency identifier (e.g. "DOL")
     * @return the exchange rate information
     * @throws ArcaValidationException if currencyId is null or blank, or not found on ARCA
     */
    public ExchangeRate execute(String currencyId) {
        if (currencyId == null || currencyId.trim().isEmpty()) {
            throw new ArcaValidationException("currencyId must not be null or blank");
        }

        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, config.cuit());

        FEParamGetCotizacion soapRequest = new FEParamGetCotizacion();
        soapRequest.setAuth(auth);
        soapRequest.setMonId(currencyId.trim());

        FECotizacionResponse soapResponse = soapPort.invoke(soapRequest);
        return CatalogMapper.toExchangeRate(soapResponse);
    }
}
