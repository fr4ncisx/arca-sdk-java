package io.github.fr4ncisx.arca.wsfev1.model.batch;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeRequest;

import java.util.List;

/**
 * Request containing a batch of electronic invoices to be authorized.
 *
 * @param requests    the list of individual invoice authorization requests
 * @param strategy    the batch processing strategy to apply
 * @param maxParallel the maximum number of in-flight requests in this batch; must be positive for PARALLEL_LIMITED
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record BatchRequest(
        List<CaeRequest> requests,
        BatchStrategy strategy,
        int maxParallel
) {

    /**
     * Creates a batch with an immutable snapshot of its requests.
     *
     * @param requests the list of individual invoice authorization requests
     * @param strategy the batch processing strategy to apply
     * @param maxParallel the maximum number of in-flight requests in this batch; must be positive for PARALLEL_LIMITED
     * @throws ArcaValidationException if requests or strategy are invalid, an entry is null, or parallelism is invalid.
     */
    public BatchRequest {
        if (requests == null || requests.isEmpty()) {
            throw new ArcaValidationException("requests list must not be null or empty");
        }
        for (CaeRequest request : requests) {
            if (request == null) {
                throw new ArcaValidationException("requests list must not contain null entries");
            }
        }
        requests = List.copyOf(requests);
        if (strategy == null) {
            throw new ArcaValidationException("strategy must not be null");
        }
        if (strategy == BatchStrategy.PARALLEL_LIMITED && maxParallel <= 0) {
            throw new ArcaValidationException("maxParallel must be greater than 0 when using PARALLEL_LIMITED strategy");
        }
    }
}
