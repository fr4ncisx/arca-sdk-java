package io.github.fr4ncisx.arca.wsfev1.internal.usecase.caea;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECAEARegInformativo;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECAEAResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.common.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.caea.CaeaReportRequest;
import io.github.fr4ncisx.arca.wsfev1.model.caea.CaeaReportResponse;

/**
 * Internal usecase to report electronic vouchers issued under a CAEA.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public final class ReportCaeaUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FECAEARegInformativo, FECAEAResponse> soapPort;

    /**
     * Creates a new ReportCaeaUseCase instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     */
    public ReportCaeaUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FECAEARegInformativo, FECAEAResponse> soapPort) {
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
     * Executes the use case to report vouchers under a CAEA.
     *
     * @param request the report details
     * @return the reporting response containing voucher registration outcomes
     * @throws ArcaValidationException if request is null
     */
    public CaeaReportResponse execute(CaeaReportRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, config.cuit());
        FECAEARegInformativo soapReq = CaeaMapper.toSoapRequest(auth, request);
        FECAEAResponse soapResp = soapPort.invoke(soapReq);
        return CaeaMapper.toDomainResponse(soapResp);
    }
}
