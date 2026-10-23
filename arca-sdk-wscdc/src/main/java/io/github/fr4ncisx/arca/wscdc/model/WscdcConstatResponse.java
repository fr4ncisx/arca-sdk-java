package io.github.fr4ncisx.arca.wscdc.model;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
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
     * Creates an immutable snapshot of the supplied collections.
     *
     * @param result the validation result (e.g. "A" for approved, "R" for rejected)
     * @param processDate the date when the request was processed by ARCA, or null
     * @param observations the list of validation observations/warnings
     * @param errors the list of validation errors
     * @throws ArcaValidationException if a required collection or one of its elements is null.
     * @throws NullPointerException if a required scalar value is null.
     */
    public WscdcConstatResponse {
        Objects.requireNonNull(result, "result must not be null");
        if (observations == null) {
            throw new ArcaValidationException("observations must not be null");
        }
        if (errors == null) {
            throw new ArcaValidationException("errors must not be null");
        }
        try {
            observations = List.copyOf(observations);
            errors = List.copyOf(errors);
        } catch (NullPointerException exception) {
            throw new ArcaValidationException("response lists must not contain null entries", exception);
        }
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
