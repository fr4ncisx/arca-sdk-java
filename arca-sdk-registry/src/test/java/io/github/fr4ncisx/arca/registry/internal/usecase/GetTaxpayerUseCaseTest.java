package io.github.fr4ncisx.arca.registry.internal.usecase;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.registry.internal.generated.*;
import io.github.fr4ncisx.arca.registry.model.TaxpayerData;
import jakarta.xml.ws.WebServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the {@link GetTaxpayerUseCase}.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
class GetTaxpayerUseCaseTest {

    private ArcaConfig config;
    private AuthProvider authProvider;
    private ArcaAccessTicket ticket;

    @BeforeEach
    void setUp() {
        config = new ArcaConfig(
                Cuit.parse("20-33333333-4"),
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(2),
                Duration.ofSeconds(2)
        );
        ticket = new ArcaAccessTicket("token-val", "sign-val", Instant.now(), Instant.now().plusSeconds(300));
        authProvider = service -> ticket;
    }

    @Test
    @SuppressWarnings("null")
    void executeSucceedsAndMapsFields() {
        PersonaReturn soapResponse = new PersonaReturn();
        Persona persona = new Persona();
        persona.setIdPersona(20444444445L);
        persona.setNombre("JUAN");
        persona.setApellido("PEREZ");
        persona.setRazonSocial("JUAN PEREZ");
        persona.setEstadoClave("ACTIVO");
        persona.setTipoPersona("FISICA");
        soapResponse.setPersona(persona);

        AtomicReference<GetPersona> capturedReq = new AtomicReference<>();
        ArcaSoapPort<GetPersona, PersonaReturn> port = req -> {
            capturedReq.set(req);
            return soapResponse;
        };

        GetTaxpayerUseCase useCase = new GetTaxpayerUseCase(config, authProvider, port);
        TaxpayerData data = useCase.execute(Cuit.parse("20-44444444-5"));

        assertThat(data).isNotNull();
        assertThat(data.cuit()).isEqualTo(20444444445L);
        assertThat(data.businessName()).isEqualTo("JUAN PEREZ");
        assertThat(data.keyState()).isEqualTo("ACTIVO");
        assertThat(data.personType()).isEqualTo("FISICA");
        assertThat(capturedReq.get()).isNotNull();
        assertThat(capturedReq.get().getToken()).isEqualTo("token-val");
    }

    @Test
    void executeThrowsValidationExceptionIfPersonaNotFound() {
        PersonaReturn soapResponse = new PersonaReturn(); // Persona is null

        ArcaSoapPort<GetPersona, PersonaReturn> port = req -> soapResponse;
        GetTaxpayerUseCase useCase = new GetTaxpayerUseCase(config, authProvider, port);

        assertThatThrownBy(() -> useCase.execute(Cuit.parse("20-44444444-5")))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void executeThrowsValidationExceptionOnSRValidationFault() {
        SRValidationException faultInfo = new SRValidationException();
        SRValidationException_Exception fault = new SRValidationException_Exception("Taxpayer is invalid", faultInfo);

        ArcaSoapPort<GetPersona, PersonaReturn> port = req -> {
            throw new ArcaSoapException("Fault", new WebServiceException(fault));
        };

        GetTaxpayerUseCase useCase = new GetTaxpayerUseCase(config, authProvider, port);

        assertThatThrownBy(() -> useCase.execute(Cuit.parse("20-44444444-5")))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("validation failed")
                .hasMessageContaining("Taxpayer is invalid");
    }
}
