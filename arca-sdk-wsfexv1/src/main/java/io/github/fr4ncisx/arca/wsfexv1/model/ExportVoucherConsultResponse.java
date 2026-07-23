package io.github.fr4ncisx.arca.wsfexv1.model;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Result of querying an export voucher containing the detailed invoice and AFIP messages.
 *
 * @param detail the queried export voucher details, or null if not found
 * @param errors the list of errors returned by ARCA
 * @param events the list of events returned by ARCA
 * @author fr4ncisx
 * @since 0.7.0
 */
public record ExportVoucherConsultResponse(
        @Nullable ExportVoucherDetail detail,
        List<AfipError> errors,
        List<AfipError> events
) {

    public ExportVoucherConsultResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
        events = events == null ? List.of() : List.copyOf(events);
    }
}
