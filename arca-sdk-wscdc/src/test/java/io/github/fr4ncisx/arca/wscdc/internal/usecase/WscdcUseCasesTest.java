package io.github.fr4ncisx.arca.wscdc.internal.usecase;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wscdc.internal.generated.CmpResponse;
import io.github.fr4ncisx.arca.wscdc.model.WscdcConstatRequest;
import io.github.fr4ncisx.arca.wscdc.model.WscdcConstatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit and use case behavior tests for WSCDC voucher constatation module.
 *
 * @author fr4ncisx
 * @since 0.9.0
 */
class WscdcUseCasesTest {

    private ArcaConfig config;
    private AuthProvider authProvider;
    private ArcaAccessTicket ticket;

    /**
     * Set up common configurations and mocks.
     */
    @BeforeEach
    void setUp() {
        config = new ArcaConfig(
            Cuit.parse("20-30000000-3"),
            ArcaEnvironment.HOMOLOGACION,
            Duration.ofSeconds(5),
            Duration.ofSeconds(5)
        );
        authProvider = mock(AuthProvider.class);
        ticket = new ArcaAccessTicket("token-123", "sign-456", Instant.now(), Instant.now().plusSeconds(3600));
        when(authProvider.authenticate(any())).thenReturn(ticket);
    }

    /**
     * Validates that the constatation use case executes correctly and maps JAXB responses.
     */
    @Test
    @SuppressWarnings("unchecked")
    void checkVoucherUseCaseExecutesCorrectly() {
        ArcaSoapPort<WscdcRequestWrapper, CmpResponse> soapPort = mock(ArcaSoapPort.class);
        CmpResponse mockSoapResponse = new CmpResponse();
        mockSoapResponse.setResultado("A");
        mockSoapResponse.setFchProceso("20260715100000");

        when(soapPort.invoke(any())).thenReturn(mockSoapResponse);

        ConstatVoucherUseCase useCase = new ConstatVoucherUseCase(config, authProvider, soapPort);
        WscdcConstatRequest request = new WscdcConstatRequest(
            "CAE",
            Cuit.parse("20-30000000-3"),
            1,
            1,
            100L,
            LocalDate.of(2026, 7, 15),
            new BigDecimal("121.0"),
            "12345678901234",
            null,
            null,
            List.of()
        );

        WscdcConstatResponse response = useCase.execute(request);
        assertThat(response).isNotNull();
        assertThat(response.isApproved()).isTrue();
        assertThat(response.result()).isEqualTo("A");
        assertThat(response.processDate()).isEqualTo(LocalDate.of(2026, 7, 15));
    }
}
