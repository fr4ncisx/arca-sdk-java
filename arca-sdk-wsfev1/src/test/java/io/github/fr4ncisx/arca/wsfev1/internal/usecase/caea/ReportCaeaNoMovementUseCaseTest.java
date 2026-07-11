package io.github.fr4ncisx.arca.wsfev1.internal.usecase.caea;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.model.caea.CaeaNoMovementRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ReportCaeaNoMovementUseCase}.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
@SuppressWarnings("null")
class ReportCaeaNoMovementUseCaseTest {

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
    void executeCallsSoapPortSuccessfully() {
        AtomicReference<FECAEASinMovimientoInformar> capturedRequest = new AtomicReference<>();
        FECAEASinMovResponse soapResponse = new FECAEASinMovResponse();
        soapResponse.setResultado("A");

        ArcaSoapPort<FECAEASinMovimientoInformar, FECAEASinMovResponse> port = req -> {
            capturedRequest.set(req);
            return soapResponse;
        };

        ReportCaeaNoMovementUseCase useCase = new ReportCaeaNoMovementUseCase(config, authProvider, port);
        useCase.execute(new CaeaNoMovementRequest("caea-123", 1));

        FECAEASinMovimientoInformar soapReq = capturedRequest.get();
        assertThat(soapReq).isNotNull();
        assertThat(soapReq.getAuth().getToken()).isEqualTo("token-val");
    }

    @Test
    void executeThrowsValidationExceptionWhenARCARejects() {
        FECAEASinMovResponse soapResponse = new FECAEASinMovResponse();
        soapResponse.setResultado("R");

        ArrayOfErr errArray = new ArrayOfErr();
        Err err = new Err();
        err.setCode(1001);
        err.setMsg("Already declared with movement");
        errArray.getErr().add(err);
        soapResponse.setErrors(errArray);

        ArcaSoapPort<FECAEASinMovimientoInformar, FECAEASinMovResponse> port = req -> soapResponse;

        ReportCaeaNoMovementUseCase useCase = new ReportCaeaNoMovementUseCase(config, authProvider, port);

        assertThatThrownBy(() -> useCase.execute(new CaeaNoMovementRequest("caea-123", 1)))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("Already declared with movement");
    }
}
