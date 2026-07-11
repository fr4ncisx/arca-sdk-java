package io.github.fr4ncisx.arca.wsfev1.model.caea;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

/**
 * Domain record representing a query to check if a sales point has registered a no-movement declaration for a CAEA.
 *
 * @param caea       the CAEA code to query
 * @param salesPoint the sales point to check
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record CaeaNoMovementQuery(String caea, int salesPoint) {

    /**
     * Standard compact constructor validating CAEA no-movement query parameters.
     */
    public CaeaNoMovementQuery {
        if (caea == null || caea.strip().isEmpty()) {
            throw new ArcaValidationException("caea must not be null or empty");
        }
        if (salesPoint <= 0) {
            throw new ArcaValidationException("salesPoint must be strictly positive");
        }
    }
}
