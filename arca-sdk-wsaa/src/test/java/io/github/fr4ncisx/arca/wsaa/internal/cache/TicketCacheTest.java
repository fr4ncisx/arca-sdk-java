package io.github.fr4ncisx.arca.wsaa.internal.cache;

import java.time.Instant;
import java.util.Optional;

import io.github.fr4ncisx.arca.core.clock.FixedClock;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TicketCache} and {@link InMemoryTicketCache}.
 * <p>
 * Verifies in-memory storage, lazy expiration, eviction, clearing, replacement,
 * and argument validation.
 *
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
class TicketCacheTest {

    private static final String KEY = "wsaa-login";
    private static final Instant NOW = Instant.parse("2026-05-15T10:00:00Z");
    private static final Instant GENERATION = NOW.minusSeconds(60);
    private static final Instant EXPIRATION = NOW.plusSeconds(60);
    private static final String TOKEN = "abc123def456ghi789";
    private static final String SIGN = "signed-data-abcdef";

    /**
     * Verifies that an empty cache returns an empty Optional.
     */
    @Test
    void returnsEmptyWhenCacheIsEmpty() {
        TicketCache cache = cacheAt(NOW);

        Optional<ArcaAccessTicket> result = cache.get(KEY);

        assertThat(result).isEmpty();
    }

    /**
     * Verifies that a stored ticket is returned while it has not expired.
     */
    @Test
    void returnsTicketWhenNotExpired() {
        TicketCache cache = cacheAt(NOW);
        ArcaAccessTicket ticket = ticket(EXPIRATION);

        cache.put(KEY, ticket);

        assertThat(cache.get(KEY)).contains(ticket);
    }

    /**
     * Verifies that expired tickets are not returned and are removed lazily.
     */
    @Test
    void returnsEmptyAndEvictsWhenExpired() {
        TicketCache cache = cacheAt(NOW);
        ArcaAccessTicket expired = ticket(NOW.minusSeconds(1));

        cache.put(KEY, expired);

        assertThat(cache.get(KEY)).isEmpty();
        assertThat(cache.get(KEY)).isEmpty();
    }

    /**
     * Verifies that a ticket expiring exactly at the current instant remains valid.
     */
    @Test
    void returnsTicketWhenClockEqualsExpiration() {
        TicketCache cache = cacheAt(NOW);
        ArcaAccessTicket boundary = ticket(NOW);

        cache.put(KEY, boundary);

        assertThat(cache.get(KEY)).contains(boundary);
    }

    /**
     * Verifies that putting a second ticket for the same key replaces the previous ticket.
     */
    @Test
    void putReplacesTicketForSameKey() {
        TicketCache cache = cacheAt(NOW);
        ArcaAccessTicket first = ticket(EXPIRATION);
        ArcaAccessTicket second = new ArcaAccessTicket(
                "replacement-token",
                "replacement-sign",
                GENERATION,
                EXPIRATION.plusSeconds(60));

        cache.put(KEY, first);
        cache.put(KEY, second);

        assertThat(cache.get(KEY)).contains(second);
    }

    /**
     * Verifies that evict removes the ticket associated with a key.
     */
    @Test
    void evictRemovesTicket() {
        TicketCache cache = cacheAt(NOW);

        cache.put(KEY, ticket(EXPIRATION));
        cache.evict(KEY);

        assertThat(cache.get(KEY)).isEmpty();
    }

    /**
     * Verifies that clear removes every stored ticket.
     */
    @Test
    void clearRemovesAllTickets() {
        TicketCache cache = cacheAt(NOW);

        cache.put(KEY, ticket(EXPIRATION));
        cache.put("other-service", ticket(EXPIRATION.plusSeconds(30)));
        cache.clear();

        assertThat(cache.get(KEY)).isEmpty();
        assertThat(cache.get("other-service")).isEmpty();
    }

    /**
     * Verifies that constructor and method null arguments are rejected.
     */
    @Test
    void rejectsNullArguments() {
        TicketCache cache = cacheAt(NOW);

        assertThatThrownBy(() -> new InMemoryTicketCache(null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("clock must not be null");
        assertThatThrownBy(() -> cache.get(null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("key must not be null");
        assertThatThrownBy(() -> cache.put(null, ticket(EXPIRATION)))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("key must not be null");
        assertThatThrownBy(() -> cache.put(KEY, null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("ticket must not be null");
        assertThatThrownBy(() -> cache.evict(null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("key must not be null");
    }

    private static TicketCache cacheAt(Instant instant) {
        return new InMemoryTicketCache(new FixedClock(instant));
    }

    private static ArcaAccessTicket ticket(Instant expiration) {
        return new ArcaAccessTicket(TOKEN, SIGN, GENERATION, expiration);
    }
}
