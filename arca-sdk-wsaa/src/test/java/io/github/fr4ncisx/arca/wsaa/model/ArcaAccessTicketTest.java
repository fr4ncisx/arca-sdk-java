package io.github.fr4ncisx.arca.wsaa.model;

import io.github.fr4ncisx.arca.core.clock.FixedClock;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ArcaAccessTicket} record validation, expiration logic,
 * comparison ordering, and sensitive data redaction.
 * <p>
 * Verifies that null fields are rejected, expiration is evaluated correctly
 * against an injected clock, and toString() masks token and sign values.
 *
 * @author fr4ncisx
 * @since 0.1.0-M2
 */
@NullMarked
class ArcaAccessTicketTest {

    private static final Instant GEN = Instant.parse("2026-05-15T10:00:00Z");
    private static final Instant EXP = Instant.parse("2026-05-15T15:00:00Z");
    private static final String TOKEN = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz";
    private static final String SIGN = "signed-data-here-abcdef123456789xyz";

    /**
     * Verifies that all four record fields are accessible and return the
     * values passed at construction time.
     */
    @Test
    void recordHasFourFields() {
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);

        assertThat(ticket.token()).isEqualTo(TOKEN);
        assertThat(ticket.sign()).isEqualTo(SIGN);
        assertThat(ticket.generationTime()).isEqualTo(GEN);
        assertThat(ticket.expirationTime()).isEqualTo(EXP);
    }

    /**
     * Validates that a null token is rejected with ArcaValidationException.
     */
    @Test
    void rejectsNullToken() {
        assertThatThrownBy(() -> new ArcaAccessTicket(null, SIGN, GEN, EXP))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("The access token credential cannot be null.");
    }

    /**
     * Validates that a null sign is rejected with ArcaValidationException.
     */
    @Test
    void rejectsNullSign() {
        assertThatThrownBy(() -> new ArcaAccessTicket(TOKEN, null, GEN, EXP))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("The signature credential cannot be null.");
    }

    /**
     * Validates that a null generationTime is rejected with ArcaValidationException.
     */
    @Test
    void rejectsNullGenerationTime() {
        assertThatThrownBy(() -> new ArcaAccessTicket(TOKEN, SIGN, null, EXP))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("The generation time of the access ticket cannot be null.");
    }

    /**
     * Validates that a null expirationTime is rejected with ArcaValidationException.
     */
    @Test
    void rejectsNullExpirationTime() {
        assertThatThrownBy(() -> new ArcaAccessTicket(TOKEN, SIGN, GEN, null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("The expiration time of the access ticket cannot be null.");
    }

    /**
     * Verifies that isExpired returns false when the clock time is before
     * the ticket expiration time.
     */
    @Test
    void isExpiredReturnsFalseWhenClockIsBeforeExpiration() {
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);
        var clock = new FixedClock(EXP.minusSeconds(60));

        assertThat(ticket.isExpired(clock)).isFalse();
    }

    /**
     * Verifies that isExpired returns true when the clock time is after
     * the ticket expiration time.
     */
    @Test
    void isExpiredReturnsTrueWhenClockIsAfterExpiration() {
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);
        var clock = new FixedClock(EXP.plusSeconds(60));

        assertThat(ticket.isExpired(clock)).isTrue();
    }

    /**
     * Verifies that isExpired returns false when the clock time exactly
     * equals the expiration time (not yet expired at the boundary).
     */
    @Test
    void isExpiredReturnsFalseWhenClockEqualsExpiration() {
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);
        var clock = new FixedClock(EXP);

        assertThat(ticket.isExpired(clock)).isFalse();
    }

    /**
     * Validates that passing a null clock to isExpired throws
     * ArcaValidationException.
     */
    @Test
    void isExpiredRejectsNullClock() {
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);

        assertThatThrownBy(() -> ticket.isExpired(null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("The clock instance to check expiration cannot be null.");
    }

    /**
     * Verifies that toString() redacts the token and sign values,
     * showing only masked portions to prevent credential leakage in logs.
     */
    @Test
    void toStringDoesNotExposeTokenOrSign() {
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);
        var result = ticket.toString();

        assertThat(result)
                .doesNotContain(TOKEN)
                .doesNotContain(SIGN)
                .contains("abc***4yz")
                .contains("sig***xyz")
                .contains("generationTime=")
                .contains("expirationTime=");
    }

    /**
     * Verifies that toString() fully redacts values that are too short
     * to show a meaningful masked prefix and suffix.
     */
    @Test
    void toStringRedactsShortValues() {
        var ticket = new ArcaAccessTicket("short", "tiny", GEN, EXP);
        var result = ticket.toString();

        assertThat(result)
                .doesNotContain("short")
                .doesNotContain("tiny")
                .contains("***");
    }

    /**
     * Verifies that compareTo orders tickets by expiration time ascending,
     * so the most urgent (earliest expiring) ticket comes first.
     */
    @Test
    void compareToOrdersByExpirationTimeAscending() {
        var early = new ArcaAccessTicket(TOKEN, SIGN, GEN, Instant.parse("2026-05-15T12:00:00Z"));
        var middle = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);
        var late = new ArcaAccessTicket(TOKEN, SIGN, GEN, Instant.parse("2026-05-15T18:00:00Z"));

        var sorted = Stream.of(late, early, middle)
                .sorted()
                .toList();

        assertThat(sorted).containsExactly(early, middle, late);
    }

    /**
     * Validates that compareTo throws ArcaValidationException when
     * comparing against a null ticket.
     */
    @Test
    void compareToRejectsNull() {
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);

        assertThatThrownBy(() -> ticket.compareTo(null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessage("The other ArcaAccessTicket instance to compare against cannot be null.");
    }

    /**
     * Verifies that equals and hashCode consider all four fields,
     * so two tickets with identical values are equal.
     */
    @Test
    void equalsAndHashCodeCompareAllFields() {
        var ticket1 = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);
        var ticket2 = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);
        var different = new ArcaAccessTicket("other-token", SIGN, GEN, EXP);

        assertThat(ticket1).isEqualTo(ticket2).hasSameHashCodeAs(ticket2).isNotEqualTo(different);
    }

    /**
     * Verifies that equals returns false when compared against null.
     */
    @Test
    void equalsReturnsFalseForNullAndDifferentType() {
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);

        assertThat(ticket).isNotEqualTo(null);
    }
}
