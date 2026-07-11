package io.github.fr4ncisx.arca.wsfev1.internal.client;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.batch.BatchProcessUseCase;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.cae.RequestCaeUseCase;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.catalog.*;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.lastvoucher.GetLastVoucherUseCase;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.salespoint.GetSalesPointsUseCase;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.voucher.GetVoucherUseCase;
import io.github.fr4ncisx.arca.wsfev1.model.cae.*;
import io.github.fr4ncisx.arca.wsfev1.model.lastvoucher.*;
import io.github.fr4ncisx.arca.wsfev1.model.voucher.*;
import io.github.fr4ncisx.arca.wsfev1.model.salespoint.*;
import io.github.fr4ncisx.arca.wsfev1.model.batch.*;
import io.github.fr4ncisx.arca.wsfev1.model.catalog.*;
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
    private final GetVoucherTypesUseCase getVoucherTypesUseCase;
    private final GetDocumentTypesUseCase getDocumentTypesUseCase;
    private final GetVatTypesUseCase getVatTypesUseCase;
    private final GetCurrenciesUseCase getCurrenciesUseCase;
    private final GetExchangeRateUseCase getExchangeRateUseCase;
    private final GetMaxRecordsUseCase getMaxRecordsUseCase;
    private final GetConceptTypesUseCase getConceptTypesUseCase;
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
     * @param getVoucherTypesUseCase the use case to list voucher types
     * @param getDocumentTypesUseCase the use case to list document types
     * @param getVatTypesUseCase    the use case to list VAT types
     * @param getCurrenciesUseCase  the use case to list currencies
     * @param getExchangeRateUseCase the use case to get currency exchange rates
     * @param getMaxRecordsUseCase  the use case to get max records per request
     * @param getConceptTypesUseCase the use case to list concept types
     * @param fedummyClient         the ping connection client
     */
    public DefaultWsfeClient(
            ArcaConfig config,
            GetLastVoucherUseCase getLastVoucherUseCase,
            RequestCaeUseCase requestCaeUseCase,
            GetSalesPointsUseCase getSalesPointsUseCase,
            GetVoucherUseCase getVoucherUseCase,
            BatchProcessUseCase batchProcessUseCase,
            GetVoucherTypesUseCase getVoucherTypesUseCase,
            GetDocumentTypesUseCase getDocumentTypesUseCase,
            GetVatTypesUseCase getVatTypesUseCase,
            GetCurrenciesUseCase getCurrenciesUseCase,
            GetExchangeRateUseCase getExchangeRateUseCase,
            GetMaxRecordsUseCase getMaxRecordsUseCase,
            GetConceptTypesUseCase getConceptTypesUseCase,
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
        if (getVoucherTypesUseCase == null) {
            throw new ArcaValidationException("getVoucherTypesUseCase must not be null");
        }
        if (getDocumentTypesUseCase == null) {
            throw new ArcaValidationException("getDocumentTypesUseCase must not be null");
        }
        if (getVatTypesUseCase == null) {
            throw new ArcaValidationException("getVatTypesUseCase must not be null");
        }
        if (getCurrenciesUseCase == null) {
            throw new ArcaValidationException("getCurrenciesUseCase must not be null");
        }
        if (getExchangeRateUseCase == null) {
            throw new ArcaValidationException("getExchangeRateUseCase must not be null");
        }
        if (getMaxRecordsUseCase == null) {
            throw new ArcaValidationException("getMaxRecordsUseCase must not be null");
        }
        if (getConceptTypesUseCase == null) {
            throw new ArcaValidationException("getConceptTypesUseCase must not be null");
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
        this.getVoucherTypesUseCase = getVoucherTypesUseCase;
        this.getDocumentTypesUseCase = getDocumentTypesUseCase;
        this.getVatTypesUseCase = getVatTypesUseCase;
        this.getCurrenciesUseCase = getCurrenciesUseCase;
        this.getExchangeRateUseCase = getExchangeRateUseCase;
        this.getMaxRecordsUseCase = getMaxRecordsUseCase;
        this.getConceptTypesUseCase = getConceptTypesUseCase;
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

    @Override
    public List<VoucherTypeDetail> getVoucherTypes() {
        return getVoucherTypesUseCase.execute();
    }

    @Override
    public List<DocumentTypeInfo> getDocumentTypes() {
        return getDocumentTypesUseCase.execute();
    }

    @Override
    public List<VatTypeInfo> getVatTypes() {
        return getVatTypesUseCase.execute();
    }

    @Override
    public List<CurrencyInfo> getCurrencies() {
        return getCurrenciesUseCase.execute();
    }

    @Override
    public ExchangeRate getExchangeRate(String currencyId) {
        return getExchangeRateUseCase.execute(currencyId);
    }

    @Override
    public int getMaxRecordsPerRequest() {
        return getMaxRecordsUseCase.execute();
    }

    @Override
    public List<ConceptTypeInfo> getConceptTypes() {
        return getConceptTypesUseCase.execute();
    }
}
