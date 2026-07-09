package io.github.fr4ncisx.arca.wsfev1.model;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.util.List;

/**
 * Result of processing a batch of electronic invoices.
 *
 * @param entries     the list of processed entries containing request, response, and error status
 * @param interrupted indicates if the batch execution was cancelled prematurely due to a connection failure
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record BatchResponse(
        List<BatchEntry> entries,
        boolean interrupted
) {

    public BatchResponse {
        if (entries == null) {
            throw new ArcaValidationException("entries must not be null");
        }
    }
}
