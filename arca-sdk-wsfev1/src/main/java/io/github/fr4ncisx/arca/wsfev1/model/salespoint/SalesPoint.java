package io.github.fr4ncisx.arca.wsfev1.model.salespoint;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Represents an authorized sales point registered on ARCA.
 *
 * @param number       the sales point number
 * @param emissionType the type of voucher emission method used
 * @param blocked      indicates if the sales point is blocked
 * @param dropDate     the date the sales point was dropped, if any
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record SalesPoint(
        int number,
        String emissionType,
        boolean blocked,
        Optional<LocalDate> dropDate
) {

    public SalesPoint {
        if (number <= 0) {
            throw new ArcaValidationException("number must be greater than 0");
        }
        if (emissionType == null || emissionType.trim().isEmpty()) {
            throw new ArcaValidationException("emissionType must not be null or blank");
        }
        if (dropDate == null) {
            throw new ArcaValidationException("dropDate must not be null");
        }
    }
}
