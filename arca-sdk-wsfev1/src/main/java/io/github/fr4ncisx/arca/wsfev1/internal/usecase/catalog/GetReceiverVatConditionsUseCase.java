package io.github.fr4ncisx.arca.wsfev1.internal.usecase.catalog;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.CondicionIvaReceptorResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEParamGetCondicionIvaReceptor;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CatalogMapper;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.catalog.VatConditionInfo;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Use case to retrieve the catalog of official receiver VAT conditions from ARCA.
 *
 * @author fr4ncisx
 * @since 0.6.0
 */
public final class GetReceiverVatConditionsUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FEParamGetCondicionIvaReceptor, CondicionIvaReceptorResponse> soapPort;

    /**
     * Creates a new instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     */
    public GetReceiverVatConditionsUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FEParamGetCondicionIvaReceptor, CondicionIvaReceptorResponse> soapPort) {
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
     * Executes the query to retrieve receiver VAT condition details.
     *
     * @param voucherClass optional voucher class to filter the results (e.g. "A", "B")
     * @return the list of receiver VAT condition details
     */
    public List<VatConditionInfo> execute(@Nullable String voucherClass) {
        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, config.cuit());

        FEParamGetCondicionIvaReceptor soapRequest = new FEParamGetCondicionIvaReceptor();
        soapRequest.setAuth(auth);
        if (voucherClass != null && !voucherClass.trim().isEmpty()) {
            soapRequest.setClaseCmp(voucherClass.trim());
        }

        CondicionIvaReceptorResponse soapResponse = soapPort.invoke(soapRequest);
        return CatalogMapper.toReceiverVatConditions(soapResponse);
    }
}
