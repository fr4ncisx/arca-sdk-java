package io.github.fr4ncisx.arca.wsmtxca.model;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Represents the response from a WSMTXCA voucher authorization request.
 *
 * @param voucherType the type code of the voucher
 * @param salesPoint the sales point of the voucher
 * @param voucherNumber the authorized number of the voucher
 * @param authorizationCode the CAE or CAEA code
 * @param authorizationType the type of authorization (e.g. "CAE", "CAEA")
 * @param expirationDate the expiration date of the authorization
 * @param issueDate the issue date registered by ARCA
 * @param status the status of authorization: "A" (Approved), "R" (Rejected)
 * @param errors the list of errors returned by ARCA
 * @param observations the list of observations/warnings returned by ARCA
 * @author fr4ncisx
 * @since 0.7.0
 */
public record WsmtxcaVoucherResponse(
    short voucherType,
    int salesPoint,
    long voucherNumber,
    @Nullable String authorizationCode,
    @Nullable String authorizationType,
    @Nullable LocalDate expirationDate,
    @Nullable LocalDate issueDate,
    String status,
    List<WsmtxcaError> errors,
    List<WsmtxcaError> observations
) {

    /**
     * Creates an immutable snapshot of the supplied collections.
     *
     * @param voucherType the type code of the voucher
     * @param salesPoint the sales point of the voucher
     * @param voucherNumber the authorized number of the voucher
     * @param authorizationCode the CAE or CAEA code
     * @param authorizationType the type of authorization (e.g. "CAE", "CAEA")
     * @param expirationDate the expiration date of the authorization
     * @param issueDate the issue date registered by ARCA
     * @param status the status of authorization: "A" (Approved), "R" (Rejected)
     * @param errors the list of errors returned by ARCA
     * @param observations the list of observations/warnings returned by ARCA
     * @throws ArcaValidationException if a required collection or one of its elements is null.
     */
    public WsmtxcaVoucherResponse {
        errors = snapshot(errors, "errors");
        observations = snapshot(observations, "observations");
    }

    private static <T> List<T> snapshot(List<T> values, String fieldName) {
        if (values == null) {
            throw new ArcaValidationException(fieldName + " must not be null");
        }
        try {
            return List.copyOf(values);
        } catch (NullPointerException exception) {
            throw new ArcaValidationException(fieldName + " must not contain null entries", exception);
        }
    }
}
