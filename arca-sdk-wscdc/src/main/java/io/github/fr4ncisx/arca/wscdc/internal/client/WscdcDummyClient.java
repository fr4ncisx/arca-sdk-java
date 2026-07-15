package io.github.fr4ncisx.arca.wscdc.internal.client;

import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wscdc.internal.generated.DummyResponse;
import io.github.fr4ncisx.arca.wscdc.internal.usecase.WscdcMapper;
import io.github.fr4ncisx.arca.wscdc.model.WscdcDummyResponse;
import java.util.Objects;

/**
 * Internal helper client to execute the WSCDC ComprobanteDummy operation.
 *
 * @author fr4ncisx
 * @since 0.9.0
 */
public final class WscdcDummyClient {

    private final ArcaSoapPort<Void, DummyResponse> soapPort;

    /**
     * Constructs a new WscdcDummyClient.
     *
     * @param soapPort the SOAP port wrapper
     */
    public WscdcDummyClient(ArcaSoapPort<Void, DummyResponse> soapPort) {
        this.soapPort = Objects.requireNonNull(soapPort, "soapPort must not be null");
    }

    /**
     * Pings the WSCDC service.
     *
     * @return the public dummy response status
     */
    public WscdcDummyResponse ping() {
        DummyResponse response = soapPort.invoke(null);
        return WscdcMapper.mapDummy(response);
    }
}
