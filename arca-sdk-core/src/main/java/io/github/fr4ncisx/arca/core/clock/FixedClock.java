package io.github.fr4ncisx.arca.core.clock;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.time.Instant;

/**
 * Test clock that always returns the same configurable instant.
 * <p>
 * Immutable: changing the fixed instant produces a new instance via
 * withFixed(Instant) instead of mutating state.
 * Uses a compact constructor to reject null instants at creation time.
 *
 * @param fixed the instant this clock will always return, never null
 * @author fr4ncisx
 * @since 0.1.0-M1
 */
public record FixedClock(Instant fixed) implements ArcaClock {

    /**
     * Compact constructor that rejects null instants.
     *
     * @param fixed the fixed instant, must not be null
     * @throws ArcaValidationException if fixed is null
     */
    public FixedClock {
        if (fixed == null) {
            throw new ArcaValidationException("fixed must not be null");
        }
    }

    /**
     * Returns the fixed instant configured at construction time.
     *
     * @return the fixed instant, never null
     */
    @Override
    public Instant now() {
        return fixed;
    }

    /**
     * Returns a new FixedClock with a different fixed instant.
     * This clock remains unchanged (immutable).
     *
     * @param newFixed the new fixed instant, must not be null
     * @return new FixedClock instance with the given instant
     * @throws ArcaValidationException if newFixed is null
     */
    public FixedClock withFixed(Instant newFixed) {
        return new FixedClock(newFixed);
    }
}
