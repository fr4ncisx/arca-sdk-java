package io.github.fr4ncisx.arca.wsaa.internal.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.github.fr4ncisx.arca.core.clock.ArcaClock;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;

/**
 * In-memory implementation of the WSAA ticket cache.
 * <p>
 * The cache stores one {@link ArcaAccessTicket} per key inside the current
 * process and removes expired tickets lazily when they are read.
 *
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
public final class InMemoryTicketCache implements TicketCache {

    private static final String NULL_CLOCK = "clock must not be null";
    private static final String NULL_KEY = "key must not be null";
    private static final String NULL_TICKET = "ticket must not be null";

    private final ArcaClock clock;
    private final Map<String, ArcaAccessTicket> tickets = new HashMap<>();

    /**
     * Creates an in-memory cache using the supplied clock for expiration checks.
     *
     * @param clock time source used to evaluate ticket expiration.
     * @throws ArcaValidationException if clock is null.
     */
    public InMemoryTicketCache(ArcaClock clock) {
        if (clock == null) {
            throw new ArcaValidationException(NULL_CLOCK);
        }
        this.clock = clock;
    }

    /**
     * Returns a valid ticket associated with the key and evicts it when expired.
     *
     * @param key cache key associated with the ticket.
     * @return stored valid ticket, or an empty value when absent or expired.
     * @throws ArcaValidationException if key is null.
     */
    @Override
    public Optional<ArcaAccessTicket> get(String key) {
        validateKey(key);
        ArcaAccessTicket ticket = tickets.get(key);
        if (ticket == null) {
            return Optional.empty();
        }
        if (ticket.isExpired(clock)) {
            tickets.remove(key);
            return Optional.empty();
        }
        return Optional.of(ticket);
    }

    /**
     * Stores or replaces a ticket for the supplied key.
     *
     * @param key cache key associated with the ticket.
     * @param ticket ticket to store.
     * @throws ArcaValidationException if key or ticket is null.
     */
    @Override
    public void put(String key, ArcaAccessTicket ticket) {
        validateKey(key);
        if (ticket == null) {
            throw new ArcaValidationException(NULL_TICKET);
        }
        tickets.put(key, ticket);
    }

    /**
     * Removes the ticket associated with the supplied key.
     *
     * @param key cache key to evict.
     * @throws ArcaValidationException if key is null.
     */
    @Override
    public void evict(String key) {
        validateKey(key);
        tickets.remove(key);
    }

    /**
     * Removes all tickets stored in memory.
     */
    @Override
    public void clear() {
        tickets.clear();
    }

    private static void validateKey(String key) {
        if (key == null) {
            throw new ArcaValidationException(NULL_KEY);
        }
    }
}
