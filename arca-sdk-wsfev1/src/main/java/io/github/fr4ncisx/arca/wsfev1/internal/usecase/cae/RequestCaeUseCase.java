package io.github.fr4ncisx.arca.wsfev1.internal.usecase.cae;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECAEResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECAESolicitar;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CaeMapper;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeRequest;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeResponse;

/**
 * Internal use case to request a voucher authorization (CAE) from ARCA.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public final class RequestCaeUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FECAESolicitar, FECAEResponse> soapPort;

    /**
     * Creates a new use case instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     */
    public RequestCaeUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FECAESolicitar, FECAEResponse> soapPort) {
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
     * Executes the use case to obtain a CAE for the given voucher details.
     *
     * @param request the public CAE request details
     * @return the authorization response containing CAE or business errors
     */
    public CaeResponse execute(CaeRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, config.cuit());
        FECAESolicitar soapReq = CaeMapper.toSoapRequest(auth, request);
        FECAEResponse soapResp = soapPort.invoke(soapReq);
        return CaeMapper.toDomainResponse(soapResp);
    }
}
