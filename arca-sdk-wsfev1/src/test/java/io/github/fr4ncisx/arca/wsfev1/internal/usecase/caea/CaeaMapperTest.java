package io.github.fr4ncisx.arca.wsfev1.internal.usecase.caea;

import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.common.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeVatLine;
import io.github.fr4ncisx.arca.wsfev1.model.caea.*;
import io.github.fr4ncisx.arca.wsfev1.model.common.ConceptType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VatType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CaeaMapper}.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
class CaeaMapperTest {

    private ArcaAccessTicket ticket;
    private Cuit companyCuit;

    @BeforeEach
    void setUp() {
        ticket = new ArcaAccessTicket("token-123", "sign-456", Instant.now(), Instant.now().plusSeconds(3600));
        companyCuit = Cuit.parse("20-33333333-4");
    }

    @Test
    void toSoapRequestForRequestCaeaMapsFieldsCorrectly() {
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, companyCuit);
        CaeaRequest request = new CaeaRequest(202607, 1);

        FECAEASolicitar soapRequest = CaeaMapper.toSoapRequest(auth, request);

        assertThat(soapRequest).isNotNull();
        assertThat(soapRequest.getAuth()).isSameAs(auth);
        assertThat(soapRequest.getPeriodo()).isEqualTo(202607);
        assertThat(soapRequest.getOrden()).isEqualTo((short) 1);
    }

    @Test
    void toDomainResponseForCaeaResponseMapsFieldsCorrectly() {
        FECAEAGetResponse soapResponse = new FECAEAGetResponse();

        FECAEAGet resultGet = new FECAEAGet();
        resultGet.setCAEA("caea-123456");
        resultGet.setPeriodo(202607);
        resultGet.setOrden((short) 1);
        resultGet.setFchVigDesde("20260701");
        resultGet.setFchVigHasta("20260715");
        resultGet.setFchTopeInf("20260720");

        ArrayOfObs obsArray = new ArrayOfObs();
        Obs obs = new Obs();
        obs.setCode(2001);
        obs.setMsg("Warning test");
        obsArray.getObs().add(obs);
        resultGet.setObservaciones(obsArray);

        soapResponse.setResultGet(resultGet);

        ArrayOfErr errArray = new ArrayOfErr();
        Err err = new Err();
        err.setCode(1001);
        err.setMsg("Error test");
        errArray.getErr().add(err);
        soapResponse.setErrors(errArray);

        CaeaResponse domainResponse = CaeaMapper.toDomainResponse(soapResponse);

        assertThat(domainResponse).isNotNull();
        assertThat(domainResponse.caea()).isEqualTo("caea-123456");
        assertThat(domainResponse.period()).isEqualTo(202607);
        assertThat(domainResponse.order()).isEqualTo(1);
        assertThat(domainResponse.startDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(domainResponse.endDate()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(domainResponse.expirationDate()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(domainResponse.errors()).hasSize(1);
        assertThat(domainResponse.errors().get(0).code()).isEqualTo(1001);
        assertThat(domainResponse.errors().get(0).message()).isEqualTo("Error test");
        assertThat(domainResponse.observations()).hasSize(1);
        assertThat(domainResponse.observations().get(0).code()).isEqualTo(2001);
        assertThat(domainResponse.observations().get(0).message()).isEqualTo("Warning test");
    }

    @Test
    void toSoapRequestForQueryCaeaMapsFieldsCorrectly() {
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, companyCuit);
        CaeaQuery query = new CaeaQuery(202607, 2);

        FECAEAConsultar soapRequest = CaeaMapper.toSoapRequest(auth, query);

        assertThat(soapRequest).isNotNull();
        assertThat(soapRequest.getAuth()).isSameAs(auth);
        assertThat(soapRequest.getPeriodo()).isEqualTo(202607);
        assertThat(soapRequest.getOrden()).isEqualTo((short) 2);
    }

    @Test
    void toSoapRequestForReportCaeaMapsFieldsCorrectly() {
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, companyCuit);
        CaeaReportRequest request = new CaeaReportRequest(
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
        );

        FECAEARegInformativo soapRequest = CaeaMapper.toSoapRequest(auth, request);

        assertThat(soapRequest).isNotNull();
        assertThat(soapRequest.getAuth()).isSameAs(auth);
        assertThat(soapRequest.getFeCAEARegInfReq()).isNotNull();

        var cab = soapRequest.getFeCAEARegInfReq().getFeCabReq();
        assertThat(cab.getCantReg()).isEqualTo(1);
        assertThat(cab.getPtoVta()).isEqualTo(2);
        assertThat(cab.getCbteTipo()).isEqualTo(VoucherType.INVOICE_A.code());

        var details = soapRequest.getFeCAEARegInfReq().getFeDetReq().getFECAEADetRequest();
        assertThat(details).hasSize(1);
        var det = details.get(0);
        assertThat(det.getCAEA()).isEqualTo("caea-123");
        assertThat(det.getConcepto()).isEqualTo(ConceptType.PRODUCTS.code());
        assertThat(det.getDocNro()).isEqualTo(27444444449L);
        assertThat(det.getCbteDesde()).isEqualTo(105L);
        assertThat(det.getCbteFch()).isEqualTo("20260707");
        assertThat(det.getImpTotal()).isEqualTo(121.0);
        assertThat(det.getImpNeto()).isEqualTo(100.0);
        assertThat(det.getImpIVA()).isEqualTo(21.0);

        var vatArray = det.getIva();
        assertThat(vatArray).isNotNull();
        assertThat(vatArray.getAlicIva()).hasSize(1);
        var vat = vatArray.getAlicIva().get(0);
        assertThat(vat.getId()).isEqualTo(VatType.VAT_21.code());
        assertThat(vat.getBaseImp()).isEqualTo(100.0);
        assertThat(vat.getImporte()).isEqualTo(21.0);
    }

    @Test
    void toDomainResponseForCaeaReportResponseMapsFieldsCorrectly() {
        FECAEAResponse soapResponse = new FECAEAResponse();

        FECAEACabResponse cab = new FECAEACabResponse();
        cab.setResultado("P");
        soapResponse.setFeCabResp(cab);

        FECAEADetResponse det = new FECAEADetResponse();
        det.setCbteDesde(105L);
        det.setResultado("A");

        ArrayOfObs obsArray = new ArrayOfObs();
        Obs obs = new Obs();
        obs.setCode(2001);
        obs.setMsg("Obs test");
        obsArray.getObs().add(obs);
        det.setObservaciones(obsArray);

        ArrayOfFECAEADetResponse detArray = new ArrayOfFECAEADetResponse();
        detArray.getFECAEADetResponse().add(det);
        soapResponse.setFeDetResp(detArray);

        ArrayOfErr globalErrArray = new ArrayOfErr();
        Err globalErr = new Err();
        globalErr.setCode(1001);
        globalErr.setMsg("Global error");
        globalErrArray.getErr().add(globalErr);
        soapResponse.setErrors(globalErrArray);

        CaeaReportResponse domainResponse = CaeaMapper.toDomainResponse(soapResponse);

        assertThat(domainResponse).isNotNull();
        assertThat(domainResponse.result()).isEqualTo("P");
        assertThat(domainResponse.errors()).hasSize(1);
        assertThat(domainResponse.errors().get(0).code()).isEqualTo(1001);
        assertThat(domainResponse.errors().get(0).message()).isEqualTo("Global error");

        assertThat(domainResponse.results()).hasSize(1);
        var detailResult = domainResponse.results().get(0);
        assertThat(detailResult.number()).isEqualTo(105L);
        assertThat(detailResult.result()).isEqualTo("A");
        assertThat(detailResult.errors()).isEmpty();
        assertThat(detailResult.observations()).hasSize(1);
        assertThat(detailResult.observations().get(0).code()).isEqualTo(2001);
        assertThat(detailResult.observations().get(0).message()).isEqualTo("Obs test");
    }

    @Test
    void toSoapRequestForReportCaeaNoMovementMapsFieldsCorrectly() {
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, companyCuit);
        CaeaNoMovementRequest request = new CaeaNoMovementRequest("caea-123", 1);

        FECAEASinMovimientoInformar soapRequest = CaeaMapper.toSoapRequest(auth, request);

        assertThat(soapRequest).isNotNull();
        assertThat(soapRequest.getAuth()).isSameAs(auth);
        assertThat(soapRequest.getCAEA()).isEqualTo("caea-123");
        assertThat(soapRequest.getPtoVta()).isEqualTo(1);
    }

    @Test
    void toSoapRequestForQueryCaeaNoMovementMapsFieldsCorrectly() {
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, companyCuit);
        CaeaNoMovementQuery query = new CaeaNoMovementQuery("caea-123", 1);

        FECAEASinMovimientoConsultar soapRequest = CaeaMapper.toSoapRequest(auth, query);

        assertThat(soapRequest).isNotNull();
        assertThat(soapRequest.getAuth()).isSameAs(auth);
        assertThat(soapRequest.getCAEA()).isEqualTo("caea-123");
        assertThat(soapRequest.getPtoVta()).isEqualTo(1);
    }
}
