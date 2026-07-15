package io.github.fr4ncisx.arca.wscdc.internal.usecase;

import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wscdc.internal.generated.CmpAuthRequest;
import io.github.fr4ncisx.arca.wscdc.internal.generated.CmpDatos;
import io.github.fr4ncisx.arca.wscdc.internal.generated.CmpResponse;
import io.github.fr4ncisx.arca.wscdc.internal.generated.DummyResponse;
import io.github.fr4ncisx.arca.wscdc.internal.generated.Err;
import io.github.fr4ncisx.arca.wscdc.internal.generated.Obs;
import io.github.fr4ncisx.arca.wscdc.internal.generated.ArrayOfObs;
import io.github.fr4ncisx.arca.wscdc.internal.generated.ArrayOfErr;
import io.github.fr4ncisx.arca.wscdc.model.WscdcConstatRequest;
import io.github.fr4ncisx.arca.wscdc.model.WscdcConstatResponse;
import io.github.fr4ncisx.arca.wscdc.model.WscdcDummyResponse;
import io.github.fr4ncisx.arca.wscdc.model.WscdcOptionalField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the WscdcMapper maps correctly between internal stubs and external domain records.
 *
 * @author fr4ncisx
 * @since 0.9.0
 */
class WscdcMapperTest {

    private ArcaAccessTicket ticket;
    private Cuit companyCuit;

    /**
     * Initializes the common testing ticket and CUIT.
     */
    @BeforeEach
    void setUp() {
        ticket = new ArcaAccessTicket("token-123", "sign-456", Instant.now(), Instant.now().plusSeconds(3600));
        companyCuit = Cuit.parse("20-30000000-3");
    }

    /**
     * Validates that mapAuth successfully maps authentication tickets.
     */
    @Test
    void mapsAuthenticationCorrectly() {
        CmpAuthRequest auth = WscdcMapper.mapAuth(ticket, companyCuit);
        assertThat(auth).isNotNull();
        assertThat(auth.getToken()).isEqualTo("token-123");
        assertThat(auth.getSign()).isEqualTo("sign-456");
        assertThat(auth.getCuit()).isEqualTo(20300000003L);
    }

    /**
     * Validates that mapRequest maps all parameters from WscdcConstatRequest to JAX-WS stub structures.
     */
    @Test
    void mapsRequestCorrectly() {
        WscdcConstatRequest request = new WscdcConstatRequest(
            "CAE",
            companyCuit,
            1,
            1,
            100L,
            LocalDate.of(2026, 7, 15),
            new BigDecimal("121.00"),
            "12345678901234",
            "80",
            "20300000007",
            List.of(new WscdcOptionalField("61", "1"))
        );

        CmpDatos data = WscdcMapper.mapRequest(request);
        assertThat(data).isNotNull();
        assertThat(data.getCbteModo()).isEqualTo("CAE");
        assertThat(data.getCuitEmisor()).isEqualTo(20300000003L);
        assertThat(data.getPtoVta()).isEqualTo(1);
        assertThat(data.getCbteTipo()).isEqualTo(1);
        assertThat(data.getCbteNro()).isEqualTo(100L);
        assertThat(data.getCbteFch()).isEqualTo("20260715");
        assertThat(data.getImpTotal()).isEqualTo(121.00);
        assertThat(data.getCodAutorizacion()).isEqualTo("12345678901234");
        assertThat(data.getDocTipoReceptor()).isEqualTo("80");
        assertThat(data.getDocNroReceptor()).isEqualTo("20300000007");
        assertThat(data.getOpcionales()).isNotNull();
        assertThat(data.getOpcionales().getOpcional()).hasSize(1);
        assertThat(data.getOpcionales().getOpcional().get(0).getId()).isEqualTo("61");
        assertThat(data.getOpcionales().getOpcional().get(0).getValor()).isEqualTo("1");
    }

    /**
     * Validates that mapResponse maps approved response stubs into domain response records.
     */
    @Test
    void mapsApprovedResponseCorrectly() {
        CmpResponse response = new CmpResponse();
        response.setResultado("A");
        response.setFchProceso("20260715103000");

        WscdcConstatResponse mapped = WscdcMapper.mapResponse(response);
        assertThat(mapped).isNotNull();
        assertThat(mapped.isApproved()).isTrue();
        assertThat(mapped.result()).isEqualTo("A");
        assertThat(mapped.processDate()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(mapped.observations()).isEmpty();
        assertThat(mapped.errors()).isEmpty();
    }

    /**
     * Validates that mapResponse parses observations and errors correctly.
     */
    @Test
    void mapsRejectedResponseWithObservationsAndErrors() {
        CmpResponse response = new CmpResponse();
        response.setResultado("R");
        response.setFchProceso("20260715");

        ArrayOfObs obsArray = new ArrayOfObs();
        Obs obs = new Obs();
        obs.setCode(10015);
        obs.setMsg("El comprobante no existe");
        obsArray.getObs().add(obs);
        response.setObservaciones(obsArray);

        ArrayOfErr errArray = new ArrayOfErr();
        Err err = new Err();
        err.setCode(600);
        err.setMsg("Validacion fallida");
        errArray.getErr().add(err);
        response.setErrors(errArray);

        WscdcConstatResponse mapped = WscdcMapper.mapResponse(response);
        assertThat(mapped).isNotNull();
        assertThat(mapped.isApproved()).isFalse();
        assertThat(mapped.result()).isEqualTo("R");
        assertThat(mapped.processDate()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(mapped.observations()).hasSize(1);
        assertThat(mapped.observations().get(0).code()).isEqualTo(10015);
        assertThat(mapped.observations().get(0).message()).isEqualTo("El comprobante no existe");
        assertThat(mapped.errors()).hasSize(1);
        assertThat(mapped.errors().get(0).code()).isEqualTo(600);
        assertThat(mapped.errors().get(0).message()).isEqualTo("Validacion fallida");
    }

    /**
     * Validates that mapDummy translates DummyResponse stubs to domain records.
     */
    @Test
    void mapsDummyResponseCorrectly() {
        DummyResponse dummy = new DummyResponse();
        dummy.setAppServer("OK");
        dummy.setDbServer("OK");
        dummy.setAuthServer("OK");

        WscdcDummyResponse mapped = WscdcMapper.mapDummy(dummy);
        assertThat(mapped).isNotNull();
        assertThat(mapped.isOk()).isTrue();
        assertThat(mapped.appServer()).isEqualTo("OK");
        assertThat(mapped.dbServer()).isEqualTo("OK");
        assertThat(mapped.authServer()).isEqualTo("OK");
    }
}
