package io.github.fr4ncisx.arca.wsaa.internal.auth;

import io.github.fr4ncisx.arca.core.clock.FixedClock;
import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.cache.InMemoryTicketCache;
import io.github.fr4ncisx.arca.wsaa.internal.client.LoginCmsClient;
import io.github.fr4ncisx.arca.wsaa.internal.tra.TraGenerator;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsaa.spi.TraSigner;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency stress tests for {@link DefaultAuthProvider}.
 * <p>
 * Ensures that under high concurrent demand for the same service token, only
 * a single remote WSAA login request is initiated while other waiting threads
 * safely await and receive the same response.
 *
 * @author fr4ncisx
 * @since 1.1.0
 */
class DefaultAuthProviderStressTest {

    private static final String SERVICE = "wsfe";
    private static final Instant NOW = Instant.parse("2026-07-07T10:00:00Z");
    private static final Cuit CUIT = Cuit.parse("20-33333333-4");

    @Test
    void authenticatesConcurrentlyWithoutDuplicateRemoteCalls() throws Exception {
        var clock = new FixedClock(NOW);
        var ticketCache = new InMemoryTicketCache(clock);
        var traGenerator = new TraGenerator(clock);
        
        TraSigner mockSigner = xml -> "signed-payload";
        var config = new ArcaConfig(CUIT, ArcaEnvironment.HOMOLOGACION, Duration.ofSeconds(5), Duration.ofSeconds(5));
        var loginCmsClient = new LoginCmsClient(config, "http://localhost:12345/ws/services/LoginCms");

        var token = "token-123";
        var sign = "sign-456";
        var successResponse = """
            <loginTicketReturn version="1.0">
              <header>
                <uniqueId>9999</uniqueId>
                <generationTime>2026-07-07T09:00:00-03:00</generationTime>
                <expirationTime>2026-07-07T21:00:00-03:00</expirationTime>
              </header>
              <credentials>
                <token>%s</token>
                <sign>%s</sign>
              </credentials>
            </loginTicketReturn>
            """.formatted(token, sign);

        var remoteCallCount = new AtomicInteger(0);

        injectSoapPort(loginCmsClient, request -> {
            remoteCallCount.incrementAndGet();
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return successResponse;
        });

        var provider = new DefaultAuthProvider(ticketCache, traGenerator, mockSigner, loginCmsClient);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Callable<ArcaAccessTicket>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                startLatch.await();
                return provider.authenticate(SERVICE);
            });
        }

        List<Future<ArcaAccessTicket>> futures = new ArrayList<>();
        for (var task : tasks) {
            futures.add(executor.submit(task));
        }

        startLatch.countDown();

        for (var future : futures) {
            ArcaAccessTicket ticket = future.get(3, TimeUnit.SECONDS);
            assertThat(ticket.token()).isEqualTo(token);
            assertThat(ticket.sign()).isEqualTo(sign);
        }

        executor.shutdown();
        
        assertThat(remoteCallCount.get()).isEqualTo(1);
    }

    private void injectSoapPort(LoginCmsClient client, ArcaSoapPort<String, String> mockPort) throws Exception {
        Field field = LoginCmsClient.class.getDeclaredField("soapPort");
        field.setAccessible(true);
        field.set(client, mockPort);
    }
}
