package io.github.fr4ncisx.arca.wsfev1.model.caea;

import io.github.fr4ncisx.arca.wsfev1.model.common.AfipError;

import java.util.List;

/**
 * Domain record representing the outcome of reporting a batch of vouchers under a CAEA.
 *
 * @param result  the global batch processing result ("A" for accepted, "R" for rejected, "P" for partial)
 * @param results the list of outcomes for each individual voucher in the batch
 * @param errors  any global or system errors reported by ARCA
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record CaeaReportResponse(
        String result,
        List<CaeaVoucherResult> results,
        List<AfipError> errors
) {

    /**
     * Standard compact constructor initializing lists as immutable copies.
     */
    public CaeaReportResponse {
        results = results != null ? List.copyOf(results) : List.of();
        errors = errors != null ? List.copyOf(errors) : List.of();
    }
}
