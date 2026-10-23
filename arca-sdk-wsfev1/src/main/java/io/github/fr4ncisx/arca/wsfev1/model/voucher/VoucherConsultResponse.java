package io.github.fr4ncisx.arca.wsfev1.model.voucher;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.model.common.AfipError;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Response from querying details of a previously authorized voucher.
 *
 * @param detail the voucher details if found and authorized, or null if not found
 * @param errors the list of business errors returned by ARCA
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record VoucherConsultResponse(
        @Nullable VoucherDetail detail,
        List<AfipError> errors
) {

    /**
     * Creates an immutable snapshot of the supplied collections.
     *
     * @param detail the voucher details if found and authorized, or null if not found
     * @param errors the list of business errors returned by ARCA
     * @throws ArcaValidationException if a required collection or one of its elements is null.
     */
    public VoucherConsultResponse {
        if (errors == null) {
            throw new ArcaValidationException("errors must not be null");
        }
        try {
            errors = List.copyOf(errors);
        } catch (NullPointerException exception) {
            throw new ArcaValidationException("errors must not contain null entries", exception);
        }
    }
}
