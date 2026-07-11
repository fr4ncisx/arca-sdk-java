package io.github.fr4ncisx.arca.wsfev1.internal.usecase.batch;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.cae.RequestCaeUseCase;
import io.github.fr4ncisx.arca.wsfev1.model.batch.BatchEntry;
import io.github.fr4ncisx.arca.wsfev1.model.batch.BatchRequest;
import io.github.fr4ncisx.arca.wsfev1.model.batch.BatchResponse;
import io.github.fr4ncisx.arca.wsfev1.model.batch.BatchStrategy;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeRequest;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeResponse;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Use case to orchestrate batch invoice authorization requests.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public final class BatchProcessUseCase {

    private final RequestCaeUseCase requestCaeUseCase;
    private final ExecutorService executorService;

    /**
     * Creates a new BatchProcessUseCase instance.
     *
     * @param requestCaeUseCase the single CAE request use case
     * @param executorService   the shared thread pool
     * @throws ArcaValidationException if any parameter is null
     */
    public BatchProcessUseCase(RequestCaeUseCase requestCaeUseCase, ExecutorService executorService) {
        if (requestCaeUseCase == null) {
            throw new ArcaValidationException("requestCaeUseCase must not be null");
        }
        if (executorService == null) {
            throw new ArcaValidationException("executorService must not be null");
        }
        this.requestCaeUseCase = requestCaeUseCase;
        this.executorService = executorService;
    }

    /**
     * Executes batch processing of multiple invoice requests.
     *
     * @param request the batch request configuration
     * @return the processed results response
     * @throws ArcaValidationException if request is null
     */
    public BatchResponse execute(BatchRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }

        List<CaeRequest> requests = request.requests();
        BatchStrategy strategy = request.strategy();

        List<BatchEntry> entries = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean aborted = new AtomicBoolean(false);

        if (strategy == BatchStrategy.SEQUENTIAL || strategy == BatchStrategy.FAIL_FAST) {
            // Run sequentially if SEQUENTIAL or if FAIL_FAST is selected as a sequential flow
            // Note: FAIL_FAST sequentially aborts immediately on first network error
            for (CaeRequest req : requests) {
                if (aborted.get()) {
                    break;
                }
                try {
                    CaeResponse res = requestCaeUseCase.execute(req);
                    entries.add(new BatchEntry(req, Optional.of(res), Optional.empty()));
                } catch (Throwable t) {
                    entries.add(new BatchEntry(req, Optional.empty(), Optional.of(t)));
                    if (strategy == BatchStrategy.FAIL_FAST) {
                        aborted.set(true);
                    }
                }
            }
            return new BatchResponse(new ArrayList<>(entries), aborted.get());
        }

        // Parallel processing (PARALLEL_LIMITED)
        int maxParallel = request.maxParallel();
        Semaphore semaphore = new Semaphore(maxParallel);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (CaeRequest req : requests) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                if (aborted.get()) {
                    return;
                }
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                try {
                    if (aborted.get()) {
                        return;
                    }
                    CaeResponse res = requestCaeUseCase.execute(req);
                    entries.add(new BatchEntry(req, Optional.of(res), Optional.empty()));
                } catch (Throwable t) {
                    entries.add(new BatchEntry(req, Optional.empty(), Optional.of(t)));
                } finally {
                    semaphore.release();
                }
            }, executorService);
            futures.add(future);
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            // Exceptions are captured individually inside entries
        }

        // Sort entries to match the original requests order
        Map<CaeRequest, BatchEntry> entryMap = new HashMap<>();
        for (BatchEntry entry : entries) {
            entryMap.put(entry.request(), entry);
        }
        List<BatchEntry> sortedEntries = new ArrayList<>();
        for (CaeRequest req : requests) {
            BatchEntry entry = entryMap.get(req);
            if (entry != null) {
                sortedEntries.add(entry);
            }
        }

        return new BatchResponse(sortedEntries, aborted.get());
    }
}
