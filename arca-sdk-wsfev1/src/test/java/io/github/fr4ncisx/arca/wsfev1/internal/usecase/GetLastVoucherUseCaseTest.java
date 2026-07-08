package io.github.fr4ncisx.arca.wsfev1.internal.usecase;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECompUltimoAutorizado;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FERecuperaLastCbteResponse;
import io.github.fr4ncisx.arca.wsfev1.model.LastVoucherRequest;
import io.github.fr4ncisx.arca.wsfev1.model.LastVoucherResponse;
import io.github.fr4ncisx.arca.wsfev1.model.VoucherType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GetLastVoucherUseCase}.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
@SuppressWarnings("null")
class GetLastVoucherUseCaseTest {

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
        ArcaSoapPort<FECompUltimoAutorizado, FERecuperaLastCbteResponse> port = req -> new FERecuperaLastCbteResponse();

        assertThatThrownBy(() -> new GetLastVoucherUseCase(null, authProvider, port))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> new GetLastVoucherUseCase(config, null, port))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> new GetLastVoucherUseCase(config, authProvider, null))
                .isInstanceOf(ArcaValidationException.class);
    }

    @Test
    void executeCallsSoapPortWithMappedRequestAndReturnsResult() {
        var result = new FERecuperaLastCbteResponse();
        result.setPtoVta(1);
        result.setCbteNro(500);

        var invokedRequestRef = new AtomicReference<FECompUltimoAutorizado>();
        ArcaSoapPort<FECompUltimoAutorizado, FERecuperaLastCbteResponse> soapPort = request -> {
            invokedRequestRef.set(request);
            return result;
        };

        var useCase = new GetLastVoucherUseCase(config, authProvider, soapPort);
        var request = new LastVoucherRequest(1, VoucherType.INVOICE_A);

        LastVoucherResponse response = useCase.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.lastNumber()).isEqualTo(500L);

        var soapReq = invokedRequestRef.get();
        assertThat(soapReq).isNotNull();
        assertThat(soapReq.getAuth()).isNotNull();
        assertThat(soapReq.getAuth().getToken()).isEqualTo("token-val");
        assertThat(soapReq.getAuth().getSign()).isEqualTo("sign-val");
        assertThat(soapReq.getPtoVta()).isEqualTo(1);
        assertThat(soapReq.getCbteTipo()).isEqualTo(VoucherType.INVOICE_A.code());
    }
}
