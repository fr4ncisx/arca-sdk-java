package io.github.fr4ncisx.arca.core.clock;

import java.time.Instant;

/**
 * Abstracted time source for the SDK.
 * <p>
 * Decouples time-dependent components from Instant.now() so that
 * tests can control the clock deterministically.
 * <p>
 * Only two implementations are permitted:
 * SystemClock for production use and FixedClock for testing.
 * <p>
 * Consumed by TicketCache, TraBuilder, ArcaAccessTicket,
 * and any component that needs an injectable timestamp.
 *
 * @author fr4ncisx
 * @since 0.1.0-M1
 */
public sealed interface ArcaClock permits SystemClock, FixedClock {

    /**
     * Returns the current instant according to this clock.
     *
     * @return current instant, never null
     */
    Instant now();
}
