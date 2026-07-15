package io.github.fr4ncisx.arca.wsmtxca.internal.usecase;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.AutorizarComprobanteRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.AutorizarComprobanteResponseType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ComprobanteCAEResponseType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ComprobanteType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarComprobanteRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarComprobanteResponseType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarUltimoComprobanteAutorizadoRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarUltimoComprobanteAutorizadoResponseType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ResultadoSimpleType;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaLastVoucherRequest;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaLastVoucherResponse;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherConsultRequest;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherConsultResponse;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherRequest;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WsmtxcaUseCasesTest {

    private ArcaConfig config;
    private AuthProvider authProvider;
    private ArcaAccessTicket ticket;

    @BeforeEach
    void setUp() {
        config = new ArcaConfig(
                Cuit.parse("20-33333333-4"),
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(5),
                Duration.ofSeconds(5)
        );
        authProvider = mock(AuthProvider.class);
        ticket = new ArcaAccessTicket("token-123", "sign-456", Instant.now(), Instant.now().plusSeconds(3600));
        when(authProvider.authenticate("wsmtxca")).thenReturn(ticket);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getLastWsmtxcaVoucherUseCaseExecutesCorrectly() {
        ArcaSoapPort<ConsultarUltimoComprobanteAutorizadoRequestType, ConsultarUltimoComprobanteAutorizadoResponseType> soapPort = mock(ArcaSoapPort.class);

        ConsultarUltimoComprobanteAutorizadoResponseType soapResponse = new ConsultarUltimoComprobanteAutorizadoResponseType();
        soapResponse.setNumeroComprobante(125);

        when(soapPort.invoke(any(ConsultarUltimoComprobanteAutorizadoRequestType.class))).thenReturn(soapResponse);

        GetLastWsmtxcaVoucherUseCase useCase = new GetLastWsmtxcaVoucherUseCase(config, authProvider, soapPort);
        WsmtxcaLastVoucherRequest request = new WsmtxcaLastVoucherRequest(1, (short) 1);
        WsmtxcaLastVoucherResponse response = useCase.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.lastVoucherNumber()).isEqualTo(125L);
        assertThat(response.salesPoint()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void authorizeWsmtxcaVoucherUseCaseExecutesCorrectly() throws Exception {
        ArcaSoapPort<AutorizarComprobanteRequestType, AutorizarComprobanteResponseType> soapPort = mock(ArcaSoapPort.class);

        AutorizarComprobanteResponseType soapResponse = new AutorizarComprobanteResponseType();
        soapResponse.setResultado(ResultadoSimpleType.A);

        ComprobanteCAEResponseType out = new ComprobanteCAEResponseType();
        out.setCodigoTipoComprobante((short) 1);
        out.setNumeroPuntoVenta(1);
        out.setNumeroComprobante(124);
        out.setCAE(12345678901234L);
        out.setFechaEmision(DatatypeFactory.newInstance().newXMLGregorianCalendar("2026-07-15"));
        out.setFechaVencimientoCAE(DatatypeFactory.newInstance().newXMLGregorianCalendar("2026-07-25"));
        soapResponse.setComprobanteResponse(out);

        when(soapPort.invoke(any(AutorizarComprobanteRequestType.class))).thenReturn(soapResponse);

        AuthorizeWsmtxcaVoucherUseCase useCase = new AuthorizeWsmtxcaVoucherUseCase(config, authProvider, soapPort);
        WsmtxcaVoucherRequest request = new WsmtxcaVoucherRequest(
                (short) 1, 1, 124L, LocalDate.of(2026, 7, 15), (short) 80, 30000000007L, (short) 1,
                BigDecimal.valueOf(100.00), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(100.00), BigDecimal.ZERO,
                BigDecimal.valueOf(121.00), "PES", BigDecimal.ONE, "S", "", (short) 1, null, null, null,
                List.of(), List.of(), List.of(), List.of()
        );
        WsmtxcaVoucherResponse response = useCase.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.authorizationCode()).isEqualTo("12345678901234");
        assertThat(response.status()).isEqualTo("A");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getWsmtxcaVoucherUseCaseExecutesCorrectly() {
        ArcaSoapPort<ConsultarComprobanteRequestType, ConsultarComprobanteResponseType> soapPort = mock(ArcaSoapPort.class);

        ConsultarComprobanteResponseType soapResponse = new ConsultarComprobanteResponseType();
        ComprobanteType comp = new ComprobanteType();
        comp.setCodigoTipoComprobante((short) 1);
        comp.setNumeroPuntoVenta(1);
        comp.setNumeroComprobante(124);
        comp.setCodigoConcepto((short) 1);
        comp.setCodigoMoneda("PES");
        comp.setImporteSubtotal(BigDecimal.valueOf(100.00));
        comp.setImporteTotal(BigDecimal.valueOf(121.00));
        soapResponse.setComprobante(comp);

        when(soapPort.invoke(any(ConsultarComprobanteRequestType.class))).thenReturn(soapResponse);

        GetWsmtxcaVoucherUseCase useCase = new GetWsmtxcaVoucherUseCase(config, authProvider, soapPort);
        WsmtxcaVoucherConsultRequest request = new WsmtxcaVoucherConsultRequest((short) 1, 1, 124L);
        WsmtxcaVoucherConsultResponse response = useCase.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.voucher()).isNotNull();
        assertThat(response.voucher().voucherType()).isEqualTo((short) 1);
        assertThat(response.voucher().totalAmount()).isEqualTo(BigDecimal.valueOf(121.00));
    }
}
