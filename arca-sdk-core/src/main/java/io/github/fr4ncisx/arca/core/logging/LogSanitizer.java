package io.github.fr4ncisx.arca.core.logging;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redacts sensitive values from diagnostic text before it is written to logs or
 * error messages.
 * <p>
 * The sanitizer preserves non-sensitive text and structural separators while
 * replacing supported secret values with {@code [REDACTED]}.
 *
 * @author fr4ncisx
 * @since 0.1.0-M2
 */
public final class LogSanitizer {

    private static final String REDACTED = "[REDACTED]";

    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "token",
        "sign",
        "password",
        "passwd",
        "pwd",
        "secret",
        "privatekey",
        "cert",
        "certificate",
        "keystorepassword",
        "pkcs12password",
        "authorization",
        "set-cookie");

    private static final Pattern SENSITIVE_BLOCK_PATTERN = Pattern.compile(
        """
        (?is)-{5}BEGIN[^\\r\\n]*?(PRIVATE KEY|CERTIFICATE)[^\\r\\n]*-{5}.*?
        -{5}END[^\\r\\n]*?(PRIVATE KEY|CERTIFICATE)[^\\r\\n]*-{5}(\\r?\\n)?\
        """);

    private static final Pattern XML_ATTRIBUTE_PATTERN = Pattern.compile(
        "([A-Z_][\\w.:-]*)(\\s*=\\s*)([\"'])(.*?)\\3",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern JSON_STRING_PATTERN = Pattern.compile(
        "(\"([^\"]+)\")(\\s*:\\s*)(\")([^\"]*)(\")");

    private static final Pattern KEY_VALUE_EQUALS_PATTERN = Pattern.compile(
        "(^|[\\s,;])([A-Z][A-Z\\d-]*)(\\s*=\\s*)((?![\"'])[^\\s,;]+)",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern KEY_VALUE_COLON_PATTERN = Pattern.compile(
        "(^|[\\s,;])([A-Z][A-Z\\d-]*)(\\s*:\\s*)([^\\r\\n,;]+)",
        Pattern.CASE_INSENSITIVE);

    /**
     * Redacts supported secret values from the provided diagnostic text.
     *
     * @param input the diagnostic text to sanitize.
     * @return {@code null} when {@code input} is {@code null}; otherwise the
     *         sanitized text.
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }

        var sanitized = SENSITIVE_BLOCK_PATTERN.matcher(input)
            .replaceAll(matchResult -> REDACTED + (matchResult.group(3) == null ? "" : matchResult.group(3)));
        sanitized = replaceXmlAttributes(sanitized);
        sanitized = replaceJsonPairs(sanitized);
        sanitized = replaceDelimitedPairs(sanitized, KEY_VALUE_EQUALS_PATTERN);
        sanitized = replaceDelimitedPairs(sanitized, KEY_VALUE_COLON_PATTERN);
        return sanitized;
    }

    private LogSanitizer() {
    }

    private static String replaceXmlAttributes(String input) {
        var matcher = XML_ATTRIBUTE_PATTERN.matcher(input);
        var result = new StringBuilder();

        while (matcher.find()) {
            var key = matcher.group(1);
            var replacement = isSensitiveKey(key)
                ? matcher.group(1) + matcher.group(2) + matcher.group(3) + REDACTED + matcher.group(3)
                : matcher.group(0);
            matcher.appendReplacement(result, MatcherEscaper.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceJsonPairs(String input) {
        var matcher = JSON_STRING_PATTERN.matcher(input);
        var result = new StringBuilder();

        while (matcher.find()) {
            var key = matcher.group(2);
            var replacement = isSensitiveKey(key)
                ? matcher.group(1) + matcher.group(3) + matcher.group(4) + REDACTED + matcher.group(6)
                : matcher.group(0);
            matcher.appendReplacement(result, MatcherEscaper.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceDelimitedPairs(String input, Pattern pattern) {
        var matcher = pattern.matcher(input);
        var result = new StringBuilder();

        while (matcher.find()) {
            var prefix = matcher.group(1);
            var key = matcher.group(2);
            var separator = matcher.group(3);
            var value = matcher.group(4);

            var replacement = isSensitiveKey(key)
                ? prefix + key + separator + REDACTED
                : prefix + key + separator + value;
            matcher.appendReplacement(result, MatcherEscaper.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private static boolean isSensitiveKey(String key) {
        return SENSITIVE_KEYS.contains(key.toLowerCase());
    }

    private static final class MatcherEscaper {

        private MatcherEscaper() {
        }

        private static String quoteReplacement(String value) {
            return Matcher.quoteReplacement(value);
        }
    }
}
