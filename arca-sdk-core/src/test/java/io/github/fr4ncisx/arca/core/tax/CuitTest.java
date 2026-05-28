package io.github.fr4ncisx.arca.core.tax;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link Cuit} value object parsing, validation, and formatting.
 * <p>
 * Verifies that valid CUIT formats are accepted, invalid formats are rejected,
 * the checksum algorithm works correctly, and the canonical string representation
 * is properly formatted.
 *
 * @author fr4ncisx
 * @since 0.1.0-M2
 */
class CuitTest {

    /**
     * Tests for the {@link Cuit#parse(String)} method.
     */
    @Nested
    @DisplayName("parse(String)")
    class ParseTests {

        /**
         * Validates that a CUIT with dashes (XX-XXXXXXXX-X) is accepted
         * and parsed correctly.
         */
        @Test
        @DisplayName("accepts format with dashes: 20-33333333-4")
        void parsesWithDashes() {
            Cuit cuit = Cuit.parse("20-33333333-4");
            assertThat(cuit.number()).isEqualTo(20_333_333_334L);
        }

        /**
         * Validates that a CUIT without dashes (XXXXXXXXXXX) is accepted
         * and parsed correctly.
         */
        @Test
        @DisplayName("accepts format without dashes: 20333333334")
        void parsesWithoutDashes() {
            Cuit cuit = Cuit.parse("20333333334");
            assertThat(cuit.number()).isEqualTo(20_333_333_334L);
        }

        /**
         * Validates that both dashed and non-dashed formats produce equal
         * Cuit instances with the same underlying number.
         */
        @Test
        @DisplayName("both formats produce equal Cuit instances")
        void bothFormatsEqual() {
            Cuit withDashes = Cuit.parse("20-33333333-4");
            Cuit withoutDashes = Cuit.parse("20333333334");
            assertThat(withDashes).isEqualTo(withoutDashes);
        }

        /**
         * Validates that passing null to parse throws ArcaValidationException.
         */
        @Test
        @DisplayName("rejects null with ArcaValidationException")
        void rejectsNull() {
            assertThatThrownBy(() -> Cuit.parse(null))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("null");
        }

        /**
         * Validates that a CUIT with an invalid check digit is rejected.
         * <p>
         * The validator computes the expected check digit using modulo 11
         * and compares it against the supplied digit.
         */
        @Test
        @DisplayName("rejects invalid verifier digit: 20-33333333-0")
        void rejectsInvalidVerifierDigit() {
            assertThatThrownBy(() -> Cuit.parse("20-33333333-0"))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("verifier");
        }

        /**
         * Validates that a CUIT with wrong length (not 11 digits) is rejected.
         */
        @Test
        @DisplayName("rejects wrong length")
        void rejectsWrongLength() {
            assertThatThrownBy(() -> Cuit.parse("20-3333333-4"))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("format");
        }

        /**
         * Validates that a CUIT containing non-numeric characters is rejected.
         */
        @Test
        @DisplayName("rejects non-numeric characters")
        void rejectsNonNumeric() {
            assertThatThrownBy(() -> Cuit.parse("20-ABC333333-4"))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("format");
        }
    }

    /**
     * Tests for the compact constructor validation logic.
     */
    @Nested
    @DisplayName("compact constructor")
    class ConstructorTests {

        /**
         * Validates that a negative number is rejected as invalid.
         */
        @Test
        @DisplayName("rejects negative values")
        void rejectsNegative() {
            assertThatThrownBy(() -> new Cuit(-1L))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("11 digits");
        }

        /**
         * Validates that zero is rejected as invalid.
         */
        @Test
        @DisplayName("rejects zero")
        void rejectsZero() {
            assertThatThrownBy(() -> new Cuit(0L))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("11 digits");
        }

        /**
         * Validates that a number with fewer than 11 digits is rejected.
         */
        @Test
        @DisplayName("rejects values with fewer than 11 digits")
        void rejectsTooShort() {
            assertThatThrownBy(() -> new Cuit(123_456_789L))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("11 digits");
        }

        /**
         * Validates that a number with more than 11 digits is rejected.
         */
        @Test
        @DisplayName("rejects values with more than 11 digits")
        void rejectsTooLong() {
            assertThatThrownBy(() -> new Cuit(123_456_789_012L))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("11 digits");
        }

        /**
         * Validates that a number with wrong checksum digit is rejected.
         */
        @Test
        @DisplayName("rejects invalid verifier digit")
        void rejectsInvalidVerifier() {
            assertThatThrownBy(() -> new Cuit(20_333_333_330L))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("verifier");
        }
    }

    /**
     * Tests for the toString() canonical formatting.
     */
    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        /**
         * Validates that toString returns the canonical XX-XXXXXXXX-X format.
         */
        @Test
        @DisplayName("returns canonical format XX-XXXXXXXX-X")
        void returnsCanonicalFormat() {
            Cuit cuit = Cuit.parse("20333333334");
            assertThat(cuit).hasToString("20-33333333-4");
        }

        /**
         * Validates that toString preserves leading zeros in the middle section.
         */
        @Test
        @DisplayName("formats with leading zeros in middle section")
        void formatsWithLeadingZeros() {
            Cuit cuit = Cuit.parse("20-00000001-9");
            assertThat(cuit).hasToString("20-00000001-9");
        }
    }

    /**
     * Tests for equals and hashCode behavior.
     */
    @Nested
    @DisplayName("equals and hashCode")
    class EqualityTests {

        /**
         * Validates that two Cuit instances with the same number are equal
         * and have the same hash code.
         */
        @Test
        @DisplayName("equal Cuits with same number")
        void equalWithSameNumber() {
            Cuit a = Cuit.parse("20-33333333-4");
            Cuit b = Cuit.parse("20333333334");
            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        }

        /**
         * Validates that two Cuit instances with different numbers are not equal.
         */
        @Test
        @DisplayName("different Cuits with different numbers")
        void differentWithDifferentNumbers() {
            Cuit a = Cuit.parse("20-33333333-4");
            Cuit b = Cuit.parse("27-12345678-0");
            assertThat(a).isNotEqualTo(b);
        }
    }

    /**
     * Tests for known valid CUIT examples from different taxpayer types.
     */
    @Nested
    @DisplayName("valid CUIT examples")
    class ValidCuitExamples {

        /**
         * Validates that a persona humana CUIT (prefix 20) is accepted.
         */
        @Test
        @DisplayName("20-33333333-4 (persona humana)")
        void personaHumana() {
            Cuit cuit = Cuit.parse("20-33333333-4");
            assertThat(cuit.number()).isEqualTo(20_333_333_334L);
        }

        /**
         * Validates that a persona juridica CUIT (prefix 27) is accepted.
         */
        @Test
        @DisplayName("27-12345678-0 (persona juridica)")
        void personaJuridica() {
            Cuit cuit = Cuit.parse("27-12345678-0");
            assertThat(cuit.number()).isEqualTo(27_123_456_780L);
        }

        /**
         * Validates that a sociedad CUIT (prefix 30) is accepted.
         */
        @Test
        @DisplayName("30-12345678-1 (sociedad)")
        void sociedad() {
            Cuit cuit = Cuit.parse("30-12345678-1");
            assertThat(cuit.number()).isEqualTo(30_123_456_781L);
        }
    }
}
