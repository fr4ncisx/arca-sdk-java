package io.github.fr4ncisx.arca.wsfev1.internal.usecase.voucher;

import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.VoucherMapper;
import io.github.fr4ncisx.arca.wsfev1.model.common.ConceptType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VatType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;
import io.github.fr4ncisx.arca.wsfev1.model.voucher.VoucherConsultRequest;
import io.github.fr4ncisx.arca.wsfev1.model.voucher.VoucherConsultResponse;
import io.github.fr4ncisx.arca.wsfev1.model.voucher.VoucherDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link VoucherMapper}.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
class VoucherMapperTest {

    private ArcaAccessTicket ticket;
    private Cuit companyCuit;

    @BeforeEach
    void setUp() {
        ticket = new ArcaAccessTicket("token-123", "sign-456", Instant.now(), Instant.now().plusSeconds(3600));
        companyCuit = Cuit.parse("20-33333333-4");
    }

    @Test
    void toSoapRequestMapsFieldsCorrectly() {
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, companyCuit);
        VoucherConsultRequest request = new VoucherConsultRequest(1, VoucherType.INVOICE_A, 42L);

        FECompConsultar soapRequest = VoucherMapper.toSoapRequest(auth, request);

        assertThat(soapRequest).isNotNull();
        assertThat(soapRequest.getAuth()).isSameAs(auth);
        assertThat(soapRequest.getFeCompConsReq()).isNotNull();
        assertThat(soapRequest.getFeCompConsReq().getCbteTipo()).isEqualTo(VoucherType.INVOICE_A.code());
        assertThat(soapRequest.getFeCompConsReq().getPtoVta()).isEqualTo(1);
        assertThat(soapRequest.getFeCompConsReq().getCbteNro()).isEqualTo(42L);
    }

    @Test
    void toDomainResponseMapsVoucherDetailCorrectly() {
        FECompConsultaResponse result = new FECompConsultaResponse();
        
        FECompConsResponse soapDetail = new FECompConsResponse();
        soapDetail.setCbteTipo(VoucherType.INVOICE_A.code());
        soapDetail.setPtoVta(1);
        soapDetail.setCbteDesde(42L);
        soapDetail.setConcepto(ConceptType.PRODUCTS.code());
        soapDetail.setDocNro(20333333334L);
        soapDetail.setImpNeto(100.0);
        soapDetail.setImpTotConc(0.0);
        soapDetail.setImpOpEx(0.0);
        soapDetail.setImpIVA(21.0);
        soapDetail.setImpTotal(121.0);
        soapDetail.setCbteFch("20260707");
        soapDetail.setCodAutorizacion("cae-424242");
        soapDetail.setFchVto("20260717");
        soapDetail.setResultado("A");

        ArrayOfAlicIva arrayIva = new ArrayOfAlicIva();
        AlicIva alic = new AlicIva();
        alic.setId(VatType.VAT_21.code());
        alic.setBaseImp(100.0);
        alic.setImporte(21.0);
        arrayIva.getAlicIva().add(alic);
        soapDetail.setIva(arrayIva);

        result.setResultGet(soapDetail);

        VoucherConsultResponse response = VoucherMapper.toDomainResponse(result);

        assertThat(response).isNotNull();
        assertThat(response.detail()).isNotNull();
        
        VoucherDetail detail = response.detail();
        assertThat(detail.voucherType()).isEqualTo(VoucherType.INVOICE_A);
        assertThat(detail.salesPoint()).isEqualTo(1);
        assertThat(detail.number()).isEqualTo(42L);
        assertThat(detail.concept()).isEqualTo(ConceptType.PRODUCTS);
        assertThat(detail.customerCuit().number()).isEqualTo(20333333334L);
        assertThat(detail.netTaxed()).isEqualTo(100.0);
        assertThat(detail.vatTotal()).isEqualTo(21.0);
        assertThat(detail.total()).isEqualTo(121.0);
        assertThat(detail.date()).isEqualTo(LocalDate.of(2026, 7, 7));
        assertThat(detail.cae()).isEqualTo("cae-424242");
        assertThat(detail.expirationDate()).isEqualTo(LocalDate.of(2026, 7, 17));
        assertThat(detail.result()).isEqualTo("A");
        assertThat(detail.vatLines()).hasSize(1);
        assertThat(detail.vatLines().get(0).vatType()).isEqualTo(VatType.VAT_21);
    }
}
