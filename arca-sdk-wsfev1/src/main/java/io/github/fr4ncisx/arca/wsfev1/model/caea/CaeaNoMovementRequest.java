package io.github.fr4ncisx.arca.wsfev1.model.caea;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

/**
 * Domain record representing a request to declare that a sales point has no movements under a CAEA code.
 *
 * @param caea       the CAEA code to report no movements for
 * @param salesPoint the sales point target
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record CaeaNoMovementRequest(String caea, int salesPoint) {

    /**
     * Standard compact constructor validating CAEA no-movement request parameters.
     */
    public CaeaNoMovementRequest {
        if (caea == null || caea.strip().isEmpty()) {
            throw new ArcaValidationException("caea must not be null or empty");
        }
        if (salesPoint <= 0) {
            throw new ArcaValidationException("salesPoint must be strictly positive");
        }
    }
}
