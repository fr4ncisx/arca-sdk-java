package io.github.fr4ncisx.arca.wsfev1.internal.usecase;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEParamGetPtosVenta;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEPtoVentaResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.ArrayOfPtoVenta;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.PtoVenta;
import io.github.fr4ncisx.arca.wsfev1.model.SalesPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("null")
class GetSalesPointsUseCaseTest {

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
        ArcaSoapPort<FEParamGetPtosVenta, FEPtoVentaResponse> port = req -> new FEPtoVentaResponse();

        assertThatThrownBy(() -> new GetSalesPointsUseCase(null, authProvider, port))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> new GetSalesPointsUseCase(config, null, port))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> new GetSalesPointsUseCase(config, authProvider, null))
                .isInstanceOf(ArcaValidationException.class);
    }

    @Test
    void executeReturnsMappedSalesPoints() {
        AtomicReference<FEParamGetPtosVenta> capturedRequest = new AtomicReference<>();
        FEPtoVentaResponse soapResponse = new FEPtoVentaResponse();
        ArrayOfPtoVenta resultGet = new ArrayOfPtoVenta();

        PtoVenta pv = new PtoVenta();
        pv.setNro(1);
        pv.setEmisionTipo("Fisico");
        pv.setBloqueado("N");
        pv.setFchBaja("null");
        resultGet.getPtoVenta().add(pv);
        soapResponse.setResultGet(resultGet);

        ArcaSoapPort<FEParamGetPtosVenta, FEPtoVentaResponse> port = req -> {
            capturedRequest.set(req);
            return soapResponse;
        };

        GetSalesPointsUseCase useCase = new GetSalesPointsUseCase(config, authProvider, port);
        List<SalesPoint> points = useCase.execute();

        assertThat(points).hasSize(1);
        assertThat(points.get(0).number()).isEqualTo(1);
        assertThat(points.get(0).emissionType()).isEqualTo("Fisico");
        assertThat(points.get(0).blocked()).isFalse();
        assertThat(points.get(0).dropDate()).isEmpty();

        FEParamGetPtosVenta soapReq = capturedRequest.get();
        assertThat(soapReq).isNotNull();
        assertThat(soapReq.getAuth().getToken()).isEqualTo("token-val");
    }

    @Test
    void executeReturnsEmptyListWhenARCAReturnsEmptyResultGet() {
        FEPtoVentaResponse soapResponse = new FEPtoVentaResponse();
        soapResponse.setResultGet(null);

        ArcaSoapPort<FEParamGetPtosVenta, FEPtoVentaResponse> port = req -> soapResponse;

        GetSalesPointsUseCase useCase = new GetSalesPointsUseCase(config, authProvider, port);
        List<SalesPoint> points = useCase.execute();

        assertThat(points).isEmpty();
    }
}
