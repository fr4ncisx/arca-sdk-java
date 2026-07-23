package io.github.fr4ncisx.arca.wsfev1.internal.usecase.catalog;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECompTotXRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FERegXReqResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CatalogMapper;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CommonMapper;

/**
 * Use case to retrieve the maximum number of records allowed per batch authorization request by ARCA.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public final class GetMaxRecordsUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FECompTotXRequest, FERegXReqResponse> soapPort;

    /**
     * Creates a new instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     */
    public GetMaxRecordsUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FECompTotXRequest, FERegXReqResponse> soapPort) {
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
     * Executes the query to retrieve the maximum allowed records.
     *
     * @return the maximum number of records as a strictly positive integer
     */
    public int execute() {
        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, config.cuit());

        FECompTotXRequest soapRequest = new FECompTotXRequest();
        soapRequest.setAuth(auth);

        FERegXReqResponse soapResponse = soapPort.invoke(soapRequest);
        return CatalogMapper.toMaxRecords(soapResponse);
    }
}
