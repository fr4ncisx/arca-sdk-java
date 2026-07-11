package io.github.fr4ncisx.arca.wsfev1.model.caea;

import io.github.fr4ncisx.arca.wsfev1.model.common.AfipError;

import java.time.LocalDate;
import java.util.List;

/**
 * Domain record representing the response details of a CAEA code from ARCA.
 * <p>
 * This object contains the official CAEA code, its period of application, and the
 * validity range (start, end, and expiration dates) along with any errors or observations
 * returned by the server.
 *
 * @param caea           the CAEA code assigned by ARCA (may be null if request was rejected)
 * @param period         the calendar period in yyyyMM format
 * @param order          the fortnight order (1 or 2)
 * @param startDate      the start date of the CAEA applicability period
 * @param endDate        the end date of the CAEA applicability period
 * @param expirationDate the expiration date for reporting vouchers under this CAEA
 * @param errors         the list of business or system errors reported by ARCA
 * @param observations   the list of warnings or observations reported by ARCA
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record CaeaResponse(
        String caea,
        int period,
        int order,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate expirationDate,
        List<AfipError> errors,
        List<AfipError> observations
) {

    /**
     * Standard compact constructor initializing lists as immutable copies.
     */
    public CaeaResponse {
        errors = errors != null ? List.copyOf(errors) : List.of();
        observations = observations != null ? List.copyOf(observations) : List.of();
    }
}
