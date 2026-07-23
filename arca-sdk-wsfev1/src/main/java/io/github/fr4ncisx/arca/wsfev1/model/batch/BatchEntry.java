package io.github.fr4ncisx.arca.wsfev1.model.batch;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeRequest;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeResponse;

import org.jspecify.annotations.Nullable;

/**
 * Individual result entry for a batch invoice authorization.
 *
 * @param request  the original CaeRequest
 * @param response the authorization result returned by ARCA, or null if an error occurred
 * @param error    the exception representing a network or soap communication error, or null if successful
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record BatchEntry(
        CaeRequest request,
        @Nullable CaeResponse response,
        @Nullable Throwable error
) {

    public BatchEntry {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
    }
}
