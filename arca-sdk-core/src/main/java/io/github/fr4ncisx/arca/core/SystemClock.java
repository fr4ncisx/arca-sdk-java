package io.github.fr4ncisx.arca.core;

import java.time.Instant;

/**
 * Production clock that delegates to Instant.now().
 * <p>
 * Thread-safe by definition: enum singleton with no mutable state.
 * Each call to now() returns the real system time.
 *
 * @see FixedClock
 * @see ArcaClock
 */
public enum SystemClock implements ArcaClock {

    /** Single instance. */
    INSTANCE;

    /**
     * Returns the current system instant.
     * @return current Instant from the system clock, never null
     */
    @Override
    public Instant now() {
        return Instant.now();
    }
}
