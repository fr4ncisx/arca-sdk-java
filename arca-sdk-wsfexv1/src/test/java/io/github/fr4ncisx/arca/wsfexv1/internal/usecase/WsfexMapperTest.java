package io.github.fr4ncisx.arca.wsfexv1.internal.usecase;

import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXAuthRequest;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXErr;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXEvents;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXGetCMP;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXGetCMPR;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXLastCMP;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXLastCMPResponse;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXOutAuthorize;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXRequest;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXGetCMPResponseDataType;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXResponseAuthorize;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXResponseLastCMP;
import io.github.fr4ncisx.arca.wsfexv1.model.AssociatedVoucher;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportActivity;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportItem;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportOptionalField;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportPermit;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherConsultRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherConsultResponse;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherDetail;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherResponse;
import io.github.fr4ncisx.arca.wsfexv1.model.LastExportVoucherRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.LastExportVoucherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WsfexMapperTest {

    private ArcaAccessTicket ticket;
    private Cuit companyCuit;

    @BeforeEach
    void setUp() {
        ticket = new ArcaAccessTicket("token-123", "sign-456", Instant.now(), Instant.now().plusSeconds(3600));
        companyCuit = Cuit.parse("20-33333333-4");
    }

    @Test
    void toAuthRequestMapsCorrectly() {
        ClsFEXAuthRequest auth = WsfexMapper.toAuthRequest(ticket, companyCuit);
        assertThat(auth).isNotNull();
        assertThat(auth.getToken()).isEqualTo("token-123");
        assertThat(auth.getSign()).isEqualTo("sign-456");
        assertThat(auth.getCuit()).isEqualTo(20333333334L);
    }

    @Test
    void toLastCmpRequestMapsCorrectly() {
        LastExportVoucherRequest request = new LastExportVoucherRequest(2, (short) 19);
        ClsFEXLastCMP soapReq = WsfexMapper.toLastCmpRequest(ticket, companyCuit, request);
        assertThat(soapReq).isNotNull();
        assertThat(soapReq.getToken()).isEqualTo("token-123");
        assertThat(soapReq.getSign()).isEqualTo("sign-456");
        assertThat(soapReq.getCuit()).isEqualTo(20333333334L);
        assertThat(soapReq.getPtoVenta()).isEqualTo(2);
        assertThat(soapReq.getCbteTipo()).isEqualTo((short) 19);
    }

    @Test
    void toFexRequestMapsAllFieldsCorrectly() {
        ExportVoucherRequest request = new ExportVoucherRequest(
                12345L,
                "20260714",
                (short) 19,
                2,
                15L,
                (short) 2,
                "S",
                List.of(new ExportPermit("P-99", 200)),
                (short) 250,
                "Foreign Client SRL",
                50001000L,
                "Fake Street 123",
                "US-TAX-123",
                "DOL",
                BigDecimal.valueOf(950.50),
                "N",
                "No obs",
                BigDecimal.valueOf(15000.00),
                "Observations text",
                List.of(new AssociatedVoucher((short) 19, 2, 14L, 20333333334L)),
                "Wire Transfer",
                "FOB",
                "Free on Board",
                (short) 2,
                List.of(new ExportItem("ITEM-01", "Exported Goods", BigDecimal.TEN, 1, BigDecimal.valueOf(1500.00), BigDecimal.ZERO, BigDecimal.valueOf(15000.00))),
                List.of(new ExportOptionalField("90", "CustomValue")),
                "20260731",
                List.of(new ExportActivity(1001L))
        );

        ClsFEXRequest soapReq = WsfexMapper.toFexRequest(request);

        assertThat(soapReq).isNotNull();
        assertThat(soapReq.getId()).isEqualTo(12345L);
        assertThat(soapReq.getFechaCbte()).isEqualTo("20260714");
        assertThat(soapReq.getCbteTipo()).isEqualTo((short) 19);
        assertThat(soapReq.getPuntoVta()).isEqualTo(2);
        assertThat(soapReq.getCbteNro()).isEqualTo(15L);
        assertThat(soapReq.getTipoExpo()).isEqualTo((short) 2);
        assertThat(soapReq.getPermisoExistente()).isEqualTo("S");
        assertThat(soapReq.getDstCmp()).isEqualTo((short) 250);
        assertThat(soapReq.getCliente()).isEqualTo("Foreign Client SRL");
        assertThat(soapReq.getCuitPaisCliente()).isEqualTo(50001000L);
        assertThat(soapReq.getDomicilioCliente()).isEqualTo("Fake Street 123");
        assertThat(soapReq.getIdImpositivo()).isEqualTo("US-TAX-123");
        assertThat(soapReq.getMonedaId()).isEqualTo("DOL");
        assertThat(soapReq.getMonedaCtz()).isEqualTo(BigDecimal.valueOf(950.50));
        assertThat(soapReq.getCanMisMonExt()).isEqualTo("N");
        assertThat(soapReq.getObsComerciales()).isEqualTo("No obs");
        assertThat(soapReq.getImpTotal()).isEqualTo(BigDecimal.valueOf(15000.00));
        assertThat(soapReq.getObs()).isEqualTo("Observations text");
        assertThat(soapReq.getFormaPago()).isEqualTo("Wire Transfer");
        assertThat(soapReq.getIncoterms()).isEqualTo("FOB");
        assertThat(soapReq.getIncotermsDs()).isEqualTo("Free on Board");
        assertThat(soapReq.getIdiomaCbte()).isEqualTo((short) 2);
        assertThat(soapReq.getFechaPago()).isEqualTo("20260731");

        assertThat(soapReq.getPermisos()).isNotNull();
        assertThat(soapReq.getPermisos().getPermiso()).hasSize(1);
        assertThat(soapReq.getPermisos().getPermiso().get(0).getIdPermiso()).isEqualTo("P-99");
        assertThat(soapReq.getPermisos().getPermiso().get(0).getDstMerc()).isEqualTo(200);

        assertThat(soapReq.getCmpsAsoc()).isNotNull();
        assertThat(soapReq.getCmpsAsoc().getCmpAsoc()).hasSize(1);
        assertThat(soapReq.getCmpsAsoc().getCmpAsoc().get(0).getCbteNro()).isEqualTo(14L);

        assertThat(soapReq.getItems()).isNotNull();
        assertThat(soapReq.getItems().getItem()).hasSize(1);
        assertThat(soapReq.getItems().getItem().get(0).getProCodigo()).isEqualTo("ITEM-01");

        assertThat(soapReq.getOpcionales()).isNotNull();
        assertThat(soapReq.getOpcionales().getOpcional()).hasSize(1);
        assertThat(soapReq.getOpcionales().getOpcional().get(0).getId()).isEqualTo("90");

        assertThat(soapReq.getActividades()).isNotNull();
        assertThat(soapReq.getActividades().getActividad()).hasSize(1);
        assertThat(soapReq.getActividades().getActividad().get(0).getId()).isEqualTo(1001L);
    }

    @Test
    void toExportVoucherResponseMapsCorrectly() {
        FEXResponseAuthorize response = new FEXResponseAuthorize();
        ClsFEXOutAuthorize result = new ClsFEXOutAuthorize();
        result.setId(123L);
        result.setCuit(20333333334L);
        result.setCbteTipo((short) 19);
        result.setPuntoVta(2);
        result.setCbteNro(15L);
        result.setCae("CAE-999");
        result.setFchVencCae("20260731");
        response.setFEXResultAuth(result);

        ClsFEXErr err = new ClsFEXErr();
        err.setErrCode(501);
        err.setErrMsg("Some business error");
        response.setFEXErr(err);

        ClsFEXEvents event = new ClsFEXEvents();
        event.setEventCode(10);
        event.setEventMsg("Some event notice");
        response.setFEXEvents(event);

        ExportVoucherResponse domainResponse = WsfexMapper.toExportVoucherResponse(response);

        assertThat(domainResponse).isNotNull();
        assertThat(domainResponse.id()).isEqualTo(123L);
        assertThat(domainResponse.cuit()).isEqualTo(20333333334L);
        assertThat(domainResponse.voucherType()).isEqualTo((short) 19);
        assertThat(domainResponse.salesPoint()).isEqualTo(2);
        assertThat(domainResponse.voucherNumber()).isEqualTo(15L);
        assertThat(domainResponse.cae()).isEqualTo("CAE-999");
        assertThat(domainResponse.caeExpirationDate()).isEqualTo("20260731");
        assertThat(domainResponse.isApproved()).isTrue();

        assertThat(domainResponse.errors()).hasSize(1);
        assertThat(domainResponse.errors().get(0).code()).isEqualTo(501);
        assertThat(domainResponse.errors().get(0).message()).isEqualTo("Some business error");

        assertThat(domainResponse.events()).hasSize(1);
        assertThat(domainResponse.events().get(0).code()).isEqualTo(10);
        assertThat(domainResponse.events().get(0).message()).isEqualTo("Some event notice");
    }
}
