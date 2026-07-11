package io.github.fr4ncisx.arca.wsfev1.internal.usecase.voucher;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECompConsResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECompConsultaResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECompConsultar;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;
import io.github.fr4ncisx.arca.wsfev1.model.voucher.VoucherConsultRequest;
import io.github.fr4ncisx.arca.wsfev1.model.voucher.VoucherConsultResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GetVoucherUseCase}.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
@SuppressWarnings("null")
class GetVoucherUseCaseTest {

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
        ArcaSoapPort<FECompConsultar, FECompConsultaResponse> port = req -> new FECompConsultaResponse();

        assertThatThrownBy(() -> new GetVoucherUseCase(null, authProvider, port))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> new GetVoucherUseCase(config, null, port))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> new GetVoucherUseCase(config, authProvider, null))
                .isInstanceOf(ArcaValidationException.class);
    }

    @Test
    void executeRejectsNullRequest() {
        ArcaSoapPort<FECompConsultar, FECompConsultaResponse> port = req -> new FECompConsultaResponse();
        GetVoucherUseCase useCase = new GetVoucherUseCase(config, authProvider, port);

        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("request must not be null");
    }

    @Test
    void executesSuccessfullyWithMockResponse() {
        AtomicReference<FECompConsultar> capturedRequest = new AtomicReference<>();
        FECompConsultaResponse soapResponse = new FECompConsultaResponse();
        FECompConsResponse soapDetail = new FECompConsResponse();
        soapDetail.setResultado("A");
        soapDetail.setCodAutorizacion("cae-12345");
        soapDetail.setFchVto("20260710");
        soapDetail.setCbteFch("20260709");
        soapDetail.setPtoVta(1);
        soapDetail.setCbteTipo(1);
        soapDetail.setConcepto(1);
        soapDetail.setDocNro(20333333334L);
        soapDetail.setCbteDesde(42L);
        soapDetail.setImpTotal(121.0);
        soapDetail.setImpNeto(100.0);
        soapDetail.setImpIVA(21.0);
        soapResponse.setResultGet(soapDetail);

        ArcaSoapPort<FECompConsultar, FECompConsultaResponse> port = req -> {
            capturedRequest.set(req);
            return soapResponse;
        };

        GetVoucherUseCase useCase = new GetVoucherUseCase(config, authProvider, port);
        VoucherConsultRequest request = new VoucherConsultRequest(1, VoucherType.INVOICE_A, 42L);

        VoucherConsultResponse response = useCase.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.detail()).isPresent();
        assertThat(response.detail().get().cae()).isEqualTo("cae-12345");
        assertThat(response.detail().get().number()).isEqualTo(42L);

        FECompConsultar soapReq = capturedRequest.get();
        assertThat(soapReq).isNotNull();
        assertThat(soapReq.getAuth().getToken()).isEqualTo("token-val");
        assertThat(soapReq.getFeCompConsReq().getCbteNro()).isEqualTo(42L);
    }
}
