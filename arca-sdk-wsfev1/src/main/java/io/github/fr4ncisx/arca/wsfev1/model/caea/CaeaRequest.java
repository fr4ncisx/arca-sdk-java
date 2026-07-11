package io.github.fr4ncisx.arca.wsfev1.model.caea;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

/**
 * Domain record representing the parameters required to request a CAEA code from ARCA.
 * <p>
 * According to AFIP regulations, the period must identify the target calendar month in
 * {@code yyyyMM} format, and the order must identify the target half of the month (1 or 2).
 *
 * @param period the target calendar period in yyyyMM format (e.g. 202607)
 * @param order  the target half of the month (1 for the first half, 2 for the second half)
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record CaeaRequest(int period, int order) {

    /**
     * Standard compact constructor validating CAEA request parameters.
     *
     * @throws ArcaValidationException if validation fails
     */
    public CaeaRequest {
        if (period <= 100000 || period > 999999) {
            throw new ArcaValidationException("period must be a positive 6-digit integer in yyyyMM format");
        }
        int month = period % 100;
        if (month < 1 || month > 12) {
            throw new ArcaValidationException("period month must be between 01 and 12");
        }
        if (order != 1 && order != 2) {
            throw new ArcaValidationException("order must be strictly 1 (first fortnight) or 2 (second fortnight)");
        }
    }
}
