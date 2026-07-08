package io.github.fr4ncisx.arca.wsfev1.internal.mapper;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.model.CaeResponse;
import io.github.fr4ncisx.arca.wsfev1.model.LastVoucherRequest;
import io.github.fr4ncisx.arca.wsfev1.model.LastVoucherResponse;
import io.github.fr4ncisx.arca.wsfev1.model.VoucherType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link WsfeResponseMapper}.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
class WsfeResponseMapperTest {

    @Test
    void toDomainResponseForLastVoucherMapsSuccessResponse() {
        var result = new FERecuperaLastCbteResponse();
        result.setPtoVta(1);
        result.setCbteNro(108);

        var request = new LastVoucherRequest(1, VoucherType.INVOICE_A);
        LastVoucherResponse response = WsfeResponseMapper.toDomainResponse(result, request);

        assertThat(response).isNotNull();
        assertThat(response.salesPoint()).isEqualTo(1);
        assertThat(response.voucherType()).isEqualTo(VoucherType.INVOICE_A);
        assertThat(response.lastNumber()).isEqualTo(108);
    }

    @Test
    void toDomainResponseForLastVoucherThrowsExceptionOnErrors() {
        var result = new FERecuperaLastCbteResponse();

        var errorsArray = new ArrayOfErr();
        var err = new Err();
        err.setCode(600);
        err.setMsg("Invalid CUIT in auth header");
        errorsArray.getErr().add(err);
        result.setErrors(errorsArray);

        var request = new LastVoucherRequest(1, VoucherType.INVOICE_A);
        assertThatThrownBy(() -> WsfeResponseMapper.toDomainResponse(result, request))
                .isInstanceOf(ArcaSoapException.class)
                .hasMessageContaining("ARCA SOAP Error [600]")
                .hasMessageContaining("Invalid CUIT in auth header");
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

        CaeResponse response = WsfeResponseMapper.toDomainResponse(result);

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

        CaeResponse response = WsfeResponseMapper.toDomainResponse(result);

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
