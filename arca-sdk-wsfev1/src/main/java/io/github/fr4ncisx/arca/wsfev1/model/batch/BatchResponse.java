package io.github.fr4ncisx.arca.wsfev1.model.batch;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.util.List;

/**
 * Result of processing a batch of electronic invoices.
 *
 * @param entries     results in input order; thread interruption retains every position with an error for unresolved requests
 * @param interrupted indicates early termination; fail-fast batches may contain only the processed prefix
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record BatchResponse(
        List<BatchEntry> entries,
        boolean interrupted
) {

    /**
     * Creates an immutable snapshot of ordered batch outcomes.
     *
     * @param entries results in input order; thread interruption retains every position with an error for unresolved requests
     * @param interrupted indicates early termination; fail-fast batches may contain only the processed prefix
     * @throws ArcaValidationException if entries or any entry is null.
     */
    public BatchResponse {
        if (entries == null) {
            throw new ArcaValidationException("entries must not be null");
        }
        for (BatchEntry entry : entries) {
            if (entry == null) {
                throw new ArcaValidationException("entries must not contain null values");
            }
        }
        entries = List.copyOf(entries);
    }
}
