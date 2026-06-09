package io.github.fr4ncisx.arca.wsaa.internal.cache;

import java.util.Optional;

import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;

/**
 * Internal cache port for WSAA access tickets.
 * <p>
 * Implementations store tickets by caller-defined keys and return only tickets
 * that remain usable according to their expiration policy.
 *
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
public interface TicketCache {

    /**
     * Returns the ticket stored for the given key when it is still valid.
     *
     * @param key cache key associated with the ticket.
     * @return stored valid ticket, or an empty value when absent or expired.
     */
    Optional<ArcaAccessTicket> get(String key);

    /**
     * Stores or replaces the ticket associated with the given key.
     *
     * @param key cache key associated with the ticket.
     * @param ticket ticket to store.
     */
    void put(String key, ArcaAccessTicket ticket);

    /**
     * Removes any ticket associated with the given key.
     *
     * @param key cache key to evict.
     */
    void evict(String key);

    /**
     * Removes all tickets from the cache.
     */
    void clear();
}
