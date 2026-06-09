package io.github.fr4ncisx.arca.core.tax;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link CuitPrefixValidator}.
 * <p>
 * Verifies that prefix policies are validated independently of the base CUIT
 * parsing and checksum rules.
 *
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
class CuitPrefixValidationTest {

    /**
     * Validates that a CUIT whose two leading digits are allowed passes.
     */
    @Test
    void acceptsAllowedPrefix() {
        var cuit = Cuit.parse("20-33333333-4");

        assertThatCode(() -> CuitPrefixValidator.validateAllowedPrefixes(cuit, Set.of(20, 27)))
            .doesNotThrowAnyException();
    }

    /**
     * Validates that a valid CUIT with a prefix outside the policy is rejected.
     */
    @Test
    void rejectsDisallowedPrefix() {
        var cuit = Cuit.parse("30-12345678-1");

        assertThatThrownBy(() -> CuitPrefixValidator.validateAllowedPrefixes(cuit, Set.of(20, 27)))
            .isInstanceOf(ArcaValidationException.class)
            .hasMessageContaining("prefix");
    }

    /**
     * Validates that a null CUIT is rejected before reading its number.
     */
    @Test
    void rejectsNullCuit() {
        assertThatThrownBy(() -> CuitPrefixValidator.validateAllowedPrefixes(null, Set.of(20)))
            .isInstanceOf(ArcaValidationException.class)
            .hasMessageContaining("cuit");
    }

    /**
     * Validates that a null prefix policy is rejected.
     */
    @Test
    void rejectsNullAllowedPrefixes() {
        var cuit = Cuit.parse("20-33333333-4");

        assertThatThrownBy(() -> CuitPrefixValidator.validateAllowedPrefixes(cuit, null))
            .isInstanceOf(ArcaValidationException.class)
            .hasMessageContaining("allowedPrefixes");
    }

    /**
     * Validates that an empty prefix policy is rejected.
     */
    @Test
    void rejectsEmptyAllowedPrefixes() {
        var cuit = Cuit.parse("20-33333333-4");

        assertThatThrownBy(() -> CuitPrefixValidator.validateAllowedPrefixes(cuit, Set.of()))
            .isInstanceOf(ArcaValidationException.class)
            .hasMessageContaining("empty");
    }

    /**
     * Validates that a null entry inside the prefix policy is rejected.
     */
    @Test
    void rejectsAllowedPrefixesContainingNull() {
        var cuit = Cuit.parse("20-33333333-4");
        var allowedPrefixes = new HashSet<Integer>();
        allowedPrefixes.add(20);
        allowedPrefixes.add(null);

        assertThatThrownBy(() -> CuitPrefixValidator.validateAllowedPrefixes(cuit, allowedPrefixes))
            .isInstanceOf(ArcaValidationException.class)
            .hasMessageContaining("10 and 99");
    }

    /**
     * Validates that policy entries lower than two digits are rejected.
     */
    @Test
    void rejectsAllowedPrefixesContainingValueLowerThan10() {
        var cuit = Cuit.parse("20-33333333-4");

        assertThatThrownBy(() -> CuitPrefixValidator.validateAllowedPrefixes(cuit, Set.of(9, 20)))
            .isInstanceOf(ArcaValidationException.class)
            .hasMessageContaining("10 and 99");
    }

    /**
     * Validates that policy entries greater than two digits are rejected.
     */
    @Test
    void rejectsAllowedPrefixesContainingValueGreaterThan99() {
        var cuit = Cuit.parse("20-33333333-4");

        assertThatThrownBy(() -> CuitPrefixValidator.validateAllowedPrefixes(cuit, Set.of(20, 100)))
            .isInstanceOf(ArcaValidationException.class)
            .hasMessageContaining("10 and 99");
    }

    /**
     * Validates that base CUIT parsing behavior remains independent from prefix policy validation.
     */
    @Test
    void doesNotChangeBaseCuitParsingBehavior() {
        assertThat(Cuit.parse("20-33333333-4").number()).isEqualTo(20_333_333_334L);
        assertThatThrownBy(() -> Cuit.parse("20-33333333-0"))
            .isInstanceOf(ArcaValidationException.class)
            .hasMessageContaining("verifier");
    }
}
