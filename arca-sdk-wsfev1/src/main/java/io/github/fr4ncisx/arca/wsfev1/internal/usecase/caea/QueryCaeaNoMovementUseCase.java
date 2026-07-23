package io.github.fr4ncisx.arca.wsfev1.internal.usecase.caea;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CaeaMapper;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.caea.CaeaNoMovementQuery;

/**
 * Internal usecase to query if a CAEA has no-movement declarations.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public final class QueryCaeaNoMovementUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FECAEASinMovimientoConsultar, FECAEASinMovConsResponse> soapPort;

    /**
     * Creates a new QueryCaeaNoMovementUseCase instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     */
    public QueryCaeaNoMovementUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FECAEASinMovimientoConsultar, FECAEASinMovConsResponse> soapPort) {
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
     * Executes the use case to consult if a CAEA no-movement declaration exists.
     *
     * @param query the query details
     * @return true if a no-movement declaration exists, false otherwise
     * @throws ArcaValidationException if query is null
     * @throws ArcaSoapException       if network or system errors occur
     */
    public boolean execute(CaeaNoMovementQuery query) {
        if (query == null) {
            throw new ArcaValidationException("query must not be null");
        }
        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, config.cuit());
        FECAEASinMovimientoConsultar soapReq = CaeaMapper.toSoapRequest(auth, query);
        FECAEASinMovConsResponse result = soapPort.invoke(soapReq);

        if (result == null) {
            throw new ArcaSoapException("Received empty response from ARCA SOAP service");
        }

        if (result.getErrors() != null && result.getErrors().getErr() != null && !result.getErrors().getErr().isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("ARCA returned errors querying CAEA no-movement status.");
            for (Err err : result.getErrors().getErr()) {
                errorMsg.append(" [Code: ").append(err.getCode()).append("] ").append(err.getMsg());
            }
            throw new ArcaSoapException(errorMsg.toString());
        }

        if (result.getResultGet() != null && result.getResultGet().getFECAEASinMov() != null) {
            for (FECAEASinMov item : result.getResultGet().getFECAEASinMov()) {
                if (query.caea().equals(item.getCAEA()) && query.salesPoint() == item.getPtoVta()) {
                    return true;
                }
            }
        }

        return false;
    }
}
