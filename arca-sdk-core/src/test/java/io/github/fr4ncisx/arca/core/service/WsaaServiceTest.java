package io.github.fr4ncisx.arca.core.service;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates the behavior and contract of {@link WsaaService}.
 *
 * @author fr4ncisx
 * @since 1.3.0
 */
class WsaaServiceTest {

    @ParameterizedTest
    @CsvSource({
            "WSFE, wsfe",
            "REGISTRY_A4, ws_sr_padron_a4",
            "WSFEX, wsfex",
            "WSMTXCA, wsmtxca",
            "WSCDC, wscdc"
    })
    void serviceIdShouldMatchExpectedOfficialIdentifier(WsaaService service, String expectedIdentifier) {
        assertThat(service.serviceId()).isEqualTo(expectedIdentifier);
    }

    @ParameterizedTest
    @CsvSource({
            "wsfe, WSFE",
            "WSFE, WSFE",
            "  wsfe  , WSFE",
            "ws_sr_padron_a4, REGISTRY_A4",
            "WS_SR_PADRON_A4, REGISTRY_A4",
            "wsfex, WSFEX",
            "wsmtxca, WSMTXCA",
            "wscdc, WSCDC"
    })
    void fromServiceIdShouldResolveValidIdentifierCaseInsensitively(String input, WsaaService expected) {
        assertThat(WsaaService.fromServiceId(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void fromServiceIdShouldThrowExceptionOnBlankInput(String invalidInput) {
        assertThatThrownBy(() -> WsaaService.fromServiceId(invalidInput))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("serviceId must not be blank");
    }

    @Test
    void fromServiceIdShouldThrowExceptionOnUnknownService() {
        assertThatThrownBy(() -> WsaaService.fromServiceId("unknown_service"))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("Unknown WSAA service: unknown_service");
    }
}
