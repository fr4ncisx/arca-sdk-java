package io.github.fr4ncisx.arca.wsmtxca.internal.usecase;

import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.AuthRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.AutorizarComprobanteRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.AutorizarComprobanteResponseType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ComprobanteCAEResponseType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarUltimoComprobanteAutorizadoRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarUltimoComprobanteAutorizadoResponseType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ResultadoSimpleType;
import io.github.fr4ncisx.arca.wsmtxca.model.AssociatedVoucher;
import io.github.fr4ncisx.arca.wsmtxca.model.ItemDetail;
import io.github.fr4ncisx.arca.wsmtxca.model.TaxDetail;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaLastVoucherRequest;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaLastVoucherResponse;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherRequest;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WsmtxcaMapperTest {

    private ArcaAccessTicket ticket;
    private Cuit companyCuit;

    @BeforeEach
    void setUp() {
        ticket = new ArcaAccessTicket("token-123", "sign-456", Instant.now(), Instant.now().plusSeconds(3600));
        companyCuit = Cuit.parse("20-33333333-4");
    }

    @Test
    void toAuthRequestMapsCorrectly() {
        AuthRequestType auth = WsmtxcaMapper.toAuthRequest(ticket, companyCuit);
        assertThat(auth).isNotNull();
        assertThat(auth.getToken()).isEqualTo("token-123");
        assertThat(auth.getSign()).isEqualTo("sign-456");
        assertThat(auth.getCuitRepresentada()).isEqualTo(20333333334L);
    }

    @Test
    void toLastVoucherRequestMapsCorrectly() {
        WsmtxcaLastVoucherRequest request = new WsmtxcaLastVoucherRequest(1, (short) 1);
        ConsultarUltimoComprobanteAutorizadoRequestType req = WsmtxcaMapper.toLastVoucherRequest(ticket, companyCuit, request);
        assertThat(req).isNotNull();
        assertThat(req.getAuthRequest().getToken()).isEqualTo("token-123");
        assertThat(req.getConsultaUltimoComprobanteAutorizadoRequest().getNumeroPuntoVenta()).isEqualTo(1);
        assertThat(req.getConsultaUltimoComprobanteAutorizadoRequest().getCodigoTipoComprobante()).isEqualTo((short) 1);
    }

    @Test
    void toLastVoucherResponseMapsCorrectly() {
        ConsultarUltimoComprobanteAutorizadoResponseType response = new ConsultarUltimoComprobanteAutorizadoResponseType();
        response.setNumeroComprobante(456);

        WsmtxcaLastVoucherResponse mapped = WsmtxcaMapper.toLastVoucherResponse(response);
        assertThat(mapped).isNotNull();
        assertThat(mapped.lastVoucherNumber()).isEqualTo(456L);
    }

    @Test
    void toAuthorizeRequestMapsAllFieldsCorrectly() throws Exception {
        WsmtxcaVoucherRequest request = new WsmtxcaVoucherRequest(
                (short) 1,
                1,
                124L,
                LocalDate.of(2026, 7, 15),
                (short) 80,
                30000000007L,
                (short) 1,
                BigDecimal.valueOf(100.00),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(100.00),
                BigDecimal.ZERO,
                BigDecimal.valueOf(121.00),
                "PES",
                BigDecimal.ONE,
                "S",
                "Comments text",
                (short) 1,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 7, 30),
                List.of(new ItemDetail(1, "gtin-123", "code-456", "Product desc", BigDecimal.TEN, (short) 7, BigDecimal.TEN, BigDecimal.ZERO, (short) 5, BigDecimal.valueOf(21.00), BigDecimal.valueOf(121.00))),
                List.of(new TaxDetail((short) 5, BigDecimal.valueOf(100.00), BigDecimal.valueOf(21.00))),
                List.of(),
                List.of(new AssociatedVoucher((short) 1, 1, 120L, null))
        );

        AutorizarComprobanteRequestType soapReq = WsmtxcaMapper.toAuthorizeRequest(ticket, companyCuit, request);
        assertThat(soapReq).isNotNull();
        assertThat(soapReq.getAuthRequest().getToken()).isEqualTo("token-123");
        assertThat(soapReq.getComprobanteCAERequest().getCodigoTipoComprobante()).isEqualTo((short) 1);
        assertThat(soapReq.getComprobanteCAERequest().getNumeroComprobante()).isEqualTo(124);
        assertThat(soapReq.getComprobanteCAERequest().getArrayItems().getItem()).hasSize(1);
        assertThat(soapReq.getComprobanteCAERequest().getArrayItems().getItem().get(0).getDescripcion()).isEqualTo("Product desc");
    }

    @Test
    void toVoucherResponseMapsCorrectly() throws Exception {
        AutorizarComprobanteResponseType response = new AutorizarComprobanteResponseType();
        response.setResultado(ResultadoSimpleType.A);

        ComprobanteCAEResponseType caeResponse = new ComprobanteCAEResponseType();
        caeResponse.setCodigoTipoComprobante((short) 1);
        caeResponse.setNumeroPuntoVenta(1);
        caeResponse.setNumeroComprobante(124);
        caeResponse.setCAE(12345678901234L);
        caeResponse.setFechaEmision(DatatypeFactory.newInstance().newXMLGregorianCalendar("2026-07-15"));
        caeResponse.setFechaVencimientoCAE(DatatypeFactory.newInstance().newXMLGregorianCalendar("2026-07-25"));
        response.setComprobanteResponse(caeResponse);

        WsmtxcaVoucherResponse domainResponse = WsmtxcaMapper.toVoucherResponse(response);
        assertThat(domainResponse).isNotNull();
        assertThat(domainResponse.voucherType()).isEqualTo((short) 1);
        assertThat(domainResponse.salesPoint()).isEqualTo(1);
        assertThat(domainResponse.voucherNumber()).isEqualTo(124L);
        assertThat(domainResponse.authorizationCode()).isEqualTo("12345678901234");
        assertThat(domainResponse.status()).isEqualTo("A");
        assertThat(domainResponse.expirationDate()).isEqualTo(LocalDate.of(2026, 7, 25));
    }
}
