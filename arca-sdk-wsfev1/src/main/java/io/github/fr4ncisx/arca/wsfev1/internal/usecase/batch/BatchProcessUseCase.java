package io.github.fr4ncisx.arca.wsfev1.internal.usecase.batch;

import io.github.fr4ncisx.arca.core.exception.ArcaException;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.cae.RequestCaeUseCase;
import io.github.fr4ncisx.arca.wsfev1.model.batch.BatchEntry;
import io.github.fr4ncisx.arca.wsfev1.model.batch.BatchRequest;
import io.github.fr4ncisx.arca.wsfev1.model.batch.BatchResponse;
import io.github.fr4ncisx.arca.wsfev1.model.batch.BatchStrategy;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeRequest;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

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
     * <p>
     * Thread interruption stops dispatch and preserves every input position.
     * Entries without a collected result report either an unknown outcome or
     * that the request was not submitted. Cancellation cannot undo remote writes.
     * Parallelism is bounded per batch; independent batches share no global limit.
     *
     * @param request the batch request configuration
     * @return the processed results response
     * @throws ArcaValidationException if request is null
     * @throws RejectedExecutionException if the executor rejects work.
     */
    public BatchResponse execute(BatchRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }

        List<CaeRequest> requests = request.requests();
        BatchStrategy strategy = request.strategy();
        if (strategy != BatchStrategy.PARALLEL_LIMITED) {
            return executeSequentially(requests, strategy);
        }
        return executeInParallel(requests, request.maxParallel());
    }

    private BatchResponse executeSequentially(List<CaeRequest> requests, BatchStrategy strategy) {
        List<BatchEntry> entries = new ArrayList<>(requests.size());
        boolean interrupted = false;
        for (CaeRequest request : requests) {
            if (Thread.currentThread().isInterrupted()) {
                return interruptedResponse(requests, indexedEntries(entries), entries.size());
            }
            try {
                entries.add(new BatchEntry(request, requestCaeUseCase.execute(request), null));
            } catch (ArcaException exception) {
                entries.add(new BatchEntry(request, null, exception));
                if (strategy == BatchStrategy.FAIL_FAST) {
                    interrupted = true;
                    break;
                }
            }
        }
        if (Thread.currentThread().isInterrupted()) {
            return interruptedResponse(requests, indexedEntries(entries), entries.size());
        }
        return new BatchResponse(entries, interrupted);
    }

    private BatchResponse executeInParallel(List<CaeRequest> requests, int maxParallel) {
        CompletionService<IndexedBatchEntry> completionService = new ExecutorCompletionService<>(executorService);
        int windowSize = Math.min(maxParallel, requests.size());
        Set<Future<IndexedBatchEntry>> pending = new HashSet<>(windowSize);
        Map<Integer, BatchEntry> entries = new HashMap<>(requests.size());
        int nextIndex = 0;
        int completed = 0;

        try {
            while (nextIndex < requests.size() && pending.size() < windowSize) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                pending.add(submit(completionService, requests, nextIndex++));
            }
            while (completed < requests.size()) {
                Future<IndexedBatchEntry> future = completionService.take();
                pending.remove(future);
                IndexedBatchEntry result = future.get();
                entries.put(result.index(), result.entry());
                completed++;

                if (nextIndex < requests.size()) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    pending.add(submit(completionService, requests, nextIndex++));
                }
            }
        } catch (InterruptedException exception) {
            try {
                collectCompletedAndCancel(pending, entries);
                return interruptedResponse(requests, entries, nextIndex);
            } finally {
                Thread.currentThread().interrupt();
            }
        } catch (ExecutionException exception) {
            cancel(pending);
            throw propagate(exception);
        } catch (RuntimeException | Error exception) {
            cancel(pending);
            throw exception;
        }

        return new BatchResponse(orderedEntries(entries, requests.size()), false);
    }

    private Future<IndexedBatchEntry> submit(
            CompletionService<IndexedBatchEntry> completionService,
            List<CaeRequest> requests,
            int index) {
        CaeRequest request = requests.get(index);
        return completionService.submit(() -> executeOne(index, request));
    }

    private IndexedBatchEntry executeOne(int index, CaeRequest request) {
        try {
            return new IndexedBatchEntry(index, new BatchEntry(request, requestCaeUseCase.execute(request), null));
        } catch (ArcaException exception) {
            return new IndexedBatchEntry(index, new BatchEntry(request, null, exception));
        }
    }

    private static List<BatchEntry> orderedEntries(Map<Integer, BatchEntry> indexedEntries, int requestCount) {
        List<BatchEntry> entries = new ArrayList<>(indexedEntries.size());
        for (int index = 0; index < requestCount; index++) {
            BatchEntry entry = indexedEntries.get(index);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static void cancel(Set<? extends Future<?>> futures) {
        futures.forEach(future -> future.cancel(true));
    }

    private static Map<Integer, BatchEntry> indexedEntries(List<BatchEntry> entries) {
        Map<Integer, BatchEntry> indexed = new HashMap<>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            indexed.put(index, entries.get(index));
        }
        return indexed;
    }

    private static void collectCompletedAndCancel(
            Set<Future<IndexedBatchEntry>> pending, Map<Integer, BatchEntry> entries) {
        cancel(pending);
        for (Future<IndexedBatchEntry> future : pending) {
            if (future.isCancelled()) {
                continue;
            }
            try {
                IndexedBatchEntry result = future.get();
                entries.put(result.index(), result.entry());
            } catch (CancellationException exception) {
                continue;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException exception) {
                throw propagate(exception);
            }
        }
    }

    private static BatchResponse interruptedResponse(
            List<CaeRequest> requests, Map<Integer, BatchEntry> entries, int submittedCount) {
        for (int index = 0; index < requests.size(); index++) {
            if (!entries.containsKey(index)) {
                String message = index < submittedCount
                        ? "Batch interrupted; operation outcome is unknown. Query its status before resubmitting."
                        : "Batch interrupted; request was not submitted.";
                entries.put(index, new BatchEntry(requests.get(index), null, new ArcaSoapException(message)));
            }
        }
        return new BatchResponse(orderedEntries(entries, requests.size()), true);
    }

    private static RuntimeException propagate(ExecutionException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof Error error) {
            throw error;
        }
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new ArcaSoapException("Batch processing failed unexpectedly", cause);
    }

    private record IndexedBatchEntry(int index, BatchEntry entry) {
    }
}
