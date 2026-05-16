package io.github.fr4ncisx.arca.wsaa.model;

import io.github.fr4ncisx.arca.core.clock.FixedClock;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArcaAccessTicketTest {

    private static final Instant GEN = Instant.parse("2026-05-15T10:00:00Z");
    private static final Instant EXP = Instant.parse("2026-05-15T15:00:00Z");
    private static final String TOKEN = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz";
    private static final String SIGN = "signed-data-here-abcdef123456789xyz";

    @Test
    void recordHasFourFields() {
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);

        assertThat(ticket.token()).isEqualTo(TOKEN);
        assertThat(ticket.sign()).isEqualTo(SIGN);
        assertThat(ticket.generationTime()).isEqualTo(GEN);
        assertThat(ticket.expirationTime()).isEqualTo(EXP);
    }

    @Test
    void rejectsNullToken() {
        assertThatThrownBy(() -> new ArcaAccessTicket(null, SIGN, GEN, EXP))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("token must not be null");
    }

    @Test
    void rejectsNullSign() {
        assertThatThrownBy(() -> new ArcaAccessTicket(TOKEN, null, GEN, EXP))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("sign must not be null");
    }

    @Test
    void rejectsNullGenerationTime() {
        assertThatThrownBy(() -> new ArcaAccessTicket(TOKEN, SIGN, null, EXP))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("generationTime must not be null");
    }

    @Test
    void rejectsNullExpirationTime() {
        assertThatThrownBy(() -> new ArcaAccessTicket(TOKEN, SIGN, GEN, null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("expirationTime must not be null");
    }

    @Test
    void isExpiredReturnsFalseWhenClockIsBeforeExpiration() {
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);
        var clock = new FixedClock(EXP.minusSeconds(60));

        assertThat(ticket.isExpired(clock)).isFalse();
    }

    @Test
    void isExpiredReturnsTrueWhenClockIsAfterExpiration() {
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);
        var clock = new FixedClock(EXP.plusSeconds(60));

        assertThat(ticket.isExpired(clock)).isTrue();
    }

    @Test
    void isExpiredReturnsFalseWhenClockEqualsExpiration() {
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);
        var clock = new FixedClock(EXP);

        assertThat(ticket.isExpired(clock)).isFalse();
    }

    @Test
    void isExpiredRejectsNullClock() {
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);

        assertThatThrownBy(() -> ticket.isExpired(null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("clock must not be null");
    }

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

    @Test
    void toStringRedactsShortValues() {
        var ticket = new ArcaAccessTicket("short", "tiny", GEN, EXP);
        var result = ticket.toString();

        assertThat(result)
                .doesNotContain("short")
                .doesNotContain("tiny")
                .contains("***");
    }

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

    @Test
    void compareToRejectsNull() {
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);

        assertThatThrownBy(() -> ticket.compareTo(null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("other ticket must not be null");
    }

    @Test
    void equalsAndHashCodeCompareAllFields() {
        var ticket1 = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);
        var ticket2 = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);
        var different = new ArcaAccessTicket("other-token", SIGN, GEN, EXP);

        assertThat(ticket1).isEqualTo(ticket2).hasSameHashCodeAs(ticket2).isNotEqualTo(different);
    }

    @Test
    void equalsReturnsFalseForNullAndDifferentType() {
        var ticket = new ArcaAccessTicket(TOKEN, SIGN, GEN, EXP);

        assertThat(ticket).isNotEqualTo(null);
    }
}
