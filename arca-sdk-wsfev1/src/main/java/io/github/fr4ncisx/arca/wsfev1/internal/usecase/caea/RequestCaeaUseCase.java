package io.github.fr4ncisx.arca.wsfev1.internal.usecase.caea;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECAEAGetResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECAEASolicitar;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CaeaMapper;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.caea.CaeaRequest;
import io.github.fr4ncisx.arca.wsfev1.model.caea.CaeaResponse;

/**
 * Internal usecase to request a new CAEA code from ARCA.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public final class RequestCaeaUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FECAEASolicitar, FECAEAGetResponse> soapPort;

    /**
     * Creates a new RequestCaeaUseCase instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     */
    public RequestCaeaUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FECAEASolicitar, FECAEAGetResponse> soapPort) {
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
     * Executes the use case to request a CAEA code.
     *
     * @param request the request details
     * @return the CAEA response containing details or errors
     * @throws ArcaValidationException if request is null
     */
    public CaeaResponse execute(CaeaRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, config.cuit());
        FECAEASolicitar soapReq = CaeaMapper.toSoapRequest(auth, request);
        FECAEAGetResponse soapResp = soapPort.invoke(soapReq);
        return CaeaMapper.toDomainResponse(soapResp);
    }
}
