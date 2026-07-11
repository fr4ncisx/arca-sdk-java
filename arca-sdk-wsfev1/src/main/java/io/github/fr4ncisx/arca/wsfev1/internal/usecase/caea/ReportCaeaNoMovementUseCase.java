package io.github.fr4ncisx.arca.wsfev1.internal.usecase.caea;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.Err;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECAEASinMovimientoInformar;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECAEASinMovResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.common.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.caea.CaeaNoMovementRequest;

/**
 * Internal usecase to inform ARCA that a CAEA code had no movements.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public final class ReportCaeaNoMovementUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FECAEASinMovimientoInformar, FECAEASinMovResponse> soapPort;

    /**
     * Creates a new ReportCaeaNoMovementUseCase instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     */
    public ReportCaeaNoMovementUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FECAEASinMovimientoInformar, FECAEASinMovResponse> soapPort) {
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
     * Executes the use case to report no movements for a CAEA.
     *
     * @param request the request details
     * @throws ArcaValidationException if the request is null or if ARCA reports that commercial movements exist
     * @throws ArcaSoapException       if network or communication errors occur
     */
    public void execute(CaeaNoMovementRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, config.cuit());
        FECAEASinMovimientoInformar soapReq = CaeaMapper.toSoapRequest(auth, request);
        FECAEASinMovResponse result = soapPort.invoke(soapReq);

        if (result == null) {
            throw new ArcaSoapException("Received empty response from ARCA SOAP service");
        }

        if ("R".equals(result.getResultado())) {
            StringBuilder errorMsg = new StringBuilder("ARCA rejected CAEA no-movement report.");
            if (result.getErrors() != null && result.getErrors().getErr() != null) {
                for (Err err : result.getErrors().getErr()) {
                    errorMsg.append(" [Code: ").append(err.getCode()).append("] ").append(err.getMsg());
                }
            }
            throw new ArcaValidationException(errorMsg.toString());
        }
    }
}
