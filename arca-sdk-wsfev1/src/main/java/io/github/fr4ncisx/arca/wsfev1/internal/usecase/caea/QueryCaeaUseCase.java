package io.github.fr4ncisx.arca.wsfev1.internal.usecase.caea;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECAEAGetResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECAEAConsultar;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CaeaMapper;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.caea.CaeaQuery;
import io.github.fr4ncisx.arca.wsfev1.model.caea.CaeaResponse;

/**
 * Internal usecase to query details of a CAEA code from ARCA.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public final class QueryCaeaUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FECAEAConsultar, FECAEAGetResponse> soapPort;

    /**
     * Creates a new QueryCaeaUseCase instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     */
    public QueryCaeaUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FECAEAConsultar, FECAEAGetResponse> soapPort) {
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
     * Executes the use case to query CAEA details.
     *
     * @param query the query parameters
     * @return the CAEA details
     * @throws ArcaValidationException if query is null, or if the CAEA does not exist
     */
    public CaeaResponse execute(CaeaQuery query) {
        if (query == null) {
            throw new ArcaValidationException("query must not be null");
        }
        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, config.cuit());
        FECAEAConsultar soapReq = CaeaMapper.toSoapRequest(auth, query);
        FECAEAGetResponse soapResp = soapPort.invoke(soapReq);
        CaeaResponse response = CaeaMapper.toDomainResponse(soapResp);
        if (response.caea() == null || response.caea().strip().isEmpty()) {
            throw new ArcaValidationException("The requested CAEA code does not exist in ARCA for period "
                    + query.period() + " and order " + query.order());
        }
        return response;
    }
}
