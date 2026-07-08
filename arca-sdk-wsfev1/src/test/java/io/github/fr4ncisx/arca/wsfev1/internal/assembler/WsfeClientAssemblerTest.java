package io.github.fr4ncisx.arca.wsfev1.internal.assembler;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.spi.WsfeClient;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link WsfeClientAssembler}.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
@SuppressWarnings("null")
class WsfeClientAssemblerTest {

    @Test
    void assembleWithOfficialEndpointsSucceeds() {
        var config = new ArcaConfig(
                Cuit.parse("20-33333333-4"),
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        );
        AuthProvider authProvider = service -> new ArcaAccessTicket("t", "s", Instant.now(), Instant.now().plusSeconds(60));

        WsfeClient client = WsfeClientAssembler.assemble(config, authProvider);

        assertThat(client).isNotNull();
    }

    @Test
    void assembleWithCustomEndpointSucceeds() {
        var config = new ArcaConfig(
                Cuit.parse("20-33333333-4"),
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        );
        AuthProvider authProvider = service -> new ArcaAccessTicket("t", "s", Instant.now(), Instant.now().plusSeconds(60));

        WsfeClient client = WsfeClientAssembler.assemble(config, authProvider, "http://localhost:8080/wsfev1");

        assertThat(client).isNotNull();
    }

    @Test
    void assembleValidatesNullArguments() {
        var config = new ArcaConfig(
                Cuit.parse("20-33333333-4"),
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        );
        AuthProvider authProvider = service -> new ArcaAccessTicket("t", "s", Instant.now(), Instant.now().plusSeconds(60));

        assertThatThrownBy(() -> WsfeClientAssembler.assemble(null, authProvider))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> WsfeClientAssembler.assemble(config, null))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> WsfeClientAssembler.assemble(config, authProvider, null))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> WsfeClientAssembler.assemble(config, authProvider, "   "))
                .isInstanceOf(ArcaValidationException.class);
    }
}
