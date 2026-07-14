package io.github.fr4ncisx.arca.wsfexv1.model;

/**
 * Represents a business error, warning, or observation returned by ARCA.
 *
 * @param code    the official numeric code
 * @param message the description of the error or event
 * @author fr4ncisx
 * @since 0.7.0
 */
public record AfipError(int code, String message) {

    public AfipError {
        if (message == null) {
            message = "";
        }
        message = message.trim();
    }
}
