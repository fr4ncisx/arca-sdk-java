package io.github.fr4ncisx.arca.core;

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
 */
public sealed interface ArcaClock permits SystemClock, FixedClock {

    /**
     * Returns the current instant according to this clock.
     * @return current instant, never null
     */
    Instant now();
}
