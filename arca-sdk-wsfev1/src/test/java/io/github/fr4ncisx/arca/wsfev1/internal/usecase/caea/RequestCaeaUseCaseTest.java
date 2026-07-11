package io.github.fr4ncisx.arca.wsfev1.internal.usecase.caea;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.model.caea.CaeaRequest;
import io.github.fr4ncisx.arca.wsfev1.model.caea.CaeaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RequestCaeaUseCase}.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
@SuppressWarnings("null")
class RequestCaeaUseCaseTest {

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
    void constructorRejectsNulls() {
        ArcaSoapPort<FECAEASolicitar, FECAEAGetResponse> port = req -> new FECAEAGetResponse();

        assertThatThrownBy(() -> new RequestCaeaUseCase(null, authProvider, port))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> new RequestCaeaUseCase(config, null, port))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> new RequestCaeaUseCase(config, authProvider, null))
                .isInstanceOf(ArcaValidationException.class);
    }

    @Test
    void executeCallsSoapPortAndReturnsCaeaDetails() {
        AtomicReference<FECAEASolicitar> capturedRequest = new AtomicReference<>();
        FECAEAGetResponse soapResponse = new FECAEAGetResponse();

        FECAEAGet resultGet = new FECAEAGet();
        resultGet.setCAEA("caea-98765");
        resultGet.setPeriodo(202607);
        resultGet.setOrden((short) 1);
        resultGet.setFchVigDesde("20260701");
        resultGet.setFchVigHasta("20260715");
        resultGet.setFchTopeInf("20260720");
        soapResponse.setResultGet(resultGet);

        ArcaSoapPort<FECAEASolicitar, FECAEAGetResponse> port = req -> {
            capturedRequest.set(req);
            return soapResponse;
        };

        RequestCaeaUseCase useCase = new RequestCaeaUseCase(config, authProvider, port);
        CaeaResponse response = useCase.execute(new CaeaRequest(202607, 1));

        assertThat(response).isNotNull();
        assertThat(response.caea()).isEqualTo("caea-98765");
        assertThat(response.period()).isEqualTo(202607);
        assertThat(response.order()).isEqualTo(1);

        FECAEASolicitar soapReq = capturedRequest.get();
        assertThat(soapReq).isNotNull();
        assertThat(soapReq.getAuth().getToken()).isEqualTo("token-val");
        assertThat(soapReq.getPeriodo()).isEqualTo(202607);
        assertThat(soapReq.getOrden()).isEqualTo((short) 1);
    }
}
