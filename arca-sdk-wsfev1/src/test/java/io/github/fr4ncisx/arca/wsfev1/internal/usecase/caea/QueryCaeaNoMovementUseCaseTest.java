package io.github.fr4ncisx.arca.wsfev1.internal.usecase.caea;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.model.caea.CaeaNoMovementQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link QueryCaeaNoMovementUseCase}.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
@SuppressWarnings("null")
class QueryCaeaNoMovementUseCaseTest {

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
    void executeReturnsTrueWhenDeclarationExists() {
        AtomicReference<FECAEASinMovimientoConsultar> capturedRequest = new AtomicReference<>();
        FECAEASinMovConsResponse soapResponse = new FECAEASinMovConsResponse();

        ArrayOfFECAEASinMov resultGet = new ArrayOfFECAEASinMov();
        FECAEASinMov item = new FECAEASinMov();
        item.setCAEA("caea-123");
        item.setPtoVta(1);
        resultGet.getFECAEASinMov().add(item);
        soapResponse.setResultGet(resultGet);

        ArcaSoapPort<FECAEASinMovimientoConsultar, FECAEASinMovConsResponse> port = req -> {
            capturedRequest.set(req);
            return soapResponse;
        };

        QueryCaeaNoMovementUseCase useCase = new QueryCaeaNoMovementUseCase(config, authProvider, port);
        boolean result = useCase.execute(new CaeaNoMovementQuery("caea-123", 1));

        assertThat(result).isTrue();

        FECAEASinMovimientoConsultar soapReq = capturedRequest.get();
        assertThat(soapReq).isNotNull();
        assertThat(soapReq.getAuth().getToken()).isEqualTo("token-val");
    }

    @Test
    void executeReturnsFalseWhenDeclarationDoesNotExist() {
        FECAEASinMovConsResponse soapResponse = new FECAEASinMovConsResponse();
        soapResponse.setResultGet(null);

        ArcaSoapPort<FECAEASinMovimientoConsultar, FECAEASinMovConsResponse> port = req -> soapResponse;

        QueryCaeaNoMovementUseCase useCase = new QueryCaeaNoMovementUseCase(config, authProvider, port);
        boolean result = useCase.execute(new CaeaNoMovementQuery("caea-123", 1));

        assertThat(result).isFalse();
    }

    @Test
    void executeThrowsSoapExceptionWhenARCAReturnsErrors() {
        FECAEASinMovConsResponse soapResponse = new FECAEASinMovConsResponse();

        ArrayOfErr errArray = new ArrayOfErr();
        Err err = new Err();
        err.setCode(1001);
        err.setMsg("Technical server error");
        errArray.getErr().add(err);
        soapResponse.setErrors(errArray);

        ArcaSoapPort<FECAEASinMovimientoConsultar, FECAEASinMovConsResponse> port = req -> soapResponse;

        QueryCaeaNoMovementUseCase useCase = new QueryCaeaNoMovementUseCase(config, authProvider, port);

        assertThatThrownBy(() -> useCase.execute(new CaeaNoMovementQuery("caea-123", 1)))
                .isInstanceOf(ArcaSoapException.class)
                .hasMessageContaining("Technical server error");
    }
}
