package io.github.fr4ncisx.arca.wsfexv1.model;

import java.util.List;

/**
 * Result of an export voucher authorization request containing CAE details and AFIP responses.
 *
 * @param id                the request identifier
 * @param cuit              the taxpayer identifier (cuit)
 * @param voucherType       the voucher type code
 * @param salesPoint        the sales point number
 * @param voucherNumber     the voucher number
 * @param cae               the authorized CAE code (null if rejected)
 * @param caeExpirationDate the expiration date of the CAE code (null if rejected, format: yyyyMMdd)
 * @param errors            the list of errors or warnings reported by ARCA
 * @param events            the list of events reported by ARCA
 * @author fr4ncisx
 * @since 0.7.0
 */
public record ExportVoucherResponse(
        long id,
        long cuit,
        short voucherType,
        int salesPoint,
        long voucherNumber,
        String cae,
        String caeExpirationDate,
        List<AfipError> errors,
        List<AfipError> events
) {

    public ExportVoucherResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
        events = events == null ? List.of() : List.copyOf(events);
    }

    /**
     * Determines if the export voucher request was approved by checking the presence of a CAE.
     *
     * @return true if approved, false otherwise
     */
    public boolean isApproved() {
        return cae != null && !cae.isBlank();
    }
}
