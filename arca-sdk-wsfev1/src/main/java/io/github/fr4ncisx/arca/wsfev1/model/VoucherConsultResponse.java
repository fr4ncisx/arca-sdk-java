package io.github.fr4ncisx.arca.wsfev1.model;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.util.List;
import java.util.Optional;

/**
 * Response from querying details of a previously authorized voucher.
 *
 * @param detail the voucher details if found and authorized
 * @param errors the list of business errors returned by ARCA
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record VoucherConsultResponse(
        Optional<VoucherDetail> detail,
        List<AfipError> errors
) {

    public VoucherConsultResponse {
        if (detail == null) {
            throw new ArcaValidationException("detail must not be null");
        }
        if (errors == null) {
            throw new ArcaValidationException("errors must not be null");
        }
    }
}
