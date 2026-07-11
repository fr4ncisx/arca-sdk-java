package io.github.fr4ncisx.arca.wsfev1.internal.usecase.cae;

import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.common.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeRequest;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeResponse;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeVatLine;
import io.github.fr4ncisx.arca.wsfev1.model.common.ConceptType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VatType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CaeMapper}.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
class CaeMapperTest {

    private ArcaAccessTicket ticket;
    private Cuit companyCuit;

    @BeforeEach
    void setUp() {
        ticket = new ArcaAccessTicket("token-123", "sign-456", Instant.now(), Instant.now().plusSeconds(3600));
        companyCuit = Cuit.parse("20-33333333-4");
    }

    @Test
    void toSoapRequestForCaeMapsFieldsCorrectly() {
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, companyCuit);
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

        FECAESolicitar soapRequest = CaeMapper.toSoapRequest(auth, request);

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

    @Test
    void toDomainResponseForCaeMapsSuccessResponse() {
        var result = new FECAEResponse();

        var cab = new FECAECabResponse();
        cab.setResultado("A");
        result.setFeCabResp(cab);

        var detResponse = new FECAEDetResponse();
        detResponse.setCAE("cae-1234567");
        detResponse.setCAEFchVto("20260717");
        var detArray = new ArrayOfFECAEDetResponse();
        detArray.getFECAEDetResponse().add(detResponse);
        result.setFeDetResp(detArray);

        CaeResponse response = CaeMapper.toDomainResponse(result);

        assertThat(response).isNotNull();
        assertThat(response.isApproved()).isTrue();
        assertThat(response.cae()).isEqualTo("cae-1234567");
        assertThat(response.caeExpiration()).isEqualTo(LocalDate.of(2026, 7, 17));
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void toDomainResponseForCaeMapsRejectedResponseWithErrorsAndObservations() {
        var result = new FECAEResponse();

        var cab = new FECAECabResponse();
        cab.setResultado("R");
        result.setFeCabResp(cab);

        // Header errors
        var errorsArray = new ArrayOfErr();
        var err = new Err();
        err.setCode(1001);
        err.setMsg("Invalid voucher type");
        errorsArray.getErr().add(err);
        result.setErrors(errorsArray);

        // Details observations
        var detResponse = new FECAEDetResponse();
        var obsArray = new ArrayOfObs();
        var obs = new Obs();
        obs.setCode(2001);
        obs.setMsg("VAT rates mismatch");
        obsArray.getObs().add(obs);
        detResponse.setObservaciones(obsArray);

        var detArray = new ArrayOfFECAEDetResponse();
        detArray.getFECAEDetResponse().add(detResponse);
        result.setFeDetResp(detArray);

        CaeResponse response = CaeMapper.toDomainResponse(result);

        assertThat(response).isNotNull();
        assertThat(response.isApproved()).isFalse();
        assertThat(response.cae()).isNull();
        assertThat(response.caeExpiration()).isNull();
        assertThat(response.errors()).hasSize(2);

        assertThat(response.errors().get(0).code()).isEqualTo(1001);
        assertThat(response.errors().get(0).message()).isEqualTo("Invalid voucher type");

        assertThat(response.errors().get(1).code()).isEqualTo(2001);
        assertThat(response.errors().get(1).message()).isEqualTo("VAT rates mismatch");
    }
}
