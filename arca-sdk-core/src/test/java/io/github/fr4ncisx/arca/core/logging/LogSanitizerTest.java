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
     * Validates that XML elements containing token, sign, and password values are redacted.
     */
    @Test
    void redactsSensitiveXmlElementText() {
        var input = "<login><token>abc</token><sign>xyz</sign><password>secret</password><env>test</env></login>";

        assertThat(LogSanitizer.sanitize(input))
            .isEqualTo("<login><token>[REDACTED]</token><sign>[REDACTED]</sign>"
                + "<password>[REDACTED]</password><env>test</env></login>");
    }

    /**
     * Validates that namespaced XML elements are evaluated by their local name.
     */
    @Test
    void redactsNamespacedSensitiveXmlElementText() {
        var input = "<wsaa:token>abc</wsaa:token><wsaa:sign>xyz</wsaa:sign>";

        assertThat(LogSanitizer.sanitize(input))
            .isEqualTo("<wsaa:token>[REDACTED]</wsaa:token><wsaa:sign>[REDACTED]</wsaa:sign>");
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
     * Validates that Authorization values inside XML text are redacted without removing tags.
     */
    @Test
    void redactsAuthorizationHeaderInsideXmlText() {
        var input = "<faultstring>Authorization: Bearer abc.def.ghi</faultstring>";

        assertThat(LogSanitizer.sanitize(input))
            .isEqualTo("<faultstring>Authorization: [REDACTED]</faultstring>");
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

        assertThat(sanitized)
            .doesNotContain("abc123")
            .doesNotContain("xyz789")
            .doesNotContain("superSecret")
            .doesNotContain("raw-private-key")
            .contains("[REDACTED]");
    }

    /**
     * Validates that a trailing line break is preserved when a full sensitive block is redacted.
     */
    @Test
    void preservesTrailingNewlineWhenRedactingSensitiveBlock() {
        var input = """
            -----BEGIN CERTIFICATE-----
            abcdef123456
            -----END CERTIFICATE-----
            """;

        assertThat(LogSanitizer.sanitize(input)).endsWith("\n");
    }

    /**
     * Validates that an already redacted value keeps its separators unchanged.
     */
    @Test
    void preservesSeparatorsForAlreadyRedactedValues() {
        var input = "token=[REDACTED], sign: [REDACTED]";

        assertThat(LogSanitizer.sanitize(input))
            .isEqualTo("token=[REDACTED], sign: [REDACTED]");
    }

    /**
     * Validates that non-sensitive fields remain unchanged when they appear next to sensitive ones.
     */
    @Test
    void preservesNonSensitiveFieldsAdjacentToSensitiveFields() {
        var input = "token=abc123 traceId=req-42 env=test";

        assertThat(LogSanitizer.sanitize(input))
            .isEqualTo("token=[REDACTED] traceId=req-42 env=test");
    }

    /**
     * Validates that case-insensitive matching only applies to the key, not to the value.
     */
    @Test
    void preservesSensitiveValueLetterCaseByRedactingInsteadOfNormalizingInput() {
        var input = "ToKeN=AbC123XyZ";

        assertThat(LogSanitizer.sanitize(input)).isEqualTo("ToKeN=[REDACTED]");
    }

    /**
     * Validates that JSON and delimited pairs can coexist without interfering with each other.
     */
    @Test
    void sanitizesJsonAndDelimitedPairsInTheSameInput() {
        var input = """
            Authorization: Bearer abc.def
            {"password":"superSecret","env":"test"}
            token=abc123
            """;

        assertThat(LogSanitizer.sanitize(input)).isEqualTo("""
            Authorization: [REDACTED]
            {"password":"[REDACTED]","env":"test"}
            token=[REDACTED]
            """);
    }
}
