package io.github.fr4ncisx.arca.wsfev1.model.caea;

import io.github.fr4ncisx.arca.wsfev1.model.common.AfipError;

import java.util.List;

/**
 * Domain record representing the processing outcome for a single voucher reported under a CAEA.
 *
 * @param number       the voucher number
 * @param result       the processing result ("A" for accepted, "R" for rejected)
 * @param errors       the list of errors for this specific voucher
 * @param observations the list of warnings or observations for this specific voucher
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record CaeaVoucherResult(
        long number,
        String result,
        List<AfipError> errors,
        List<AfipError> observations
) {

    /**
     * Standard compact constructor initializing lists as immutable copies.
     */
    public CaeaVoucherResult {
        errors = errors != null ? List.copyOf(errors) : List.of();
        observations = observations != null ? List.copyOf(observations) : List.of();
    }
}
