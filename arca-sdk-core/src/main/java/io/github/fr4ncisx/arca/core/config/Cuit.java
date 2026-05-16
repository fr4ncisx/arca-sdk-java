package io.github.fr4ncisx.arca.core.config;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

/**
 * An immutable value object that represents an Argentine CUIT (Clave Unica
 * de Identificacion Tributaria).
 * <p>
 * A CUIT looks like XX-XXXXXXXX-X. The first two digits tell you the
 * taxpayer type, the next eight are the ID number, and the last digit
 * is a checksum calculated with AFIP's modulo 11 algorithm.
 * <p>
 * Common examples: 20-33333333-4 for individuals, 27-12345678-0 for
 * legal entities, 30-12345678-1 for companies.
 * <p>
 * Throws ArcaValidationException if the number is not 11 digits or
 * the checksum digit is wrong.
 *
 * @param number the 11-digit CUIT number without dashes
 */
public record Cuit(long number) {

    private static final long MIN_CUIT = 10_000_000_000L;
    private static final long MAX_CUIT = 99_999_999_999L;
    private static final int[] WEIGHTS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    /**
     * Validates the CUIT when you create one. Checks that the number has
     * exactly 11 digits and that the checksum digit is correct.
     */
    public Cuit {
        if (number < MIN_CUIT || number > MAX_CUIT) {
            throw new ArcaValidationException("Cuit must have 11 digits");
        }
        if (!isValidVerifierDigit(number)) {
            throw new ArcaValidationException("Invalid CUIT verifier digit");
        }
    }

    /**
     * Parses a CUIT from a string. You can pass it with dashes like
     * "20-33333333-4" or without them like "20333333334" -- both work.
     *
     * @param raw the CUIT string, with or without dashes
     * @return a validated Cuit instance
     * @throws ArcaValidationException if raw is null, has the wrong format,
     *         has non-numeric characters, or the checksum is wrong
     */
    public static Cuit parse(String raw) {
        if (raw == null) {
            throw new ArcaValidationException("Cuit cannot be null");
        }
        String digits = raw.replace("-", "");
        if (digits.length() != 11 || !digits.chars().allMatch(Character::isDigit)) {
            throw new ArcaValidationException("Invalid CUIT format: " + raw);
        }
        return new Cuit(Long.parseLong(digits));
    }

    /**
     * Returns the CUIT formatted as XX-XXXXXXXX-X.
     *
     * @return the CUIT in its canonical string form.
     */
    @Override
    public String toString() {
        String s = String.valueOf(number);
        return s.substring(0, 2) + "-" + s.substring(2, 10) + "-" + s.substring(10);
    }

    /**
     * Checks whether the CUIT's checksum digit is correct using AFIP's
     * modulo 11 algorithm. It multiplies each of the first 10 digits by
     * a weight (5,4,3,2,7,6,5,4,3,2), adds them up, and uses the
     * remainder to figure out what the last digit should be. If the
     * remainder is 0 the checksum is 0, if it is 1 the checksum is 9,
     * otherwise it is 11 minus the remainder.
     *
     * @param cuit the full 11-digit CUIT number
     * @return true if the checksum digit matches
     */
    private static boolean isValidVerifierDigit(long cuit) {
        int expectedVerifier = (int) (cuit % 10);
        long first10 = cuit / 10;

        int sum = 0;
        long temp = first10;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += (int) (temp % 10) * WEIGHTS[WEIGHTS.length - 1 - i];
            temp /= 10;
        }

        int remainder = sum % 11;
        int computedVerifier = switch (remainder) {
            case 0 -> 0;
            case 1 -> 9;
            default -> 11 - remainder;
        };
        return computedVerifier == expectedVerifier;
    }
}
