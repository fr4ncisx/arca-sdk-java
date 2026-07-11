package io.github.fr4ncisx.arca.wsfev1.model.catalog;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Exchange rate of a specific currency compared to Argentine Pesos (ARS).
 *
 * @param currencyId the currency string identifier (e.g. "DOL")
 * @param rate       the exchange rate value
 * @param date       the date of the exchange rate value, if returned by ARCA
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public record ExchangeRate(
        String currencyId,
        double rate,
        Optional<LocalDate> date
) {

    public ExchangeRate {
        if (currencyId == null || currencyId.trim().isEmpty()) {
            throw new ArcaValidationException("currencyId must not be null or blank");
        }
        if (rate <= 0) {
            throw new ArcaValidationException("rate must be strictly positive");
        }
        if (date == null) {
            throw new ArcaValidationException("date must not be null");
        }
    }
}
