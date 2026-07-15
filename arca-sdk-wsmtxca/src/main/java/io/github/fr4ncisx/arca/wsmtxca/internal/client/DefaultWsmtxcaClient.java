package io.github.fr4ncisx.arca.wsmtxca.internal.client;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsmtxca.internal.usecase.AuthorizeWsmtxcaVoucherUseCase;
import io.github.fr4ncisx.arca.wsmtxca.internal.usecase.GetLastWsmtxcaVoucherUseCase;
import io.github.fr4ncisx.arca.wsmtxca.internal.usecase.GetWsmtxcaVoucherUseCase;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaLastVoucherRequest;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaLastVoucherResponse;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherConsultRequest;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherConsultResponse;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherRequest;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherResponse;
import io.github.fr4ncisx.arca.wsmtxca.spi.WsmtxcaClient;

public final class DefaultWsmtxcaClient implements WsmtxcaClient {

    private final ArcaConfig config;
    private final GetLastWsmtxcaVoucherUseCase getLastWsmtxcaVoucherUseCase;
    private final AuthorizeWsmtxcaVoucherUseCase authorizeWsmtxcaVoucherUseCase;
    private final GetWsmtxcaVoucherUseCase getWsmtxcaVoucherUseCase;
    private final WsmtxcaDummyClient wsmtxcaDummyClient;

    public DefaultWsmtxcaClient(
            ArcaConfig config,
            GetLastWsmtxcaVoucherUseCase getLastWsmtxcaVoucherUseCase,
            AuthorizeWsmtxcaVoucherUseCase authorizeWsmtxcaVoucherUseCase,
            GetWsmtxcaVoucherUseCase getWsmtxcaVoucherUseCase,
            WsmtxcaDummyClient wsmtxcaDummyClient) {
        if (config == null) {
            throw new ArcaValidationException("config must not be null");
        }
        if (getLastWsmtxcaVoucherUseCase == null) {
            throw new ArcaValidationException("getLastWsmtxcaVoucherUseCase must not be null");
        }
        if (authorizeWsmtxcaVoucherUseCase == null) {
            throw new ArcaValidationException("authorizeWsmtxcaVoucherUseCase must not be null");
        }
        if (getWsmtxcaVoucherUseCase == null) {
            throw new ArcaValidationException("getWsmtxcaVoucherUseCase must not be null");
        }
        if (wsmtxcaDummyClient == null) {
            throw new ArcaValidationException("wsmtxcaDummyClient must not be null");
        }
        this.config = config;
        this.getLastWsmtxcaVoucherUseCase = getLastWsmtxcaVoucherUseCase;
        this.authorizeWsmtxcaVoucherUseCase = authorizeWsmtxcaVoucherUseCase;
        this.getWsmtxcaVoucherUseCase = getWsmtxcaVoucherUseCase;
        this.wsmtxcaDummyClient = wsmtxcaDummyClient;
    }

    @Override
    public WsmtxcaLastVoucherResponse getLastVoucher(WsmtxcaLastVoucherRequest request) {
        return getLastWsmtxcaVoucherUseCase.execute(request);
    }

    @Override
    public WsmtxcaVoucherResponse authorize(WsmtxcaVoucherRequest request) {
        return authorizeWsmtxcaVoucherUseCase.execute(request);
    }

    @Override
    public WsmtxcaVoucherConsultResponse getVoucher(WsmtxcaVoucherConsultRequest request) {
        return getWsmtxcaVoucherUseCase.execute(request);
    }

    @Override
    public boolean ping() {
        return wsmtxcaDummyClient.ping(config.environment(), config.readTimeout());
    }
}
