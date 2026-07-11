package io.github.fr4ncisx.arca.wsfev1.internal.usecase.voucher;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.common.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeVatLine;
import io.github.fr4ncisx.arca.wsfev1.model.common.AfipError;
import io.github.fr4ncisx.arca.wsfev1.model.common.ConceptType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VatType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;
import io.github.fr4ncisx.arca.wsfev1.model.voucher.VoucherConsultRequest;
import io.github.fr4ncisx.arca.wsfev1.model.voucher.VoucherConsultResponse;
import io.github.fr4ncisx.arca.wsfev1.model.voucher.VoucherDetail;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Encapsulated package-private translator for Voucher query requests and responses.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
final class VoucherMapper {

    private VoucherMapper() {
    }

    static FECompConsultar toSoapRequest(FEAuthRequest auth, VoucherConsultRequest request) {
        if (auth == null) {
            throw new ArcaValidationException("auth must not be null");
        }
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        FECompConsultaReq inner = new FECompConsultaReq();
        inner.setCbteTipo(request.voucherType().code());
        inner.setCbteNro(request.voucherNumber());
        inner.setPtoVta(request.salesPoint());

        FECompConsultar soapRequest = new FECompConsultar();
        soapRequest.setAuth(auth);
        soapRequest.setFeCompConsReq(inner);
        return soapRequest;
    }

    static VoucherConsultResponse toDomainResponse(FECompConsultaResponse result) {
        if (result == null) {
            throw new ArcaSoapException("Received empty result from ARCA SOAP service");
        }

        List<AfipError> errors = new ArrayList<>();
        if (result.getErrors() != null && result.getErrors().getErr() != null) {
            for (Err err : result.getErrors().getErr()) {
                errors.add(new AfipError(err.getCode(), err.getMsg()));
            }
        }

        VoucherDetail detail = null;
        if (result.getResultGet() != null) {
            FECompConsResponse soapDetail = result.getResultGet();

            LocalDate date = null;
            if (soapDetail.getCbteFch() != null && !soapDetail.getCbteFch().trim().isEmpty()) {
                try {
                    date = CommonMapper.parseDate(soapDetail.getCbteFch());
                } catch (Exception e) {
                    throw new ArcaSoapException("Failed to parse voucher date: " + soapDetail.getCbteFch(), e);
                }
            }

            LocalDate expDate = null;
            if (soapDetail.getFchVto() != null && !soapDetail.getFchVto().trim().isEmpty()) {
                try {
                    expDate = CommonMapper.parseDate(soapDetail.getFchVto());
                } catch (Exception e) {
                    throw new ArcaSoapException("Failed to parse CAE expiration date: " + soapDetail.getFchVto(), e);
                }
            }

            List<CaeVatLine> vatLines = new ArrayList<>();
            if (soapDetail.getIva() != null && soapDetail.getIva().getAlicIva() != null) {
                for (AlicIva alic : soapDetail.getIva().getAlicIva()) {
                    vatLines.add(new CaeVatLine(
                            VatType.fromCode(alic.getId()),
                            alic.getBaseImp(),
                            alic.getImporte()
                    ));
                }
            }

            detail = new VoucherDetail(
                    VoucherType.fromCode(soapDetail.getCbteTipo()),
                    soapDetail.getPtoVta(),
                    soapDetail.getCbteDesde(),
                    ConceptType.fromCode(soapDetail.getConcepto()),
                    new Cuit(soapDetail.getDocNro()),
                    soapDetail.getImpNeto(),
                    soapDetail.getImpTotConc(),
                    soapDetail.getImpOpEx(),
                    soapDetail.getImpIVA(),
                    soapDetail.getImpTotal(),
                    date,
                    soapDetail.getCodAutorizacion(),
                    expDate,
                    soapDetail.getResultado(),
                    vatLines
            );
        }

        return new VoucherConsultResponse(Optional.ofNullable(detail), errors);
    }
}
