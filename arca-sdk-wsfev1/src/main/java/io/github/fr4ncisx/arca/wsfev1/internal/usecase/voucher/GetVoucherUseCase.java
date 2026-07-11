package io.github.fr4ncisx.arca.wsfev1.internal.usecase.voucher;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECompConsultar;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECompConsultaResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.common.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.voucher.VoucherConsultRequest;
import io.github.fr4ncisx.arca.wsfev1.model.voucher.VoucherConsultResponse;

/**
 * Use case to query the details of a previously authorized voucher from ARCA.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public final class GetVoucherUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<FECompConsultar, FECompConsultaResponse> soapPort;

    /**
     * Creates a new GetVoucherUseCase instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     * @throws ArcaValidationException if any parameter is null
     */
    public GetVoucherUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<FECompConsultar, FECompConsultaResponse> soapPort
    ) {
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
     * Executes the voucher consult query against ARCA.
     *
     * @param request the consult request parameters
     * @return the query response mapped to domain structures
     * @throws ArcaValidationException if request is null
     */
    public VoucherConsultResponse execute(VoucherConsultRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }

        ArcaAccessTicket ticket = authProvider.authenticate("wsfe");
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, config.cuit());
        FECompConsultar soapRequest = VoucherMapper.toSoapRequest(auth, request);

        FECompConsultaResponse soapResponse = soapPort.invoke(soapRequest);
        return VoucherMapper.toDomainResponse(soapResponse);
    }
}
