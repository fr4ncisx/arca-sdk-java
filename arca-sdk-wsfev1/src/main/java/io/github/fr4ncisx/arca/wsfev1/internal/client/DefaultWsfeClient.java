package io.github.fr4ncisx.arca.wsfev1.internal.client;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.*;
import io.github.fr4ncisx.arca.wsfev1.model.*;
import io.github.fr4ncisx.arca.wsfev1.spi.WsfeClient;

import java.util.List;

/**
 * Default implementation of the {@link WsfeClient} interface.
 * <p>
 * Decouples presentation and client calls by delegating directly to use cases.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public final class DefaultWsfeClient implements WsfeClient {

    private final ArcaConfig config;
    private final GetLastVoucherUseCase getLastVoucherUseCase;
    private final RequestCaeUseCase requestCaeUseCase;
    private final GetSalesPointsUseCase getSalesPointsUseCase;
    private final GetVoucherUseCase getVoucherUseCase;
    private final BatchProcessUseCase batchProcessUseCase;
    private final FedummyClient fedummyClient;

    /**
     * Creates a new client instance.
     *
     * @param config                the SDK configuration
     * @param getLastVoucherUseCase the use case to query last voucher numbers
     * @param requestCaeUseCase    the use case to request CAE authorization
     * @param getSalesPointsUseCase the use case to list sales points
     * @param getVoucherUseCase     the use case to retrieve authorized voucher details
     * @param batchProcessUseCase   the use case to process batches
     * @param fedummyClient         the ping connection client
     */
    public DefaultWsfeClient(
            ArcaConfig config,
            GetLastVoucherUseCase getLastVoucherUseCase,
            RequestCaeUseCase requestCaeUseCase,
            GetSalesPointsUseCase getSalesPointsUseCase,
            GetVoucherUseCase getVoucherUseCase,
            BatchProcessUseCase batchProcessUseCase,
            FedummyClient fedummyClient) {
        if (config == null) {
            throw new ArcaValidationException("config must not be null");
        }
        if (getLastVoucherUseCase == null) {
            throw new ArcaValidationException("getLastVoucherUseCase must not be null");
        }
        if (requestCaeUseCase == null) {
            throw new ArcaValidationException("requestCaeUseCase must not be null");
        }
        if (getSalesPointsUseCase == null) {
            throw new ArcaValidationException("getSalesPointsUseCase must not be null");
        }
        if (getVoucherUseCase == null) {
            throw new ArcaValidationException("getVoucherUseCase must not be null");
        }
        if (batchProcessUseCase == null) {
            throw new ArcaValidationException("batchProcessUseCase must not be null");
        }
        if (fedummyClient == null) {
            throw new ArcaValidationException("fedummyClient must not be null");
        }
        this.config = config;
        this.getLastVoucherUseCase = getLastVoucherUseCase;
        this.requestCaeUseCase = requestCaeUseCase;
        this.getSalesPointsUseCase = getSalesPointsUseCase;
        this.getVoucherUseCase = getVoucherUseCase;
        this.batchProcessUseCase = batchProcessUseCase;
        this.fedummyClient = fedummyClient;
    }

    @Override
    public LastVoucherResponse getLastVoucher(LastVoucherRequest request) {
        return getLastVoucherUseCase.execute(request);
    }

    @Override
    public CaeResponse requestCae(CaeRequest request) {
        return requestCaeUseCase.execute(request);
    }

    @Override
    public boolean ping() {
        return fedummyClient.ping(config.environment(), config.readTimeout());
    }

    @Override
    public List<SalesPoint> getSalesPoints() {
        return getSalesPointsUseCase.execute();
    }

    @Override
    public VoucherConsultResponse getVoucher(VoucherConsultRequest request) {
        return getVoucherUseCase.execute(request);
    }

    @Override
    public BatchResponse processBatch(BatchRequest request) {
        return batchProcessUseCase.execute(request);
    }
}
