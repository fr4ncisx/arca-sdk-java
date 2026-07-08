package io.github.fr4ncisx.arca.wsaa.internal.auth;

import io.github.fr4ncisx.arca.core.clock.ArcaClock;
import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsaa.internal.cache.TicketCache;
import io.github.fr4ncisx.arca.wsaa.internal.client.LoginCmsClient;
import io.github.fr4ncisx.arca.wsaa.internal.cms.CmsSigner;
import io.github.fr4ncisx.arca.wsaa.internal.tra.TraGenerator;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Standard implementation of {@link AuthProvider} with caching, lazy initialization,
 * and concurrent request collision control.
 * <p>
 * This orchestrator manages the authentication lifecycle for WSAA. It coordinates
 * ticket retrieval from cache, ticket generation, cryptographic CMS signing, and
 * WSAA SOAP service invocation.
 * <p>
 * In high-concurrency scenarios, multiple threads requesting tickets for the same
 * service will coordinate so that only one remote request is sent, with all other
 * threads waiting for the result.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public final class DefaultAuthProvider implements AuthProvider {

    private static final Duration DEFAULT_TTL = Duration.ofHours(12);

    private final TicketCache ticketCache;
    private final TraGenerator traGenerator;
    private final CmsSigner cmsSigner;
    private final LoginCmsClient loginCmsClient;
    private final ConcurrentHashMap<String, PendingLogin> activeLogins = new ConcurrentHashMap<>();

    /**
     * Creates a new DefaultAuthProvider with the required collaborators.
     *
     * @param ticketCache    the cache used to store and retrieve valid tickets
     * @param traGenerator   the generator for TRA XML requests
     * @param cmsSigner      the signer for generating CMS payloads
     * @param loginCmsClient the SOAP client to communicate with WSAA
     * @throws ArcaValidationException if any collaborator is null
     */
    public DefaultAuthProvider(
            TicketCache ticketCache,
            TraGenerator traGenerator,
            CmsSigner cmsSigner,
            LoginCmsClient loginCmsClient) {
        if (ticketCache == null) {
            throw new ArcaValidationException("ticketCache must not be null");
        }
        if (traGenerator == null) {
            throw new ArcaValidationException("traGenerator must not be null");
        }
        if (cmsSigner == null) {
            throw new ArcaValidationException("cmsSigner must not be null");
        }
        if (loginCmsClient == null) {
            throw new ArcaValidationException("loginCmsClient must not be null");
        }
        this.ticketCache = ticketCache;
        this.traGenerator = traGenerator;
        this.cmsSigner = cmsSigner;
        this.loginCmsClient = loginCmsClient;
    }

    @Override
    public ArcaAccessTicket authenticate(String service) throws ArcaAuthException, ArcaSoapException {
        if (service == null || service.trim().isEmpty()) {
            throw new ArcaValidationException("service must not be blank");
        }

        String sanitizedService = service.trim();

        // 1. Initial check against cache (no lock acquired)
        Optional<ArcaAccessTicket> cached = ticketCache.get(sanitizedService);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 2. Coordinate concurrent login requests using a atomic map check
        PendingLogin newLogin = new PendingLogin();
        PendingLogin activeLogin = activeLogins.putIfAbsent(sanitizedService, newLogin);

        if (activeLogin == null) {
            // This thread is the initiator
            try {
                // Double check cache inside the synchronized-like boundary
                Optional<ArcaAccessTicket> doubleChecked = ticketCache.get(sanitizedService);
                if (doubleChecked.isPresent()) {
                    newLogin.ticket = doubleChecked.get();
                } else {
                    String tra = traGenerator.generate(sanitizedService, DEFAULT_TTL);
                    String cms = cmsSigner.sign(tra);
                    newLogin.ticket = loginCmsClient.loginCms(cms);
                    ticketCache.put(sanitizedService, newLogin.ticket);
                }
            } catch (Throwable t) {
                newLogin.error = t;
            } finally {
                activeLogins.remove(sanitizedService);
                newLogin.latch.countDown();
            }
            activeLogin = newLogin;
        } else {
            // This thread is a waiter, wait for the initiator to complete
            try {
                activeLogin.latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ArcaAuthException("Thread was interrupted while waiting for authentication", e);
            }
        }

        // 3. Handle result propagation
        Throwable error = activeLogin.error;
        if (error != null) {
            if (error instanceof ArcaAuthException e) {
                throw e;
            }
            if (error instanceof ArcaSoapException e) {
                throw e;
            }
            throw new ArcaAuthException("Authentication failed unexpectedly: " + error.getMessage(), error);
        }

        ArcaAccessTicket ticket = activeLogin.ticket;
        if (ticket == null) {
            throw new ArcaAuthException("Authentication succeeded but returned no ticket.");
        }
        return ticket;
    }

    private static final class PendingLogin {
        final CountDownLatch latch = new CountDownLatch(1);
        @Nullable ArcaAccessTicket ticket;
        @Nullable Throwable error;
    }
}
