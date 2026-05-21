package io.github.fr4ncisx.arca.core.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogSanitizer}.
 * <p>
 * Verifies that supported secret formats are redacted while non-sensitive text
 * and structural separators remain unchanged.
 *
 * @author fr4ncisx
 * @since 0.1.0-M2
 */
class LogSanitizerTest {

    /**
     * Validates that a {@code null} input returns {@code null}.
     */
    @Test
    void returnsNullForNullInput() {
        assertThat(LogSanitizer.sanitize(null)).isNull();
    }

    /**
     * Validates that non-sensitive diagnostic text is preserved exactly.
     */
    @Test
    void preservesNonSensitiveTextUnchanged() {
        var input = "status=ok traceId=abc123 retries=2";

        assertThat(LogSanitizer.sanitize(input)).isEqualTo(input);
    }

    /**
     * Validates that sensitive keys are matched without lowercasing the whole input.
     */
    @Test
    void matchesSensitiveKeysCaseInsensitivelyWithoutLowercasingWholeInput() {
        var input = "ToKeN=ABC123 service=WSAA MixedCaseValue";

        assertThat(LogSanitizer.sanitize(input))
            .isEqualTo("ToKeN=[REDACTED] service=WSAA MixedCaseValue");
    }

    /**
     * Validates that token values in {@code key=value} format are redacted.
     */
    @Test
    void redactsTokenInKeyValueFormat() {
        var input = "token=abc123";

        assertThat(LogSanitizer.sanitize(input)).isEqualTo("token=[REDACTED]");
    }

    /**
     * Validates that sign values in {@code key: value} format are redacted.
     */
    @Test
    void redactsSignInColonFormat() {
        var input = "sign: ZXCVBN";

        assertThat(LogSanitizer.sanitize(input)).isEqualTo("sign: [REDACTED]");
    }

    /**
     * Validates that password values in simple JSON are redacted.
     */
    @Test
    void redactsPasswordInJsonFormat() {
        var input = "{\"password\":\"superSecret\",\"env\":\"test\"}";

        assertThat(LogSanitizer.sanitize(input))
            .isEqualTo("{\"password\":\"[REDACTED]\",\"env\":\"test\"}");
    }

    /**
     * Validates that XML attributes containing token and sign are redacted.
     */
    @Test
    void redactsTokenAndSignInXmlAttributes() {
        var input = "<login token=\"abc\" sign='xyz' env=\"test\"/>";

        assertThat(LogSanitizer.sanitize(input))
            .isEqualTo("<login token=\"[REDACTED]\" sign='[REDACTED]' env=\"test\"/>");
    }

    /**
     * Validates that Authorization header values are redacted.
     */
    @Test
    void redactsAuthorizationHeader() {
        var input = "Authorization: Bearer abc.def.ghi";

        assertThat(LogSanitizer.sanitize(input))
            .isEqualTo("Authorization: [REDACTED]");
    }

    /**
     * Validates that Set-Cookie header values are redacted.
     */
    @Test
    void redactsSetCookieHeader() {
        var input = "Set-Cookie=sessionId=abc123; Path=/; HttpOnly";

        assertThat(LogSanitizer.sanitize(input))
            .isEqualTo("Set-Cookie=[REDACTED]; Path=/; HttpOnly");
    }

    /**
     * Validates that private key blocks are fully redacted.
     */
    @Test
    void redactsPrivateKeyBlock() {
        var input = """
            -----BEGIN PRIVATE KEY-----
            abcdef123456
            -----END PRIVATE KEY-----
            """;

        assertThat(LogSanitizer.sanitize(input)).isEqualTo("[REDACTED]\n");
    }

    /**
     * Validates that certificate blocks are fully redacted.
     */
    @Test
    void redactsCertificateBlock() {
        var input = """
            -----BEGIN CERTIFICATE-----
            abcdef123456
            -----END CERTIFICATE-----
            """;

        assertThat(LogSanitizer.sanitize(input)).isEqualTo("[REDACTED]\n");
    }

    /**
     * Validates that key names and separators are preserved while sensitive values are redacted.
     */
    @Test
    void preservesKeyNamesAndSeparatorsWhileRedactingValues() {
        var input = "token = abc123, sign: xyz789";

        assertThat(LogSanitizer.sanitize(input))
            .isEqualTo("token = [REDACTED], sign: [REDACTED]");
    }

    /**
     * Validates that original secret values do not survive sanitization.
     */
    @Test
    void doesNotLeakOriginalSensitiveValuesInSupportedFormats() {
        var input = """
            token=abc123
            sign: xyz789
            {"password":"superSecret"}
            <auth privateKey="raw-private-key"/>
            """;

        var sanitized = LogSanitizer.sanitize(input);

        assertThat(sanitized).doesNotContain("abc123");
        assertThat(sanitized).doesNotContain("xyz789");
        assertThat(sanitized).doesNotContain("superSecret");
        assertThat(sanitized).doesNotContain("raw-private-key");
        assertThat(sanitized).contains("[REDACTED]");
    }
}
