package io.github.fr4ncisx.arca.wsmtxca.model;

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
) {}
