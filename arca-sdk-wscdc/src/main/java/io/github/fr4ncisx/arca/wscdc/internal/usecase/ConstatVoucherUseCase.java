package io.github.fr4ncisx.arca.wscdc.internal.usecase;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wscdc.internal.generated.CmpAuthRequest;
import io.github.fr4ncisx.arca.wscdc.internal.generated.CmpDatos;
import io.github.fr4ncisx.arca.wscdc.internal.generated.CmpResponse;
import io.github.fr4ncisx.arca.wscdc.model.WscdcConstatRequest;
import io.github.fr4ncisx.arca.wscdc.model.WscdcConstatResponse;
import java.util.Objects;

/**
 * Use case to validate the authenticity of a voucher with WSCDC.
 *
 * @author fr4ncisx
 * @since 0.9.0
 */
public final class ConstatVoucherUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<WscdcRequestWrapper, CmpResponse> soapPort;

    /**
     * Constructs a new ConstatVoucherUseCase.
     *
     * @param config the client configuration
     * @param authProvider the authentication provider
     * @param soapPort the SOAP port wrapper
     */
    public ConstatVoucherUseCase(
        ArcaConfig config,
        AuthProvider authProvider,
        ArcaSoapPort<WscdcRequestWrapper, CmpResponse> soapPort
    ) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.authProvider = Objects.requireNonNull(authProvider, "authProvider must not be null");
        this.soapPort = Objects.requireNonNull(soapPort, "soapPort must not be null");
    }

    /**
     * Executes the voucher validation flow.
     *
     * @param request the public domain request
     * @return the public domain response
     */
    public WscdcConstatResponse execute(WscdcConstatRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ArcaAccessTicket ticket = authProvider.authenticate("wscdc");
        CmpAuthRequest auth = WscdcMapper.mapAuth(ticket, config.cuit());
        CmpDatos reqData = WscdcMapper.mapRequest(request);

        WscdcRequestWrapper wrapper = new WscdcRequestWrapper(auth, reqData);
        CmpResponse response = soapPort.invoke(wrapper);
        return WscdcMapper.mapResponse(response);
    }
}
