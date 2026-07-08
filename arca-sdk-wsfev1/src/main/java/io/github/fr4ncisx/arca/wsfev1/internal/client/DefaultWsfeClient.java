package io.github.fr4ncisx.arca.wsfev1.internal.client;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.GetLastVoucherUseCase;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.RequestCaeUseCase;
import io.github.fr4ncisx.arca.wsfev1.model.CaeRequest;
import io.github.fr4ncisx.arca.wsfev1.model.CaeResponse;
import io.github.fr4ncisx.arca.wsfev1.model.LastVoucherRequest;
import io.github.fr4ncisx.arca.wsfev1.model.LastVoucherResponse;
import io.github.fr4ncisx.arca.wsfev1.spi.WsfeClient;

/**
 * Default implementation of the {@link WsfeClient} interface.
 * <p>
 * Decouples presentation and client calls by delegating directly to use cases.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public final class DefaultWsfeClient implements WsfeClient {

    private final GetLastVoucherUseCase getLastVoucherUseCase;
    private final RequestCaeUseCase requestCaeUseCase;

    /**
     * Creates a new client instance.
     *
     * @param getLastVoucherUseCase the use case to query last voucher numbers
     * @param requestCaeUseCase    the use case to request CAE authorization
     */
    public DefaultWsfeClient(
            GetLastVoucherUseCase getLastVoucherUseCase,
            RequestCaeUseCase requestCaeUseCase) {
        if (getLastVoucherUseCase == null) {
            throw new ArcaValidationException("getLastVoucherUseCase must not be null");
        }
        if (requestCaeUseCase == null) {
            throw new ArcaValidationException("requestCaeUseCase must not be null");
        }
        this.getLastVoucherUseCase = getLastVoucherUseCase;
        this.requestCaeUseCase = requestCaeUseCase;
    }

    @Override
    public LastVoucherResponse getLastVoucher(LastVoucherRequest request) {
        return getLastVoucherUseCase.execute(request);
    }

    @Override
    public CaeResponse requestCae(CaeRequest request) {
        return requestCaeUseCase.execute(request);
    }
}
