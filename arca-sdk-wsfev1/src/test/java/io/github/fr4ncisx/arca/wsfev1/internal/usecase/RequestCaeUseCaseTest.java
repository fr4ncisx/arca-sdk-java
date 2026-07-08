package io.github.fr4ncisx.arca.wsfev1.internal.usecase;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.model.*;
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
 * Unit tests for {@link RequestCaeUseCase}.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
@SuppressWarnings("null")
class RequestCaeUseCaseTest {

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
        ArcaSoapPort<FECAESolicitar, FECAEResponse> port = req -> new FECAEResponse();

        assertThatThrownBy(() -> new RequestCaeUseCase(null, authProvider, port))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> new RequestCaeUseCase(config, null, port))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> new RequestCaeUseCase(config, authProvider, null))
                .isInstanceOf(ArcaValidationException.class);
    }

    @Test
    void executeCallsSoapPortWithMappedRequestAndReturnsCaeResponse() {
        var result = new FECAEResponse();
        var cab = new FECAECabResponse();
        cab.setResultado("A");
        result.setFeCabResp(cab);

        var detResponse = new FECAEDetResponse();
        detResponse.setCAE("cae-code-99");
        detResponse.setCAEFchVto("20260720");
        var detArray = new ArrayOfFECAEDetResponse();
        detArray.getFECAEDetResponse().add(detResponse);
        result.setFeDetResp(detArray);

        var invokedRequestRef = new AtomicReference<FECAESolicitar>();
        ArcaSoapPort<FECAESolicitar, FECAEResponse> soapPort = request -> {
            invokedRequestRef.set(request);
            return result;
        };

        var useCase = new RequestCaeUseCase(config, authProvider, soapPort);
        var request = new CaeRequest(
                VoucherType.INVOICE_A,
                1,
                105L,
                ConceptType.PRODUCTS,
                Cuit.parse("27-44444444-9"),
                100.0,
                0.0,
                0.0,
                21.0,
                121.0,
                LocalDate.of(2026, 7, 7),
                List.of(new CaeVatLine(VatType.VAT_21, 100.0, 21.0))
        );

        CaeResponse response = useCase.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.isApproved()).isTrue();
        assertThat(response.cae()).isEqualTo("cae-code-99");
        assertThat(response.caeExpiration()).isEqualTo(LocalDate.of(2026, 7, 20));

        var soapReq = invokedRequestRef.get();
        assertThat(soapReq).isNotNull();
        assertThat(soapReq.getAuth().getToken()).isEqualTo("token-val");
    }
}
