package io.github.fr4ncisx.arca.wsfexv1.model;

import java.util.List;

/**
 * Result containing the last authorized export voucher number and associated information.
 *
 * @param lastNumber the last authorized voucher number
 * @param lastDate   the date of the last authorized voucher (format: yyyyMMdd)
 * @param errors     the errors returned by ARCA
 * @param events     the events returned by ARCA
 * @author fr4ncisx
 * @since 0.7.0
 */
public record LastExportVoucherResponse(
        long lastNumber,
        String lastDate,
        List<AfipError> errors,
        List<AfipError> events
) {

    public LastExportVoucherResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
        events = events == null ? List.of() : List.copyOf(events);
    }
}
