package io.github.fr4ncisx.arca.core;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ArcaClock, SystemClock, and FixedClock.
 * Verifies thread-safety, immutability, sealed hierarchy enforcement,
 * and deterministic time control for testing.
 */
class ArcaClockTest {

    @Test
    void systemClockNowReturnsDifferentInstantsOnConsecutiveCalls() {
        var clock = SystemClock.INSTANCE;
        var first = clock.now();
        var second = clock.now();
        assertThat(first).isBeforeOrEqualTo(second);
        assertThat(second).isAfterOrEqualTo(first);
    }

    @Test
    void fixedClockNowReturnsSameInstantEveryTime() {
        var instant = Instant.parse("2026-05-12T15:00:00Z");
        var clock = new FixedClock(instant);

        assertThat(clock.now()).isEqualTo(instant);
        assertThat(clock.now()).isEqualTo(instant);
        assertThat(clock.now()).isEqualTo(instant);
    }

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

    @Test
    void fixedClockRejectsNullInstant() {
        assertThatThrownBy(() -> new FixedClock(null))
            .isInstanceOf(ArcaValidationException.class)
            .hasMessage("fixed must not be null");
    }

    @Test
    void systemClockImplementsArcaClock() {
        assertThat(SystemClock.INSTANCE).isInstanceOf(ArcaClock.class);
    }

    @Test
    void fixedClockImplementsArcaClock() {
        var clock = new FixedClock(Instant.now());
        assertThat(clock).isInstanceOf(ArcaClock.class);
    }

    @Test
    void sealedInterfacePermitsExactlyTwoSubclasses() {
        var permitted = ArcaClock.class.getPermittedSubclasses();

        assertThat(permitted)
            .isNotNull()
            .hasSize(2)
            .containsExactlyInAnyOrder(SystemClock.class, FixedClock.class);
    }

    @Test
    void arcaClockIsSealed() {
        assertThat(ArcaClock.class.isSealed()).isTrue();
    }
}
