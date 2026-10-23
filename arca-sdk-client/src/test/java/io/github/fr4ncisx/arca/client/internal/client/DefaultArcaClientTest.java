package io.github.fr4ncisx.arca.client.internal.client;

import io.github.fr4ncisx.arca.core.clock.SystemClock;
import io.github.fr4ncisx.arca.core.clock.FixedClock;
import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.internal.cache.PersistentTicketCache;
import io.github.fr4ncisx.arca.wsaa.internal.cache.TicketCache;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsaa.internal.auth.DefaultAuthProvider;
import io.github.fr4ncisx.arca.wsaa.internal.client.LoginCmsClient;
import io.github.fr4ncisx.arca.wsaa.internal.tra.TraGenerator;
import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.test.support.ArcaMockServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that persistent ticket caches are isolated by taxpayer and environment.
 *
 * @author fr4ncisx
 * @since 1.2.4
 */
class DefaultArcaClientTest {

    private static final Instant GENERATION_TIME = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant EXPIRATION_TIME = Instant.parse("2099-01-01T00:00:00Z");

    @TempDir
    Path ticketCacheRoot;

    /**
     * Verifies that clients sharing a root directory do not share tickets across identities.
     */
    @Test
    void isolatesTicketsByEnvironmentAndCuit() {
        TicketCache productionClient = DefaultArcaClient.createTicketCache(
                config(Cuit.parse("20-33333333-4"), ArcaEnvironment.PRODUCCION));
        TicketCache otherTaxpayer = DefaultArcaClient.createTicketCache(
                config(Cuit.parse("30-12345678-1"), ArcaEnvironment.PRODUCCION));
        TicketCache homologationClient = DefaultArcaClient.createTicketCache(
                config(Cuit.parse("20-33333333-4"), ArcaEnvironment.HOMOLOGACION));

        ArcaAccessTicket productionTicket = ticket("production");
        ArcaAccessTicket otherTicket = ticket("other-taxpayer");
        ArcaAccessTicket homologationTicket = ticket("homologation");
        productionClient.put("wsfe", productionTicket);
        otherTaxpayer.put("wsfe", otherTicket);
        homologationClient.put("wsfe", homologationTicket);

        assertThat(productionClient.get("wsfe")).contains(productionTicket);
        assertThat(otherTaxpayer.get("wsfe")).contains(otherTicket);
        assertThat(homologationClient.get("wsfe")).contains(homologationTicket);

        productionClient.clear();

        assertThat(productionClient.get("wsfe")).isEmpty();
        assertThat(otherTaxpayer.get("wsfe")).contains(otherTicket);
        assertThat(homologationClient.get("wsfe")).contains(homologationTicket);
    }

    /**
     * Verifies that an unscoped cache entry is not reused after cache namespacing.
     */
    @Test
    void ignoresLegacyUnscopedTicketFiles() {
        TicketCache legacyCache = new PersistentTicketCache(ticketCacheRoot, SystemClock.INSTANCE);
        legacyCache.put("wsfe", ticket("legacy"));

        TicketCache scopedCache = DefaultArcaClient.createTicketCache(
                config(Cuit.parse("20-33333333-4"), ArcaEnvironment.PRODUCCION));

        assertThat(scopedCache.get("wsfe")).isEmpty();
        assertThat(legacyCache.get("wsfe")).isPresent();
    }

    /**
     * Rejects legacy reuse, surfaces duplicate-ticket rejection, and reuses the scoped ticket after restart.
     */
    @Test
    void transitionsFromLegacyCacheWithoutReusingAnotherIdentity() {
        TicketCache legacy = new PersistentTicketCache(ticketCacheRoot, SystemClock.INSTANCE);
        legacy.put("wsfe", ticket("legacy"));
        ArcaConfig config = config(Cuit.parse("20-33333333-4"), ArcaEnvironment.HOMOLOGACION);
        TicketCache scoped = DefaultArcaClient.createTicketCache(config);
        try (var server = new ArcaMockServer()) {
            server.start();
            server.stubLoginCmsRawBody("""
                    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                      <soap:Body><soap:Fault><faultcode>coe.alreadyAuthenticated</faultcode>
                        <faultstring>A valid access ticket already exists</faultstring>
                      </soap:Fault></soap:Body>
                    </soap:Envelope>
                    """, 500);
            var login = new LoginCmsClient(config, server.baseUrl() + "/ws/services/LoginCms");
            var generator = new TraGenerator(SystemClock.INSTANCE);
            var auth = new DefaultAuthProvider(scoped, generator, tra -> "QUJD", login);

            assertThatThrownBy(() -> auth.authenticate("wsfe")).isInstanceOf(ArcaAuthException.class);
            assertThat(legacy.get("wsfe")).isPresent();
            assertThat(scoped.get("wsfe")).isEmpty();

            var expiredLegacy = new PersistentTicketCache(ticketCacheRoot,
                    new FixedClock(EXPIRATION_TIME.plusSeconds(1)));
            assertThat(expiredLegacy.get("wsfe")).isEmpty();
            server.reset();
            server.stubLoginCmsRawBody("""
                    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                      <soap:Body><loginCmsResponse><loginCmsReturn><![CDATA[
                        <loginTicketResponse><header>
                          <generationTime>2026-01-01T00:00:00Z</generationTime>
                          <expirationTime>2099-01-01T00:00:00Z</expirationTime>
                        </header><credentials><token>fresh-dummy-token</token><sign>dummy-sign</sign></credentials>
                        </loginTicketResponse>
                      ]]></loginCmsReturn></loginCmsResponse></soap:Body>
                    </soap:Envelope>
                    """, 200);
            ArcaAccessTicket fresh = auth.authenticate("wsfe");
            assertThat(scoped.get("wsfe")).contains(fresh);

            server.reset();
            server.stubLoginCmsRawBody("<invalid/>", 500);
            var restarted = new DefaultAuthProvider(DefaultArcaClient.createTicketCache(config),
                    generator, tra -> "QUJD", login);
            assertThat(restarted.authenticate("wsfe")).isEqualTo(fresh);
        }
    }

    private ArcaConfig config(Cuit cuit, ArcaEnvironment environment) {
        return new ArcaConfig(cuit, environment, Duration.ofSeconds(1), Duration.ofSeconds(1))
                .withTicketCacheDir(ticketCacheRoot);
    }

    private ArcaAccessTicket ticket(String token) {
        return new ArcaAccessTicket(token, token + "-sign", GENERATION_TIME, EXPIRATION_TIME);
    }
}
