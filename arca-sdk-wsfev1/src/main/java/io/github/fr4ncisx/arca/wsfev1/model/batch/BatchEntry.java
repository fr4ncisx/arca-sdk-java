package io.github.fr4ncisx.arca.wsfev1.model.batch;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeRequest;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeResponse;

import java.util.Optional;

/**
 * Individual result entry for a batch invoice authorization.
 *
 * @param request  the original CaeRequest
 * @param response the authorization result returned by ARCA, if successful
 * @param error    the exception representing a network or soap communication error, if any occurred
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record BatchEntry(
        CaeRequest request,
        Optional<CaeResponse> response,
        Optional<Throwable> error
) {

    public BatchEntry {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        if (response == null) {
            throw new ArcaValidationException("response must not be null");
        }
        if (error == null) {
            throw new ArcaValidationException("error must not be null");
        }
    }
}
