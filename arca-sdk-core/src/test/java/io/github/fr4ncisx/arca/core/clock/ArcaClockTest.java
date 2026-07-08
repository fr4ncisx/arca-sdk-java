package io.github.fr4ncisx.arca.core.clock;

import java.time.Instant;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ArcaClock} implementations: {@link SystemClock} and {@link FixedClock}.
 * <p>
 * Verifies that SystemClock returns progressing instants, FixedClock returns
 * a constant instant, withFixed produces a new instance, and the sealed
 * interface permits exactly two implementations.
 *
 * @author fr4ncisx
 * @since 0.1.0-M1
 */
@SuppressWarnings("null")
class ArcaClockTest {

    /**
     * Validates that SystemClock returns different (or equal) instants on
     * consecutive calls, confirming it delegates to real system time.
     */
    @Test
    void systemClockNowReturnsDifferentInstantsOnConsecutiveCalls() {
        var clock = SystemClock.INSTANCE;
        var first = clock.now();
        var second = clock.now();
        assertThat(first).isBeforeOrEqualTo(second);
        assertThat(second).isAfterOrEqualTo(first);
    }

    /**
     * Validates that FixedClock returns the same instant on every call,
     * confirming deterministic time for testing.
     */
    @Test
    void fixedClockNowReturnsSameInstantEveryTime() {
        var instant = Instant.parse("2026-05-12T15:00:00Z");
        var clock = new FixedClock(instant);

        assertThat(clock.now()).isEqualTo(instant);
        assertThat(clock.now()).isEqualTo(instant);
        assertThat(clock.now()).isEqualTo(instant);
    }

    /**
     * Validates that withFixed returns a new FixedClock instance with the
     * updated instant while the original clock remains unchanged.
     */
    @Test
    void fixedClockWithFixedReturnsNewInstanceAndOriginalUnchanged() {
        var original = Instant.parse("2026-05-12T15:00:00Z");
        var updated = Instant.parse("2026-05-13T15:00:00Z");
        var clock = new FixedClock(original);

        var newClock = clock.withFixed(updated);

        assertThat(newClock).isNotSameAs(clock);
        assertThat(newClock.now()).isEqualTo(updated);
        assertThat(clock.now()).isEqualTo(original);
    }

    /**
     * Validates that FixedClock rejects a null instant with ArcaValidationException.
     */
    @Test
    void fixedClockRejectsNullInstant() {
        assertThatThrownBy(() -> new FixedClock(null))
            .isInstanceOf(ArcaValidationException.class)
            .hasMessage("fixed must not be null");
    }

    /**
     * Validates that SystemClock implements the ArcaClock interface.
     */
    @Test
    void systemClockImplementsArcaClock() {
        assertThat(SystemClock.INSTANCE).isInstanceOf(ArcaClock.class);
    }

    /**
     * Validates that FixedClock implements the ArcaClock interface.
     */
    @Test
    void fixedClockImplementsArcaClock() {
        var clock = new FixedClock(Instant.now());
        assertThat(clock).isInstanceOf(ArcaClock.class);
    }

    /**
     * Validates that the ArcaClock sealed interface permits exactly two
     * subclasses: SystemClock and FixedClock.
     */
    @Test
    void sealedInterfacePermitsExactlyTwoSubclasses() {
        var permitted = ArcaClock.class.getPermittedSubclasses();

        assertThat(permitted)
            .isNotNull()
            .hasSize(2)
            .containsExactlyInAnyOrder(SystemClock.class, FixedClock.class);
    }

    /**
     * Validates that ArcaClock is declared as a sealed interface.
     */
    @Test
    void arcaClockIsSealed() {
        assertThat(ArcaClock.class.isSealed()).isTrue();
    }
}
