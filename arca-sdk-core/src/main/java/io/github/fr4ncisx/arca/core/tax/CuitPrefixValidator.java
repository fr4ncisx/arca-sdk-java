package io.github.fr4ncisx.arca.core.tax;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.util.Set;

/**
 * Validates CUIT prefixes against an externally supplied policy.
 * <p>
 * This validator does not parse CUIT values and does not repeat the checksum
 * validation performed by {@link Cuit}. It only reads the two leading digits
 * from an already validated CUIT number and checks them against the provided
 * set of allowed prefixes.
 *
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
public final class CuitPrefixValidator {

    private static final long PREFIX_DIVISOR = 1_000_000_000L;
    private static final int MIN_PREFIX = 10;
    private static final int MAX_PREFIX = 99;

    private CuitPrefixValidator() {
    }

    /**
     * Validates that the CUIT prefix is present in the provided allowed prefix set.
     * <p>
     * The {@code allowedPrefixes} argument is a caller-provided policy. Every
     * entry must be a two-digit integer between 10 and 99.
     *
     * @param cuit the already validated CUIT to evaluate.
     * @param allowedPrefixes the allowed two-digit CUIT prefixes.
     * @throws ArcaValidationException if {@code cuit} is null, if
     *         {@code allowedPrefixes} is null or empty, if any configured prefix
     *         is null or outside the 10-99 range, or if the CUIT prefix is not
     *         allowed.
     */
    public static void validateAllowedPrefixes(Cuit cuit, Set<Integer> allowedPrefixes) {
        if (cuit == null) {
            throw new ArcaValidationException("cuit must not be null");
        }
        if (allowedPrefixes == null) {
            throw new ArcaValidationException("allowedPrefixes must not be null");
        }
        if (allowedPrefixes.isEmpty()) {
            throw new ArcaValidationException("allowedPrefixes must not be empty");
        }

        for (Integer prefix : allowedPrefixes) {
            if (prefix == null || prefix < MIN_PREFIX || prefix > MAX_PREFIX) {
                throw new ArcaValidationException("allowedPrefixes must contain only values between 10 and 99");
            }
        }

        int cuitPrefix = (int) (cuit.number() / PREFIX_DIVISOR);
        if (!allowedPrefixes.contains(cuitPrefix)) {
            throw new ArcaValidationException("CUIT prefix is not allowed");
        }
    }
}
