package io.github.fr4ncisx.arca.wscdc.model;

import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Represents the response from a voucher constatation request.
 *
 * @param result the validation result (e.g. "A" for approved, "R" for rejected)
 * @param processDate the date when the request was processed by ARCA, or null
 * @param observations the list of validation observations/warnings
 * @param errors the list of validation errors
 * @author fr4ncisx
 * @since 0.9.0
 */
public record WscdcConstatResponse(
    String result,
    @Nullable LocalDate processDate,
    List<WscdcObservation> observations,
    List<WscdcError> errors
) {
    /**
     * Compact constructor to validate required parameters.
     */
    public WscdcConstatResponse {
        java.util.Objects.requireNonNull(result, "result must not be null");
        java.util.Objects.requireNonNull(observations, "observations must not be null");
        java.util.Objects.requireNonNull(errors, "errors must not be null");
    }

    /**
     * Checks if the voucher was approved/validated successfully.
     *
     * @return true if the result is "A", false otherwise
     */
    public boolean isApproved() {
        return "A".equalsIgnoreCase(result);
    }
}
