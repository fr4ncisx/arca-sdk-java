package io.github.fr4ncisx.arca.wsfev1.internal.usecase.lastvoucher;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.ArrayOfErr;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.Err;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECompUltimoAutorizado;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FERecuperaLastCbteResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.internal.adapter.LastVoucherMapper;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;
import io.github.fr4ncisx.arca.wsfev1.model.lastvoucher.LastVoucherRequest;
import io.github.fr4ncisx.arca.wsfev1.model.lastvoucher.LastVoucherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LastVoucherMapper}.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
class LastVoucherMapperTest {

    private ArcaAccessTicket ticket;
    private Cuit companyCuit;

    @BeforeEach
    void setUp() {
        ticket = new ArcaAccessTicket("token-123", "sign-456", Instant.now(), Instant.now().plusSeconds(3600));
        companyCuit = Cuit.parse("20-33333333-4");
    }

    @Test
    void toSoapRequestForLastVoucherMapsFieldsCorrectly() {
        FEAuthRequest auth = CommonMapper.toAuthRequest(ticket, companyCuit);
        LastVoucherRequest request = new LastVoucherRequest(2, VoucherType.INVOICE_C);

        FECompUltimoAutorizado soapRequest = LastVoucherMapper.toSoapRequest(auth, request);

        assertThat(soapRequest).isNotNull();
        assertThat(soapRequest.getAuth()).isSameAs(auth);
        assertThat(soapRequest.getPtoVta()).isEqualTo(2);
        assertThat(soapRequest.getCbteTipo()).isEqualTo(VoucherType.INVOICE_C.code());
    }

    @Test
    void toDomainResponseForLastVoucherMapsSuccessResponse() {
        var result = new FERecuperaLastCbteResponse();
        result.setPtoVta(1);
        result.setCbteNro(108);

        var request = new LastVoucherRequest(1, VoucherType.INVOICE_A);
        LastVoucherResponse response = LastVoucherMapper.toDomainResponse(result, request);

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
        assertThatThrownBy(() -> LastVoucherMapper.toDomainResponse(result, request))
                .isInstanceOf(ArcaSoapException.class)
                .hasMessageContaining("ARCA SOAP Error [600]")
                .hasMessageContaining("Invalid CUIT in auth header");
    }
}
