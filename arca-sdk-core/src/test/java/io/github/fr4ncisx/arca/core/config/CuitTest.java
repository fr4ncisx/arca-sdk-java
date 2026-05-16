package io.github.fr4ncisx.arca.core.config;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Cuit value object")
class CuitTest {

    @Nested
    @DisplayName("parse(String)")
    class ParseTests {

        @Test
        @DisplayName("accepts format with dashes: 20-33333333-4")
        void parsesWithDashes() {
            Cuit cuit = Cuit.parse("20-33333333-4");
            assertThat(cuit.number()).isEqualTo(20_333_333_334L);
        }

        @Test
        @DisplayName("accepts format without dashes: 20333333334")
        void parsesWithoutDashes() {
            Cuit cuit = Cuit.parse("20333333334");
            assertThat(cuit.number()).isEqualTo(20_333_333_334L);
        }

        @Test
        @DisplayName("both formats produce equal Cuit instances")
        void bothFormatsEqual() {
            Cuit withDashes = Cuit.parse("20-33333333-4");
            Cuit withoutDashes = Cuit.parse("20333333334");
            assertThat(withDashes).isEqualTo(withoutDashes);
        }

        @Test
        @DisplayName("rejects null with ArcaValidationException")
        void rejectsNull() {
            assertThatThrownBy(() -> Cuit.parse(null))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("null");
        }

        @Test
        @DisplayName("rejects invalid verifier digit: 20-33333333-0")
        void rejectsInvalidVerifierDigit() {
            assertThatThrownBy(() -> Cuit.parse("20-33333333-0"))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("verifier");
        }

        @Test
        @DisplayName("rejects wrong length")
        void rejectsWrongLength() {
            assertThatThrownBy(() -> Cuit.parse("20-3333333-4"))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("format");
        }

        @Test
        @DisplayName("rejects non-numeric characters")
        void rejectsNonNumeric() {
            assertThatThrownBy(() -> Cuit.parse("20-ABC333333-4"))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("format");
        }
    }

    @Nested
    @DisplayName("compact constructor")
    class ConstructorTests {

        @Test
        @DisplayName("rejects negative values")
        void rejectsNegative() {
            assertThatThrownBy(() -> new Cuit(-1L))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("11 digits");
        }

        @Test
        @DisplayName("rejects zero")
        void rejectsZero() {
            assertThatThrownBy(() -> new Cuit(0L))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("11 digits");
        }

        @Test
        @DisplayName("rejects values with fewer than 11 digits")
        void rejectsTooShort() {
            assertThatThrownBy(() -> new Cuit(123_456_789L))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("11 digits");
        }

        @Test
        @DisplayName("rejects values with more than 11 digits")
        void rejectsTooLong() {
            assertThatThrownBy(() -> new Cuit(123_456_789_012L))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("11 digits");
        }

        @Test
        @DisplayName("rejects invalid verifier digit")
        void rejectsInvalidVerifier() {
            assertThatThrownBy(() -> new Cuit(20_333_333_330L))
                    .isInstanceOf(ArcaValidationException.class)
                    .hasMessageContaining("verifier");
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("returns canonical format XX-XXXXXXXX-X")
        void returnsCanonicalFormat() {
            Cuit cuit = Cuit.parse("20333333334");
            assertThat(cuit).hasToString("20-33333333-4");
        }

        @Test
        @DisplayName("formats with leading zeros in middle section")
        void formatsWithLeadingZeros() {
            Cuit cuit = Cuit.parse("20-00000001-9");
            assertThat(cuit).hasToString("20-00000001-9");
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualityTests {

        @Test
        @DisplayName("equal Cuits with same number")
        void equalWithSameNumber() {
            Cuit a = Cuit.parse("20-33333333-4");
            Cuit b = Cuit.parse("20333333334");
            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        }

        @Test
        @DisplayName("different Cuits with different numbers")
        void differentWithDifferentNumbers() {
            Cuit a = Cuit.parse("20-33333333-4");
            Cuit b = Cuit.parse("27-12345678-0");
            assertThat(a).isNotEqualTo(b);
        }
    }

    @Nested
    @DisplayName("valid CUIT examples")
    class ValidCuitExamples {

        @Test
        @DisplayName("20-33333333-4 (persona humana)")
        void personaHumana() {
            Cuit cuit = Cuit.parse("20-33333333-4");
            assertThat(cuit.number()).isEqualTo(20_333_333_334L);
        }

        @Test
        @DisplayName("27-12345678-0 (persona jurídica)")
        void personaJuridica() {
            Cuit cuit = Cuit.parse("27-12345678-0");
            assertThat(cuit.number()).isEqualTo(27_123_456_780L);
        }

        @Test
        @DisplayName("30-12345678-1 (sociedad)")
        void sociedad() {
            Cuit cuit = Cuit.parse("30-12345678-1");
            assertThat(cuit.number()).isEqualTo(30_123_456_781L);
        }
    }
}
