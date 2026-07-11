package io.github.fr4ncisx.arca.wsfev1.model.cae;

import io.github.fr4ncisx.arca.wsfev1.model.common.AfipError;

import java.time.LocalDate;
import java.util.List;

/**
 * Result of a voucher authorization request containing CAE details and AFIP responses.
 *
 * @param approved      whether the voucher was successfully approved by ARCA
 * @param cae           the authorized CAE code (null if rejected)
 * @param caeExpiration the expiration date of the CAE code (null if rejected)
 * @param errors        the list of errors or warnings reported by ARCA
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public record CaeResponse(
        boolean approved,
        String cae,
        LocalDate caeExpiration,
        List<AfipError> errors
) {

    public CaeResponse {
        if (errors == null) {
            errors = List.of();
        } else {
            errors = List.copyOf(errors);
        }
    }

    /**
     * Helper method to determine if the voucher request was approved.
     *
     * @return true if approved, false otherwise
     */
    public boolean isApproved() {
        return approved;
    }
}
