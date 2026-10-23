package io.github.fr4ncisx.arca.wsaa.internal.cache;

import io.github.fr4ncisx.arca.core.clock.SystemClock;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Exercises a persistent ticket cache from a separate JVM process.
 *
 * @author fr4ncisx
 * @since 1.2.4
 */
public final class TicketCacheProcessProbe {

    private TicketCacheProcessProbe() {
    }

    /**
     * Repeatedly writes and reads a ticket for the process coordination test.
     *
     * @param args cache directory and worker identifier.
     */
    public static void main(String[] args) {
        Path directory = Path.of(args[0]);
        String workerId = args[1];
        var cache = new PersistentTicketCache(directory, SystemClock.INSTANCE);
        for (int iteration = 0; iteration < 40; iteration++) {
            Instant generationTime = Instant.now();
            cache.put("wsfe", new ArcaAccessTicket(
                    workerId + "-" + iteration,
                    "test-sign",
                    generationTime,
                    generationTime.plusSeconds(3600)));
            ArcaAccessTicket ticket = cache.get("wsfe")
                    .orElseThrow(() -> new IllegalStateException("ticket disappeared during cache operation"));
            if (!ticket.token().startsWith("worker-")) {
                throw new IllegalStateException("ticket contents were corrupted");
            }
        }
        System.out.print("ok");
    }
}
