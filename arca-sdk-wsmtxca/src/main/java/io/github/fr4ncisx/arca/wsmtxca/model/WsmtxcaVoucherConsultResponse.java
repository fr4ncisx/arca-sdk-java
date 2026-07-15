package io.github.fr4ncisx.arca.wsmtxca.model;

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
) {}
