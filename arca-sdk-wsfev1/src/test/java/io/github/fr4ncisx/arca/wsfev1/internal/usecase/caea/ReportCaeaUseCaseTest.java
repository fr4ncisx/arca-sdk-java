package io.github.fr4ncisx.arca.wsfev1.internal.usecase.caea;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeVatLine;
import io.github.fr4ncisx.arca.wsfev1.model.caea.*;
import io.github.fr4ncisx.arca.wsfev1.model.common.ConceptType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VatType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ReportCaeaUseCase}.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
@SuppressWarnings("null")
class ReportCaeaUseCaseTest {

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
    void executeCallsSoapPortAndReturnsCaeaReportResponse() {
        AtomicReference<FECAEARegInformativo> capturedRequest = new AtomicReference<>();
        FECAEAResponse soapResponse = new FECAEAResponse();

        FECAEACabResponse cab = new FECAEACabResponse();
        cab.setResultado("A");
        soapResponse.setFeCabResp(cab);

        FECAEADetResponse det = new FECAEADetResponse();
        det.setCbteDesde(105L);
        det.setResultado("A");

        ArrayOfFECAEADetResponse detArray = new ArrayOfFECAEADetResponse();
        detArray.getFECAEADetResponse().add(det);
        soapResponse.setFeDetResp(detArray);

        ArcaSoapPort<FECAEARegInformativo, FECAEAResponse> port = req -> {
            capturedRequest.set(req);
            return soapResponse;
        };

        ReportCaeaUseCase useCase = new ReportCaeaUseCase(config, authProvider, port);
        CaeaReportResponse response = useCase.execute(new CaeaReportRequest(
                "caea-123",
                2,
                VoucherType.INVOICE_A,
                List.of(new CaeaReportDetail(
                        ConceptType.PRODUCTS,
                        Cuit.parse("27-44444444-9"),
                        105L,
                        LocalDate.of(2026, 7, 7),
                        121.0,
                        100.0,
                        0.0,
                        21.0,
                        List.of(new CaeVatLine(VatType.VAT_21, 100.0, 21.0))
                ))
        ));

        assertThat(response).isNotNull();
        assertThat(response.result()).isEqualTo("A");
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).number()).isEqualTo(105L);

        FECAEARegInformativo soapReq = capturedRequest.get();
        assertThat(soapReq).isNotNull();
        assertThat(soapReq.getAuth().getToken()).isEqualTo("token-val");
    }
}
