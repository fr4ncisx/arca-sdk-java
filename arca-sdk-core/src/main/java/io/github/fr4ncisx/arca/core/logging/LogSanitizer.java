package io.github.fr4ncisx.arca.core.logging;

import java.util.Set;
import java.util.function.Function;
import java.util.regex.MatchResult;
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
    private static final String EMPTY = "";

    private static final int GROUP_MATCH = 0;
    private static final int GROUP_TRAILING_NEWLINE = 3;

    private static final int XML_GROUP_KEY = 1;
    private static final int XML_GROUP_SEPARATOR = 2;
    private static final int XML_GROUP_QUOTE = 3;

    private static final int JSON_GROUP_QUOTED_KEY = 1;
    private static final int JSON_GROUP_KEY = 2;
    private static final int JSON_GROUP_SEPARATOR = 3;
    private static final int JSON_GROUP_VALUE_QUOTE_START = 4;
    private static final int JSON_GROUP_VALUE_QUOTE_END = 6;

    private static final int DELIMITED_GROUP_PREFIX = 1;
    private static final int DELIMITED_GROUP_KEY = 2;
    private static final int DELIMITED_GROUP_SEPARATOR = 3;
    private static final int DELIMITED_GROUP_VALUE = 4;

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

    private static final Pattern XML_ELEMENT_PATTERN = Pattern.compile(
        "(<([A-Z_][\\w.:-]*)(?:\\s[^>]*)?>)([^<>]*)(</\\2>)",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern JSON_STRING_PATTERN = Pattern.compile(
        "(\"([^\"]+)\")(\\s*:\\s*)(\")([^\"]*)(\")");

    private static final Pattern KEY_VALUE_EQUALS_PATTERN = Pattern.compile(
        "(^|[\\s,;>])([A-Z][A-Z\\d-]*)(\\s*=\\s*)((?![\"'])[^\\s,;<]+)",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern KEY_VALUE_COLON_PATTERN = Pattern.compile(
        "(^|[\\s,;>])([A-Z][A-Z\\d-]*)(\\s*:\\s*)([^\\r\\n,;<]+)",
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

        var sanitized = redactSensitiveBlocks(input);
        sanitized = replaceXmlAttributes(sanitized);
        sanitized = replaceXmlElements(sanitized);
        sanitized = replaceJsonPairs(sanitized);
        sanitized = replaceDelimitedPairs(sanitized, KEY_VALUE_EQUALS_PATTERN);
        sanitized = replaceDelimitedPairs(sanitized, KEY_VALUE_COLON_PATTERN);
        return sanitized;
    }

    private LogSanitizer() {
    }

    /**
     * Redacts full sensitive blocks and preserves a trailing line separator when present.
     *
     * @param input the diagnostic text to sanitize.
     * @return the input with certificate and private key blocks replaced.
     */
    private static String redactSensitiveBlocks(String input) {
        return replaceMatches(input, SENSITIVE_BLOCK_PATTERN,
            match -> REDACTED + defaultString(match.group(GROUP_TRAILING_NEWLINE)));
    }

    /**
     * Redacts supported sensitive XML attribute values while preserving the original quotes.
     *
     * @param input the diagnostic text to sanitize.
     * @return the input with sensitive XML attribute values replaced.
     */
    private static String replaceXmlAttributes(String input) {
        return replaceMatches(input, XML_ATTRIBUTE_PATTERN, match -> {
            var key = match.group(XML_GROUP_KEY);
            if (isSensitiveKey(key)) {
                var separator = match.group(XML_GROUP_SEPARATOR);
                var quote = match.group(XML_GROUP_QUOTE);
                return key + separator + quote + REDACTED + quote;
            }

            return match.group(GROUP_MATCH);
        });
    }

    /**
     * Redacts supported sensitive XML element text while preserving element names.
     *
     * @param input the diagnostic text to sanitize.
     * @return the input with sensitive XML element text replaced.
     */
    private static String replaceXmlElements(String input) {
        return replaceMatches(input, XML_ELEMENT_PATTERN, match -> {
            var key = match.group(2);
            if (isSensitiveKey(key)) {
                return match.group(1) + REDACTED + match.group(4);
            }

            return match.group(GROUP_MATCH);
        });
    }

    /**
     * Redacts supported sensitive values inside simple JSON key-value pairs.
     *
     * @param input the diagnostic text to sanitize.
     * @return the input with sensitive JSON values replaced.
     */
    private static String replaceJsonPairs(String input) {
        return replaceMatches(input, JSON_STRING_PATTERN, match -> {
            var key = match.group(JSON_GROUP_KEY);
            if (isSensitiveKey(key)) {
                var quotedKey = match.group(JSON_GROUP_QUOTED_KEY);
                var separator = match.group(JSON_GROUP_SEPARATOR);
                var openingQuote = match.group(JSON_GROUP_VALUE_QUOTE_START);
                var closingQuote = match.group(JSON_GROUP_VALUE_QUOTE_END);
                return quotedKey + separator + openingQuote + REDACTED + closingQuote;
            }

            return match.group(GROUP_MATCH);
        });
    }

    /**
     * Redacts supported sensitive values in delimited key-value formats.
     *
     * @param input the diagnostic text to sanitize.
     * @param pattern the pattern that identifies the supported key-value format.
     * @return the input with sensitive delimited values replaced.
     */
    private static String replaceDelimitedPairs(String input, Pattern pattern) {
        return replaceMatches(input, pattern, match -> {
            var prefix = match.group(DELIMITED_GROUP_PREFIX);
            var key = match.group(DELIMITED_GROUP_KEY);
            var separator = match.group(DELIMITED_GROUP_SEPARATOR);
            var value = match.group(DELIMITED_GROUP_VALUE);

            if (isSensitiveKey(key)) {
                return prefix + key + separator + REDACTED;
            }

            return prefix + key + separator + value;
        });
    }

    /**
     * Rewrites every match produced by the given pattern using the supplied callback.
     *
     * @param input the source text to process.
     * @param pattern the compiled pattern to apply.
     * @param rewriter the callback that produces the replacement for each match.
     * @return the transformed text after applying every replacement.
     */
    private static String replaceMatches(String input, Pattern pattern, Function<MatchResult, String> rewriter) {
        var matcher = pattern.matcher(input);
        var result = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(rewriter.apply(matcher.toMatchResult())));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Determines whether the given key name must be treated as sensitive.
     *
     * @param key the key name extracted from the input text.
     * @return {@code true} when the key is configured as sensitive; otherwise {@code false}.
     */
    private static boolean isSensitiveKey(String key) {
        var normalized = key.toLowerCase();
        var localNameIndex = normalized.lastIndexOf(':');
        var localName = localNameIndex >= 0 ? normalized.substring(localNameIndex + 1) : normalized;
        return SENSITIVE_KEYS.contains(normalized) || SENSITIVE_KEYS.contains(localName);
    }

    /**
     * Converts a nullable string into a non-null string value.
     *
     * @param value the nullable value to normalize.
     * @return the original value when non-null, or an empty string otherwise.
     */
    private static String defaultString(String value) {
        return value == null ? EMPTY : value;
    }
}
