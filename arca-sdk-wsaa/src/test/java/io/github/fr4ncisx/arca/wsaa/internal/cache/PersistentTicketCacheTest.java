package io.github.fr4ncisx.arca.wsaa.internal.cache;

import io.github.fr4ncisx.arca.core.clock.FixedClock;
import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

    /**
     * Reports a read-only deployment volume through an SDK exception.
     */
    @Test
    void rejectsReadOnlyVolumes() {
        var filesystem = FileSystems.getFileSystem(URI.create("jrt:/"));
        assertThat(filesystem.isReadOnly()).isTrue();
        var cache = new PersistentTicketCache(filesystem.getPath("/modules/java.base"), new FixedClock(NOW));
        assertThatThrownBy(() -> cache.put(KEY, new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP)))
                .isInstanceOf(ArcaAuthException.class);
        assertThatThrownBy(() -> cache.get(KEY)).isInstanceOf(ArcaAuthException.class);
        assertThatThrownBy(() -> cache.evict(KEY)).isInstanceOf(ArcaAuthException.class);
        assertThatThrownBy(cache::clear).isInstanceOf(ArcaAuthException.class);
    }

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

    /**
     * Verifies that clear() only deletes files owned by the cache (*.cache)
     * and preserves unrelated files sharing the configured directory.
     */
    @Test
    void clearPreservesUnrelatedFilesInDirectory() throws IOException {
        var cache = new PersistentTicketCache(tempDir, new FixedClock(NOW));
        Path unrelatedFile = tempDir.resolve("operator-notes.txt");
        Files.writeString(unrelatedFile, "keep me");

        cache.put(KEY, new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP));
        cache.clear();

        assertThat(unrelatedFile).exists();
        assertThat(tempDir.resolve(KEY + ".cache")).doesNotExist();
    }

    /**
     * Verifies that persisted ticket files are restricted to their owner on
     * POSIX-compliant filesystems. The assertion is skipped on filesystems
     * without POSIX support.
     */
    @Test
    void restrictsPersistedTicketFilePermissionsOnPosixFilesystems() throws IOException {
        var cache = new PersistentTicketCache(tempDir, new FixedClock(NOW));

        cache.put(KEY, new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP));

        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return;
        }

        var permissions = Files.getPosixFilePermissions(tempDir.resolve(KEY + ".cache"));
        assertThat(permissions)
                .contains(PosixFilePermission.OWNER_READ,
                          PosixFilePermission.OWNER_WRITE)
                .doesNotContain(PosixFilePermission.GROUP_READ,
                                PosixFilePermission.OTHERS_READ);

        var lockPermissions = Files.getPosixFilePermissions(tempDir.resolve(".ticket-cache.lock"));
        assertThat(lockPermissions)
                .contains(PosixFilePermission.OWNER_READ,
                          PosixFilePermission.OWNER_WRITE)
                .doesNotContain(PosixFilePermission.GROUP_READ,
                                PosixFilePermission.OTHERS_READ);
    }

    /**
     * Verifies that concurrent cache instances never expose partially written tickets.
     *
     * @throws Exception if a concurrent cache operation fails.
     */
    @Test
    void concurrentInstancesShareAConsistentTicketFile() throws Exception {
        var firstCache = new PersistentTicketCache(tempDir, new FixedClock(NOW));
        var secondCache = new PersistentTicketCache(tempDir, new FixedClock(NOW));
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> operations = new ArrayList<>();

        try {
            for (int worker = 0; worker < 8; worker++) {
                PersistentTicketCache cache = worker % 2 == 0 ? firstCache : secondCache;
                int workerId = worker;
                operations.add(executor.submit(() -> {
                    start.await();
                    for (int iteration = 0; iteration < 30; iteration++) {
                        cache.put(KEY, new ArcaAccessTicket(
                                "token-" + workerId + "-" + iteration, SIGN, GEN, EXP));
                        cache.get(KEY).ifPresent(ticket ->
                                assertThat(ticket.token()).startsWith("token-"));
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> operation : operations) {
                operation.get(10, TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    /**
     * Verifies that separate JVMs serialize ticket updates through the shared lock file.
     *
     * @throws Exception if a probe process fails or does not exit within the test deadline.
     */
    @Test
    void serializesTicketUpdatesAcrossProcesses() throws Exception {
        String executableName = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", executableName);
        String classPath = System.getProperty("java.class.path");
        Process first = new ProcessBuilder(
                javaExecutable.toString(), "-cp", classPath, TicketCacheProcessProbe.class.getName(),
                tempDir.toString(), "worker-a")
                .redirectErrorStream(true)
                .start();
        Process second = new ProcessBuilder(
                javaExecutable.toString(), "-cp", classPath, TicketCacheProcessProbe.class.getName(),
                tempDir.toString(), "worker-b")
                .redirectErrorStream(true)
                .start();

        boolean firstExited = first.waitFor(15, TimeUnit.SECONDS);
        boolean secondExited = second.waitFor(15, TimeUnit.SECONDS);
        if (!firstExited) {
            first.destroyForcibly();
        }
        if (!secondExited) {
            second.destroyForcibly();
        }
        String firstOutput = new String(first.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String secondOutput = new String(second.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(firstExited).isTrue();
        assertThat(secondExited).isTrue();
        assertThat(first.exitValue()).isZero();
        assertThat(second.exitValue()).isZero();
        assertThat(firstOutput).contains("ok");
        assertThat(secondOutput).contains("ok");
        assertThat(new PersistentTicketCache(tempDir, new FixedClock(NOW)).get(KEY)).isPresent();
    }
}
