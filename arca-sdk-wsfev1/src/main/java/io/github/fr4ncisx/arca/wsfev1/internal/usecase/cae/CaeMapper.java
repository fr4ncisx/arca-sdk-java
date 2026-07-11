package io.github.fr4ncisx.arca.wsfev1.internal.usecase.cae;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.common.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeRequest;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeResponse;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeVatLine;
import io.github.fr4ncisx.arca.wsfev1.model.common.AfipError;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulated package-private translator for CAE authorization requests and responses.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
final class CaeMapper {

    private CaeMapper() {
    }

    static FECAESolicitar toSoapRequest(FEAuthRequest auth, CaeRequest request) {
        if (auth == null) {
            throw new ArcaValidationException("auth must not be null");
        }
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }

        FECAECabRequest cab = new FECAECabRequest();
        cab.setCantReg(1);
        cab.setPtoVta(request.salesPoint());
        cab.setCbteTipo(request.voucherType().code());

        FECAEDetRequest det = new FECAEDetRequest();
        det.setConcepto(request.concept().code());
        det.setDocTipo(80);
        det.setDocNro(request.customerCuit().number());
        det.setCbteDesde(request.number());
        det.setCbteHasta(request.number());
        det.setCbteFch(CommonMapper.formatDate(request.date()));
        det.setImpTotal(request.total());
        det.setImpTotConc(0.0);
        det.setImpNeto(request.netTaxed());
        det.setImpOpEx(request.exempted());
        det.setImpTrib(0.0);
        det.setImpIVA(request.vatTotal());
        det.setMonId("PES");
        det.setMonCotiz(1.0);

        if (!request.vatLines().isEmpty()) {
            ArrayOfAlicIva arrayIva = new ArrayOfAlicIva();
            for (CaeVatLine line : request.vatLines()) {
                AlicIva alic = new AlicIva();
                alic.setId(line.vatType().code());
                alic.setBaseImp(line.taxBase());
                alic.setImporte(line.vatAmount());
                arrayIva.getAlicIva().add(alic);
            }
            det.setIva(arrayIva);
        }

        ArrayOfFECAEDetRequest arrayDet = new ArrayOfFECAEDetRequest();
        arrayDet.getFECAEDetRequest().add(det);

        FECAERequest caeReq = new FECAERequest();
        caeReq.setFeCabReq(cab);
        caeReq.setFeDetReq(arrayDet);

        FECAESolicitar solicitar = new FECAESolicitar();
        solicitar.setAuth(auth);
        solicitar.setFeCAEReq(caeReq);
        return solicitar;
    }

    static CaeResponse toDomainResponse(FECAEResponse result) {
        if (result == null) {
            throw new ArcaSoapException("Received empty result from ARCA SOAP service");
        }

        List<AfipError> errors = new ArrayList<>();
        if (result.getErrors() != null && result.getErrors().getErr() != null) {
            for (Err err : result.getErrors().getErr()) {
                errors.add(new AfipError(err.getCode(), err.getMsg()));
            }
        }

        boolean approved = false;
        String cae = null;
        LocalDate caeExpiration = null;

        if (result.getFeCabResp() != null) {
            approved = "A".equals(result.getFeCabResp().getResultado());
        }

        if (result.getFeDetResp() != null && result.getFeDetResp().getFECAEDetResponse() != null
                && !result.getFeDetResp().getFECAEDetResponse().isEmpty()) {
            FECAEDetResponse det = result.getFeDetResp().getFECAEDetResponse().get(0);

            if (det.getCAE() != null && !det.getCAE().trim().isEmpty()) {
                cae = det.getCAE();
                if (det.getCAEFchVto() != null && !det.getCAEFchVto().trim().isEmpty()) {
                    try {
                        caeExpiration = CommonMapper.parseDate(det.getCAEFchVto());
                    } catch (Exception e) {
                        throw new ArcaSoapException("Failed to parse CAE expiration date: " + det.getCAEFchVto(), e);
                    }
                }
            }

            if (det.getObservaciones() != null && det.getObservaciones().getObs() != null) {
                for (Obs obs : det.getObservaciones().getObs()) {
                    errors.add(new AfipError(obs.getCode(), obs.getMsg()));
                }
            }
        }

        if (!approved && errors.isEmpty()) {
            errors.add(new AfipError(-1, "Request rejected by ARCA without error details"));
        }

        return new CaeResponse(approved, cae, caeExpiration, errors);
    }
}
