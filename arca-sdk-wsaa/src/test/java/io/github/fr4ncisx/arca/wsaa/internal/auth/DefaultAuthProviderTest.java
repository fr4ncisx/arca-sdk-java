package io.github.fr4ncisx.arca.wsaa.internal.auth;

import io.github.fr4ncisx.arca.core.clock.FixedClock;
import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.cache.InMemoryTicketCache;
import io.github.fr4ncisx.arca.wsaa.internal.cache.TicketCache;
import io.github.fr4ncisx.arca.wsaa.internal.client.LoginCmsClient;
import io.github.fr4ncisx.arca.wsaa.internal.cms.CmsSigner;
import io.github.fr4ncisx.arca.wsaa.internal.tra.TraGenerator;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsaa.spi.CertificateSource;
import io.github.fr4ncisx.arca.wsaa.spi.TraSigner;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DefaultAuthProvider} including cache orchestration,
 * lazy-initialization, and concurrent renewal coordination.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
@SuppressWarnings("null")
class DefaultAuthProviderTest {

    private static final String SERVICE = "wsfe";
    private static final Instant NOW = Instant.parse("2026-07-07T10:00:00Z");
    private static final Cuit CUIT = Cuit.parse("20-33333333-4");

    private TicketCache ticketCache;
    private TraGenerator traGenerator;
    private TraSigner cmsSigner;
    private LoginCmsClient loginCmsClient;
    private ArcaConfig config;

    @BeforeAll
    static void setUpAll() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        var clock = new FixedClock(NOW);
        ticketCache = new InMemoryTicketCache(clock);
        traGenerator = new TraGenerator(clock);

        var keyStore = createKeyStore("CN=Test, SERIALNUMBER=20333333334");
        CertificateSource certSource = () -> keyStore;
        cmsSigner = new CmsSigner(certSource, "password".toCharArray());

        config = new ArcaConfig(CUIT, ArcaEnvironment.HOMOLOGACION, Duration.ofSeconds(5), Duration.ofSeconds(5));
        loginCmsClient = new LoginCmsClient(config, "http://localhost:12345/ws/services/LoginCms");
    }

    @Test
    void constructorRejectsNulls() {
        assertThatThrownBy(() -> new DefaultAuthProvider(null, traGenerator, cmsSigner, loginCmsClient))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> new DefaultAuthProvider(ticketCache, null, cmsSigner, loginCmsClient))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> new DefaultAuthProvider(ticketCache, traGenerator, null, loginCmsClient))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> new DefaultAuthProvider(ticketCache, traGenerator, cmsSigner, null))
                .isInstanceOf(ArcaValidationException.class);
    }

    @Test
    void authenticateRejectsBlankService() {
        var provider = new DefaultAuthProvider(ticketCache, traGenerator, cmsSigner, loginCmsClient);
        assertThatThrownBy(() -> provider.authenticate(null))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> provider.authenticate("   "))
                .isInstanceOf(ArcaValidationException.class);
    }

    @Test
    void returnsCachedTicketDirectlyOnCacheHit() throws Exception {
        var cachedTicket = new ArcaAccessTicket("cached-token", "cached-sign", NOW, NOW.plusSeconds(3600));
        ticketCache.put(SERVICE, cachedTicket);

        // Injecting throwing port to verify that LoginCmsClient is NOT called
        injectSoapPort(loginCmsClient, request -> {
            throw new RuntimeException("Should not be called!");
        });

        var provider = new DefaultAuthProvider(ticketCache, traGenerator, cmsSigner, loginCmsClient);
        var result = provider.authenticate(SERVICE);

        assertThat(result).isSameAs(cachedTicket);
    }

    @Test
    void requestsNewTicketAndPopulatesCacheOnCacheMiss() throws Exception {
        var token = "mock-token";
        var sign = "mock-sign";
        var xmlResponse = createSuccessXml(token, sign);

        injectSoapPort(loginCmsClient, request -> xmlResponse);

        var provider = new DefaultAuthProvider(ticketCache, traGenerator, cmsSigner, loginCmsClient);
        var ticket = provider.authenticate(SERVICE);

        assertThat(ticket).isNotNull();
        assertThat(ticket.token()).isEqualTo(token);
        assertThat(ticket.sign()).isEqualTo(sign);

        // Cache must be populated
        var inCache = ticketCache.get(SERVICE);
        assertThat(inCache).isPresent();
        assertThat(inCache.get()).isEqualTo(ticket);
    }

    @Test
    void coordinatesConcurrentRequestsToPreventCollision() throws Exception {
        var token = "concurrent-token";
        var sign = "concurrent-sign";
        var xmlResponse = createSuccessXml(token, sign);
        var callCount = new AtomicInteger(0);

        injectSoapPort(loginCmsClient, request -> {
            callCount.incrementAndGet();
            sleep(100); // Simulate network latency
            return xmlResponse;
        });

        var provider = new DefaultAuthProvider(ticketCache, traGenerator, cmsSigner, loginCmsClient);

        int threadCount = 5;
        var executor = Executors.newFixedThreadPool(threadCount);
        var startLatch = new CountDownLatch(1);
        var futures = new ArrayList<Future<ArcaAccessTicket>>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                return provider.authenticate(SERVICE);
            }));
        }

        startLatch.countDown(); // Trigger all threads

        for (Future<ArcaAccessTicket> f : futures) {
            var ticket = f.get(2, TimeUnit.SECONDS);
            assertThat(ticket.token()).isEqualTo(token);
        }

        executor.shutdown();

        // Exactly one remote JAX-WS call must have been executed
        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    void propagatesSameExceptionToAllConcurrentWaitersOnFailure() throws Exception {
        var callCount = new AtomicInteger(0);

        injectSoapPort(loginCmsClient, request -> {
            callCount.incrementAndGet();
            sleep(100);
            throw new ArcaSoapException("WSAA is down", new IOException("Connection refused"));
        });

        var provider = new DefaultAuthProvider(ticketCache, traGenerator, cmsSigner, loginCmsClient);

        int threadCount = 5;
        var executor = Executors.newFixedThreadPool(threadCount);
        var startLatch = new CountDownLatch(1);
        var futures = new ArrayList<Future<ArcaAccessTicket>>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                return provider.authenticate(SERVICE);
            }));
        }

        startLatch.countDown();

        for (Future<ArcaAccessTicket> f : futures) {
            assertThatThrownBy(f::get)
                    .hasCauseInstanceOf(ArcaSoapException.class)
                    .hasMessageContaining("WSAA is down");
        }

        executor.shutdown();
        assertThat(callCount.get()).isEqualTo(1);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void injectSoapPort(LoginCmsClient client, ArcaSoapPort<String, String> mockPort) throws Exception {
        Field field = LoginCmsClient.class.getDeclaredField("soapPort");
        field.setAccessible(true);
        field.set(client, mockPort);
    }

    private static String createSuccessXml(String token, String sign) {
        return """
            <loginTicketReturn version="1.0">
              <header>
                <uniqueId>12345</uniqueId>
                <generationTime>2026-07-07T09:00:00-03:00</generationTime>
                <expirationTime>2026-07-07T21:00:00-03:00</expirationTime>
              </header>
              <credentials>
                <token>%s</token>
                <sign>%s</sign>
              </credentials>
            </loginTicketReturn>
            """.formatted(token, sign);
    }

    private static KeyStore createKeyStore(String dn) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        X500Name issuer = new X500Name(dn);
        X500Name subject = new X500Name(dn);
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24);
        Date notAfter = new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365);

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, subject, keyPair.getPublic()
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());

        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(builder.build(signer));

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("wsaa", keyPair.getPrivate(), "password".toCharArray(), new Certificate[]{cert});
        return keyStore;
    }
}
