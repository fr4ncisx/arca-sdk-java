package io.github.fr4ncisx.arca.wsfev1.internal.mapper;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECAESolicitar;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECompUltimoAutorizado;
import io.github.fr4ncisx.arca.wsfev1.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link WsfeRequestMapper}.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
class WsfeRequestMapperTest {

    private ArcaAccessTicket ticket;
    private Cuit companyCuit;

    @BeforeEach
    void setUp() {
        ticket = new ArcaAccessTicket("token-123", "sign-456", Instant.now(), Instant.now().plusSeconds(3600));
        companyCuit = Cuit.parse("20-33333333-4");
    }

    @Test
    void toAuthRequestMapsFieldsCorrectly() {
        FEAuthRequest auth = WsfeRequestMapper.toAuthRequest(ticket, companyCuit);

        assertThat(auth).isNotNull();
        assertThat(auth.getToken()).isEqualTo("token-123");
        assertThat(auth.getSign()).isEqualTo("sign-456");
        assertThat(auth.getCuit()).isEqualTo(20333333334L);
    }

    @Test
    void toAuthRequestValidatesParameters() {
        assertThatThrownBy(() -> WsfeRequestMapper.toAuthRequest(null, companyCuit))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> WsfeRequestMapper.toAuthRequest(ticket, null))
                .isInstanceOf(ArcaValidationException.class);
    }

    @Test
    void toSoapRequestForLastVoucherMapsFieldsCorrectly() {
        FEAuthRequest auth = WsfeRequestMapper.toAuthRequest(ticket, companyCuit);
        LastVoucherRequest request = new LastVoucherRequest(2, VoucherType.INVOICE_C);

        FECompUltimoAutorizado soapRequest = WsfeRequestMapper.toSoapRequest(auth, request);

        assertThat(soapRequest).isNotNull();
        assertThat(soapRequest.getAuth()).isSameAs(auth);
        assertThat(soapRequest.getPtoVta()).isEqualTo(2);
        assertThat(soapRequest.getCbteTipo()).isEqualTo(VoucherType.INVOICE_C.code());
    }

    @Test
    void toSoapRequestForCaeMapsFieldsCorrectly() {
        FEAuthRequest auth = WsfeRequestMapper.toAuthRequest(ticket, companyCuit);
        CaeRequest request = new CaeRequest(
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

        FECAESolicitar soapRequest = WsfeRequestMapper.toSoapRequest(auth, request);

        assertThat(soapRequest).isNotNull();
        assertThat(soapRequest.getAuth()).isSameAs(auth);
        assertThat(soapRequest.getFeCAEReq()).isNotNull();

        var cab = soapRequest.getFeCAEReq().getFeCabReq();
        assertThat(cab.getCbteTipo()).isEqualTo(VoucherType.INVOICE_A.code());
        assertThat(cab.getPtoVta()).isEqualTo(1);
        assertThat(cab.getCantReg()).isEqualTo(1);

        var detList = soapRequest.getFeCAEReq().getFeDetReq().getFECAEDetRequest();
        assertThat(detList).hasSize(1);

        var det = detList.get(0);
        assertThat(det.getConcepto()).isEqualTo(ConceptType.PRODUCTS.code());
        assertThat(det.getDocTipo()).isEqualTo(80);
        assertThat(det.getDocNro()).isEqualTo(27444444449L);
        assertThat(det.getCbteDesde()).isEqualTo(105L);
        assertThat(det.getCbteHasta()).isEqualTo(105L);
        assertThat(det.getCbteFch()).isEqualTo("20260707");
        assertThat(det.getImpTotal()).isEqualTo(121.0);
        assertThat(det.getImpNeto()).isEqualTo(100.0);
        assertThat(det.getImpIVA()).isEqualTo(21.0);

        var vatArray = det.getIva();
        assertThat(vatArray).isNotNull();
        assertThat(vatArray.getAlicIva()).hasSize(1);
        var vatLine = vatArray.getAlicIva().get(0);
        assertThat(vatLine.getId()).isEqualTo(VatType.VAT_21.code());
        assertThat(vatLine.getBaseImp()).isEqualTo(100.0);
        assertThat(vatLine.getImporte()).isEqualTo(21.0);
    }
}
