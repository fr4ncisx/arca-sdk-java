package io.github.fr4ncisx.arca.wsaa.internal.cache;

import io.github.fr4ncisx.arca.core.clock.FixedClock;
import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PersistentTicketCache}.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
class PersistentTicketCacheTest {

    private static final String KEY = "wsfe";
    private static final Instant NOW = Instant.parse("2026-07-07T10:00:00Z");
    private static final Instant GEN = NOW.minusSeconds(60);
    private static final Instant EXP = NOW.plusSeconds(3600);
    private static final String TOKEN = "test-token";
    private static final String SIGN = "test-sign";

    @TempDir
    Path tempDir;

    @Test
    void constructorRejectsNulls() {
        assertThatThrownBy(() -> new PersistentTicketCache(null, new FixedClock(NOW)))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> new PersistentTicketCache(tempDir, null))
                .isInstanceOf(ArcaValidationException.class);
    }

    @Test
    void returnsEmptyWhenFileDoesNotExist() {
        var cache = new PersistentTicketCache(tempDir, new FixedClock(NOW));
        assertThat(cache.get(KEY)).isEmpty();
    }

    @Test
    void putsAndGetsValidTicket() {
        var cache = new PersistentTicketCache(tempDir, new FixedClock(NOW));
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);

        cache.put(KEY, ticket);

        Optional<ArcaAccessTicket> loaded = cache.get(KEY);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().token()).isEqualTo(TOKEN);
        assertThat(loaded.get().sign()).isEqualTo(SIGN);
        assertThat(loaded.get().generationTime()).isEqualTo(GEN);
        assertThat(loaded.get().expirationTime()).isEqualTo(EXP);
    }

    @Test
    void evictsTicketFromDiskAndReturnsEmpty() {
        var cache = new PersistentTicketCache(tempDir, new FixedClock(NOW));
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);

        cache.put(KEY, ticket);
        assertThat(cache.get(KEY)).isPresent();

        cache.evict(KEY);
        assertThat(cache.get(KEY)).isEmpty();
        assertThat(tempDir.resolve("wsfe.cache")).doesNotExist();
    }

    @Test
    void evictsTicketAutomaticallyWhenExpired() {
        var clock = new FixedClock(NOW);
        var cache = new PersistentTicketCache(tempDir, clock);

        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, NOW.plusSeconds(5));
        cache.put(KEY, ticket);

        // Advance clock past expiration
        var advancedCache = new PersistentTicketCache(tempDir, new FixedClock(NOW.plusSeconds(10)));

        assertThat(advancedCache.get(KEY)).isEmpty();
        assertThat(tempDir.resolve("wsfe.cache")).doesNotExist();
    }

    @Test
    void throwsAuthExceptionOnCorruptedPropertiesFile() throws IOException {
        var cache = new PersistentTicketCache(tempDir, new FixedClock(NOW));
        Path filePath = tempDir.resolve("wsfe.cache");
        Files.createDirectories(tempDir);

        // Write corrupt/missing properties
        Files.writeString(filePath, "token=123\n# missing other properties");

        assertThatThrownBy(() -> cache.get(KEY))
                .isInstanceOf(ArcaAuthException.class)
                .hasMessageContaining("Corrupt ticket cache");
    }

    @Test
    void throwsAuthExceptionOnInvalidTimestampFormat() throws IOException {
        var cache = new PersistentTicketCache(tempDir, new FixedClock(NOW));
        Path filePath = tempDir.resolve("wsfe.cache");
        Files.createDirectories(tempDir);

        Files.writeString(filePath, "token=123\nsign=456\ngenerationTime=not-an-instant\nexpirationTime=neither");

        assertThatThrownBy(() -> cache.get(KEY))
                .isInstanceOf(ArcaAuthException.class)
                .hasMessageContaining("Failed to parse persistent ticket timestamps");
    }

    @Test
    void clearsCacheDirectory() {
        var cache = new PersistentTicketCache(tempDir, new FixedClock(NOW));
        cache.put("wsfe", new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP));
        cache.put("ws_sr_padron_a4", new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP));

        assertThat(tempDir.resolve("wsfe.cache")).exists();
        assertThat(tempDir.resolve("ws_sr_padron_a4.cache")).exists();

        cache.clear();

        assertThat(tempDir.resolve("wsfe.cache")).doesNotExist();
        assertThat(tempDir.resolve("ws_sr_padron_a4.cache")).doesNotExist();
    }
}
