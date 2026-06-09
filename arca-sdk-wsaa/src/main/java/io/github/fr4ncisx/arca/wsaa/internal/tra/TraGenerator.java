package io.github.fr4ncisx.arca.wsaa.internal.tra;

import io.github.fr4ncisx.arca.core.clock.ArcaClock;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Generates the WSAA Ticket Request Access XML used before CMS signing.
 * <p>
 * This component is internal to the WSAA module and only builds the local XML
 * payload. It does not sign the payload and does not invoke the remote LoginCms
 * operation.
 *
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
public final class TraGenerator {

    private static final ZoneId ARGENTINA_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final long UNSIGNED_INT_MODULUS = 4_294_967_296L;

    private final ArcaClock clock;

    /**
     * Creates a generator that obtains TRA timestamps from the supplied clock.
     *
     * @param clock the clock used to derive generation and expiration times
     * @throws ArcaValidationException if clock is null
     */
    public TraGenerator(ArcaClock clock) {
        if (clock == null) {
            throw new ArcaValidationException("clock must not be null");
        }
        this.clock = clock;
    }

    /**
     * Generates a well-formed WSAA TRA XML document for the requested service.
     *
     * @param service the WSAA service identifier requested by the ticket
     * @param ttl the ticket request time-to-live
     * @return the generated TRA XML document
     * @throws ArcaValidationException if service is blank or ttl is not positive
     */
    public String generate(String service, Duration ttl) {
        validateService(service);
        validateTtl(ttl);

        Instant generationInstant = clock.now();
        Instant expirationInstant = generationInstant.plus(ttl);
        String uniqueId = uniqueId(generationInstant);

        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <loginTicketRequest version="1.0">
              <header>
                <uniqueId>%s</uniqueId>
                <generationTime>%s</generationTime>
                <expirationTime>%s</expirationTime>
              </header>
              <service>%s</service>
            </loginTicketRequest>
            """.formatted(
            uniqueId,
            format(generationInstant),
            format(expirationInstant),
            escapeXml(service.trim())
        );
    }

    private static void validateService(String service) {
        if (service == null || service.trim().isEmpty()) {
            throw new ArcaValidationException("service must not be blank");
        }
    }

    private static void validateTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new ArcaValidationException("ttl must be positive");
        }
    }

    private static String uniqueId(Instant instant) {
        long epochSeconds = instant.getEpochSecond();
        return Long.toString(Math.floorMod(epochSeconds, UNSIGNED_INT_MODULUS));
    }

    private static String format(Instant instant) {
        return DATE_TIME_FORMATTER.format(instant.atZone(ARGENTINA_ZONE));
    }

    private static String escapeXml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
