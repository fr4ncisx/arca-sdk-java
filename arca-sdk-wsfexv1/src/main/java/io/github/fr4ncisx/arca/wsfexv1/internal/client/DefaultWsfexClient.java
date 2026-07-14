package io.github.fr4ncisx.arca.wsfexv1.internal.client;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfexv1.internal.usecase.AuthorizeExportVoucherUseCase;
import io.github.fr4ncisx.arca.wsfexv1.internal.usecase.GetExportVoucherUseCase;
import io.github.fr4ncisx.arca.wsfexv1.internal.usecase.GetLastExportVoucherUseCase;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherConsultRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherConsultResponse;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherResponse;
import io.github.fr4ncisx.arca.wsfexv1.model.LastExportVoucherRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.LastExportVoucherResponse;
import io.github.fr4ncisx.arca.wsfexv1.spi.WsfexClient;

public final class DefaultWsfexClient implements WsfexClient {

    private final ArcaConfig config;
    private final GetLastExportVoucherUseCase getLastExportVoucherUseCase;
    private final AuthorizeExportVoucherUseCase authorizeExportVoucherUseCase;
    private final GetExportVoucherUseCase getExportVoucherUseCase;
    private final FexdummyClient fexdummyClient;

    public DefaultWsfexClient(
            ArcaConfig config,
            GetLastExportVoucherUseCase getLastExportVoucherUseCase,
            AuthorizeExportVoucherUseCase authorizeExportVoucherUseCase,
            GetExportVoucherUseCase getExportVoucherUseCase,
            FexdummyClient fexdummyClient) {
        if (config == null) {
            throw new ArcaValidationException("config must not be null");
        }
        if (getLastExportVoucherUseCase == null) {
            throw new ArcaValidationException("getLastExportVoucherUseCase must not be null");
        }
        if (authorizeExportVoucherUseCase == null) {
            throw new ArcaValidationException("authorizeExportVoucherUseCase must not be null");
        }
        if (getExportVoucherUseCase == null) {
            throw new ArcaValidationException("getExportVoucherUseCase must not be null");
        }
        if (fexdummyClient == null) {
            throw new ArcaValidationException("fexdummyClient must not be null");
        }
        this.config = config;
        this.getLastExportVoucherUseCase = getLastExportVoucherUseCase;
        this.authorizeExportVoucherUseCase = authorizeExportVoucherUseCase;
        this.getExportVoucherUseCase = getExportVoucherUseCase;
        this.fexdummyClient = fexdummyClient;
    }

    @Override
    public LastExportVoucherResponse getLastVoucher(LastExportVoucherRequest request) {
        return getLastExportVoucherUseCase.execute(request);
    }

    @Override
    public ExportVoucherResponse authorize(ExportVoucherRequest request) {
        return authorizeExportVoucherUseCase.execute(request);
    }

    @Override
    public ExportVoucherConsultResponse getVoucher(ExportVoucherConsultRequest request) {
        return getExportVoucherUseCase.execute(request);
    }

    @Override
    public boolean ping() {
        return fexdummyClient.ping(config.environment(), config.readTimeout());
    }
}
