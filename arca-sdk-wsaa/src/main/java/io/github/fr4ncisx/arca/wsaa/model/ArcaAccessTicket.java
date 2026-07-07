package io.github.fr4ncisx.arca.wsaa.model;

import io.github.fr4ncisx.arca.core.clock.ArcaClock;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.time.Instant;

/**
 * Immutable record that models the Access Ticket (TA) returned by WSAA
 * after a successful authentication.
 * <p>
 * Contains the credentials ({@code token} and {@code sign}) that must be
 * sent to business services, along with generation and expiration timestamps.
 * <p>
 * Implements {@link Comparable} by {@code expirationTime} ascending so that
 * a {@link java.util.PriorityQueue} can order tickets by renewal urgency.
 * <p>
 * Overrides {@code toString()} to redact sensitive fields (token and sign).
 *
 * @param token          the access token credential
 * @param sign           the signature credential
 * @param generationTime when the ticket was generated
 * @param expirationTime when the ticket expires
 * @author fr4ncisx
 * @since 0.1.0-M2
 */
public record ArcaAccessTicket(
        String token,
        String sign,
        Instant generationTime,
        Instant expirationTime) implements Comparable<ArcaAccessTicket> {

    /**
     * Validates that all fields are non-null at construction time.
     *
     * @throws ArcaValidationException if any field is null
     */
    public ArcaAccessTicket {
        if (token == null)
            throw new ArcaValidationException("The access token credential cannot be null.");
        if (sign == null)
            throw new ArcaValidationException("The signature credential cannot be null.");
        if (generationTime == null)
            throw new ArcaValidationException("The generation time of the access ticket cannot be null.");
        if (expirationTime == null)
            throw new ArcaValidationException("The expiration time of the access ticket cannot be null.");
    }

    /**
     * Checks whether this ticket has expired according to the given clock.
     *
     * @param clock the time source to evaluate expiration
     * @return true if expirationTime is before clock.now()
     * @throws ArcaValidationException if clock is null
     */
    public boolean isExpired(ArcaClock clock) {
        if (clock == null)
            throw new ArcaValidationException("The clock instance to check expiration cannot be null.");
        return expirationTime.isBefore(clock.now());
    }

    /**
     * Compares tickets by expiration time ascending (most urgent first).
     * Suitable for {@link java.util.PriorityQueue} ordering.
     *
     * @param other the ticket to compare against
     * @return negative if this expires first, positive if other expires first
     * @throws ArcaValidationException if other is null
     */
    @Override
    public int compareTo(ArcaAccessTicket other) {
        if (other == null)
            throw new ArcaValidationException("The other ArcaAccessTicket instance to compare against cannot be null.");
        return this.expirationTime.compareTo(other.expirationTime);
    }

    /**
     * Returns a redacted string representation of this ticket.
     * <p>
     * The {@code token} and {@code sign} fields are masked to prevent accidental
     * exposure in logs. Short values (six characters or fewer) are replaced
     * entirely with {@code ***}; longer values retain the first and last three
     * characters with {@code ***} in between.
     *
     * @return a human-readable, redacted representation of this ticket.
     */
    @Override
    public String toString() {
        return "ArcaAccessTicket[token=%s, sign=%s, generationTime=%s, expirationTime=%s]"
                .formatted(mask(token), mask(sign), generationTime, expirationTime);
    }

    private static String mask(String value) {
        if (value == null || value.isBlank())
            return "[REDACTED]";

        if (value.length() <= 6)
            return "***";

        return value.substring(0, 3)
                + "***"
                + value.substring(value.length() - 3);
    }
}
