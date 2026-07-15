package io.github.fr4ncisx.arca.wscdc.internal.client;

import io.github.fr4ncisx.arca.wscdc.internal.usecase.ConstatVoucherUseCase;
import io.github.fr4ncisx.arca.wscdc.model.WscdcConstatRequest;
import io.github.fr4ncisx.arca.wscdc.model.WscdcConstatResponse;
import io.github.fr4ncisx.arca.wscdc.model.WscdcDummyResponse;
import io.github.fr4ncisx.arca.wscdc.spi.WscdcClient;
import java.util.Objects;

/**
 * Default implementation of the WscdcClient SPI.
 *
 * @author fr4ncisx
 * @since 0.9.0
 */
public final class DefaultWscdcClient implements WscdcClient {

    private final WscdcDummyClient dummyClient;
    private final ConstatVoucherUseCase constatUseCase;

    /**
     * Constructs a new DefaultWscdcClient.
     *
     * @param dummyClient the dummy ping client helper
     * @param constatUseCase the constatation use case executor
     */
    public DefaultWscdcClient(WscdcDummyClient dummyClient, ConstatVoucherUseCase constatUseCase) {
        this.dummyClient = Objects.requireNonNull(dummyClient, "dummyClient must not be null");
        this.constatUseCase = Objects.requireNonNull(constatUseCase, "constatUseCase must not be null");
    }

    @Override
    public boolean ping() {
        try {
            return dummyClient.ping().isOk();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public WscdcDummyResponse dummy() {
        return dummyClient.ping();
    }

    @Override
    public WscdcConstatResponse checkVoucher(WscdcConstatRequest request) {
        return constatUseCase.execute(request);
    }
}
