package io.github.fr4ncisx.arca.wsfev1.model.common;

/**
 * Represents a business error or observation returned by ARCA.
 * <p>
 * This record is used to expose validation warnings, tax inconsistencies, or API faults
 * reported by ARCA, without throwing technical control exceptions.
 *
 * @param code    the official numeric error or observation code from ARCA
 * @param message the descriptive error message
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public record AfipError(int code, String message) {

    public AfipError {
        if (message == null) {
            message = "";
        }
        message = message.trim();
    }
}
