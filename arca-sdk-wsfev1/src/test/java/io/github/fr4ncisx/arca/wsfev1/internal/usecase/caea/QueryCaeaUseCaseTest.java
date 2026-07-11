package io.github.fr4ncisx.arca.wsfev1.internal.usecase.caea;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.model.caea.CaeaQuery;
import io.github.fr4ncisx.arca.wsfev1.model.caea.CaeaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link QueryCaeaUseCase}.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
@SuppressWarnings("null")
class QueryCaeaUseCaseTest {

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
    void executeCallsSoapPortAndReturnsCaeaDetails() {
        AtomicReference<FECAEAConsultar> capturedRequest = new AtomicReference<>();
        FECAEAGetResponse soapResponse = new FECAEAGetResponse();

        FECAEAGet resultGet = new FECAEAGet();
        resultGet.setCAEA("caea-98765");
        resultGet.setPeriodo(202607);
        resultGet.setOrden((short) 1);
        resultGet.setFchVigDesde("20260701");
        resultGet.setFchVigHasta("20260715");
        resultGet.setFchTopeInf("20260720");
        soapResponse.setResultGet(resultGet);

        ArcaSoapPort<FECAEAConsultar, FECAEAGetResponse> port = req -> {
            capturedRequest.set(req);
            return soapResponse;
        };

        QueryCaeaUseCase useCase = new QueryCaeaUseCase(config, authProvider, port);
        CaeaResponse response = useCase.execute(new CaeaQuery(202607, 1));

        assertThat(response).isNotNull();
        assertThat(response.caea()).isEqualTo("caea-98765");

        FECAEAConsultar soapReq = capturedRequest.get();
        assertThat(soapReq).isNotNull();
        assertThat(soapReq.getAuth().getToken()).isEqualTo("token-val");
    }

    @Test
    void executeThrowsValidationExceptionWhenCaeaDoesNotExist() {
        FECAEAGetResponse soapResponse = new FECAEAGetResponse();
        soapResponse.setResultGet(null); // No CAEA returned

        ArcaSoapPort<FECAEAConsultar, FECAEAGetResponse> port = req -> soapResponse;

        QueryCaeaUseCase useCase = new QueryCaeaUseCase(config, authProvider, port);

        assertThatThrownBy(() -> useCase.execute(new CaeaQuery(202607, 1)))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("does not exist");
    }
}
