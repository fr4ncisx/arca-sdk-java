package io.github.fr4ncisx.arca.wscdc.model;

/**
 * Represents a validation observation returned by the WSCDC service.
 *
 * @param code the observation code
 * @param message the observation message
 * @author fr4ncisx
 * @since 0.9.0
 */
public record WscdcObservation(
    int code,
    String message
) {}
