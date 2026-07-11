package io.github.fr4ncisx.arca.wsfev1.internal.usecase.lastvoucher;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.Err;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECompUltimoAutorizado;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FERecuperaLastCbteResponse;
import io.github.fr4ncisx.arca.wsfev1.model.lastvoucher.LastVoucherRequest;
import io.github.fr4ncisx.arca.wsfev1.model.lastvoucher.LastVoucherResponse;

/**
 * Encapsulated package-private translator for Last Voucher query requests and responses.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
final class LastVoucherMapper {

    private LastVoucherMapper() {
    }

    static FECompUltimoAutorizado toSoapRequest(FEAuthRequest auth, LastVoucherRequest request) {
        if (auth == null) {
            throw new ArcaValidationException("auth must not be null");
        }
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        FECompUltimoAutorizado soapRequest = new FECompUltimoAutorizado();
        soapRequest.setAuth(auth);
        soapRequest.setPtoVta(request.salesPoint());
        soapRequest.setCbteTipo(request.voucherType().code());
        return soapRequest;
    }

    static LastVoucherResponse toDomainResponse(
            FERecuperaLastCbteResponse result,
            LastVoucherRequest request) {
        if (result == null) {
            throw new ArcaSoapException("Received empty result from ARCA SOAP service");
        }

        if (result.getErrors() != null && result.getErrors().getErr() != null
                && !result.getErrors().getErr().isEmpty()) {
            Err error = result.getErrors().getErr().get(0);
            throw new ArcaSoapException("ARCA SOAP Error [" + error.getCode() + "]: " + error.getMsg());
        }

        int salesPoint = result.getPtoVta() > 0 ? result.getPtoVta() : request.salesPoint();
        return new LastVoucherResponse(salesPoint, request.voucherType(), result.getCbteNro());
    }
}
