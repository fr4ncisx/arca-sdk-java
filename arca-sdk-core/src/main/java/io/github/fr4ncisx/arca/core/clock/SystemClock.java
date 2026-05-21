package io.github.fr4ncisx.arca.core.clock;

import java.time.Instant;

/**
 * Production clock that delegates to Instant.now().
 * <p>
 * Thread-safe by definition: enum singleton with no mutable state.
 * Each call to now() returns the real system time.
 *
 * @author fr4ncisx
 * @since 0.1.0-M1
 */
@SuppressWarnings("java:S6548")
public enum SystemClock implements ArcaClock {

    /** Single instance. */
    INSTANCE;

    /**
     * Returns the current system instant.
     *
     * @return current Instant from the system clock, never null
     */
    @Override
    public Instant now() {
        return Instant.now();
    }
}
