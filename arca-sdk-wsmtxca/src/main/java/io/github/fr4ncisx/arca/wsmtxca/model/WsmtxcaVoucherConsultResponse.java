package io.github.fr4ncisx.arca.wsmtxca.model;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Response containing the details of a queried WSMTXCA voucher.
 *
 * @param voucher the details of the voucher, or null if not found
 * @param authorizationCode the CAE or CAEA code
 * @param authorizationType the type of authorization (e.g. "CAE", "CAEA")
 * @param errors the list of errors returned by ARCA
 * @author fr4ncisx
 * @since 0.7.0
 */
public record WsmtxcaVoucherConsultResponse(
    @Nullable WsmtxcaVoucherRequest voucher,
    @Nullable String authorizationCode,
    @Nullable String authorizationType,
    List<WsmtxcaError> errors
) {

    /**
     * Creates an immutable snapshot of the supplied collections.
     *
     * @param voucher the details of the voucher, or null if not found
     * @param authorizationCode the CAE or CAEA code
     * @param authorizationType the type of authorization (e.g. "CAE", "CAEA")
     * @param errors the list of errors returned by ARCA
     * @throws ArcaValidationException if a required collection or one of its elements is null.
     */
    public WsmtxcaVoucherConsultResponse {
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
