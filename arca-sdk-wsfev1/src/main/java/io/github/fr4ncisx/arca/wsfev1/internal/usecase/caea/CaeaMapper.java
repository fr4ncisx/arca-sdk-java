package io.github.fr4ncisx.arca.wsfev1.internal.usecase.caea;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.common.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeVatLine;
import io.github.fr4ncisx.arca.wsfev1.model.caea.*;
import io.github.fr4ncisx.arca.wsfev1.model.common.AfipError;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Translator for CAEA request and response stubs.
 *
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
final class CaeaMapper {

    private CaeaMapper() {
    }

    static FECAEASolicitar toSoapRequest(FEAuthRequest auth, CaeaRequest request) {
        if (auth == null) {
            throw new ArcaValidationException("auth must not be null");
        }
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        FECAEASolicitar req = new FECAEASolicitar();
        req.setAuth(auth);
        req.setPeriodo(request.period());
        req.setOrden((short) request.order());
        return req;
    }

    static CaeaResponse toDomainResponse(FECAEAGetResponse response) {
        if (response == null) {
            throw new ArcaSoapException("Received empty response from ARCA SOAP service");
        }

        List<AfipError> errors = new ArrayList<>();
        if (response.getErrors() != null && response.getErrors().getErr() != null) {
            for (Err err : response.getErrors().getErr()) {
                errors.add(new AfipError(err.getCode(), err.getMsg()));
            }
        }

        String caea = null;
        int period = 0;
        int order = 0;
        LocalDate startDate = null;
        LocalDate endDate = null;
        LocalDate expirationDate = null;
        List<AfipError> observations = new ArrayList<>();

        FECAEAGet resultGet = response.getResultGet();
        if (resultGet != null) {
            caea = resultGet.getCAEA();
            period = resultGet.getPeriodo();
            order = resultGet.getOrden();
            startDate = CommonMapper.parseDate(resultGet.getFchVigDesde());
            endDate = CommonMapper.parseDate(resultGet.getFchVigHasta());
            expirationDate = CommonMapper.parseDate(resultGet.getFchTopeInf());
            if (resultGet.getObservaciones() != null && resultGet.getObservaciones().getObs() != null) {
                for (Obs obs : resultGet.getObservaciones().getObs()) {
                    observations.add(new AfipError(obs.getCode(), obs.getMsg()));
                }
            }
        }

        return new CaeaResponse(caea, period, order, startDate, endDate, expirationDate, errors, observations);
    }

    static FECAEAConsultar toSoapRequest(FEAuthRequest auth, CaeaQuery query) {
        if (auth == null) {
            throw new ArcaValidationException("auth must not be null");
        }
        if (query == null) {
            throw new ArcaValidationException("query must not be null");
        }
        FECAEAConsultar req = new FECAEAConsultar();
        req.setAuth(auth);
        req.setPeriodo(query.period());
        req.setOrden((short) query.order());
        return req;
    }

    static FECAEARegInformativo toSoapRequest(FEAuthRequest auth, CaeaReportRequest request) {
        if (auth == null) {
            throw new ArcaValidationException("auth must not be null");
        }
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }

        FECAEACabRequest cab = new FECAEACabRequest();
        cab.setCantReg(request.details().size());
        cab.setPtoVta(request.salesPoint());
        cab.setCbteTipo(request.voucherType().code());

        ArrayOfFECAEADetRequest arrayDet = new ArrayOfFECAEADetRequest();
        for (CaeaReportDetail detail : request.details()) {
            FECAEADetRequest det = new FECAEADetRequest();
            det.setCAEA(request.caea());
            det.setConcepto(detail.concept().code());
            det.setDocTipo(80); // CUIT is 80
            det.setDocNro(detail.customerCuit().number());
            det.setCbteDesde(detail.number());
            det.setCbteHasta(detail.number());
            det.setCbteFch(CommonMapper.formatDate(detail.date()));
            det.setImpTotal(detail.total());
            det.setImpTotConc(0.0);
            det.setImpNeto(detail.netTaxed());
            det.setImpOpEx(detail.exempted());
            det.setImpTrib(0.0);
            det.setImpIVA(detail.vatTotal());
            det.setMonId("PES");
            det.setMonCotiz(1.0);

            if (!detail.vatLines().isEmpty()) {
                ArrayOfAlicIva arrayIva = new ArrayOfAlicIva();
                for (CaeVatLine line : detail.vatLines()) {
                    AlicIva alic = new AlicIva();
                    alic.setId(line.vatType().code());
                    alic.setBaseImp(line.taxBase());
                    alic.setImporte(line.vatAmount());
                    arrayIva.getAlicIva().add(alic);
                }
                det.setIva(arrayIva);
            }
            arrayDet.getFECAEADetRequest().add(det);
        }

        FECAEARequest regReq = new FECAEARequest();
        regReq.setFeCabReq(cab);
        regReq.setFeDetReq(arrayDet);

        FECAEARegInformativo soapReq = new FECAEARegInformativo();
        soapReq.setAuth(auth);
        soapReq.setFeCAEARegInfReq(regReq);
        return soapReq;
    }

    static CaeaReportResponse toDomainResponse(FECAEAResponse result) {
        if (result == null) {
            throw new ArcaSoapException("Received empty response from ARCA SOAP service");
        }

        List<AfipError> globalErrors = new ArrayList<>();
        if (result.getErrors() != null && result.getErrors().getErr() != null) {
            for (Err err : result.getErrors().getErr()) {
                globalErrors.add(new AfipError(err.getCode(), err.getMsg()));
            }
        }

        String globalResult = null;
        if (result.getFeCabResp() != null) {
            globalResult = result.getFeCabResp().getResultado();
        }

        List<CaeaVoucherResult> detailResults = new ArrayList<>();
        if (result.getFeDetResp() != null && result.getFeDetResp().getFECAEADetResponse() != null) {
            for (FECAEADetResponse det : result.getFeDetResp().getFECAEADetResponse()) {
                long number = det.getCbteDesde();
                String voucherResult = det.getResultado();

                List<AfipError> voucherErrors = List.of();

                List<AfipError> voucherObs = new ArrayList<>();
                if (det.getObservaciones() != null && det.getObservaciones().getObs() != null) {
                    for (Obs obs : det.getObservaciones().getObs()) {
                        voucherObs.add(new AfipError(obs.getCode(), obs.getMsg()));
                    }
                }

                detailResults.add(new CaeaVoucherResult(number, voucherResult, voucherErrors, voucherObs));
            }
        }

        return new CaeaReportResponse(globalResult, detailResults, globalErrors);
    }

    static FECAEASinMovimientoInformar toSoapRequest(FEAuthRequest auth, CaeaNoMovementRequest request) {
        if (auth == null) {
            throw new ArcaValidationException("auth must not be null");
        }
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        FECAEASinMovimientoInformar req = new FECAEASinMovimientoInformar();
        req.setAuth(auth);
        req.setCAEA(request.caea());
        req.setPtoVta(request.salesPoint());
        return req;
    }

    static FECAEASinMovimientoConsultar toSoapRequest(FEAuthRequest auth, CaeaNoMovementQuery query) {
        if (auth == null) {
            throw new ArcaValidationException("auth must not be null");
        }
        if (query == null) {
            throw new ArcaValidationException("query must not be null");
        }
        FECAEASinMovimientoConsultar req = new FECAEASinMovimientoConsultar();
        req.setAuth(auth);
        req.setCAEA(query.caea());
        req.setPtoVta(query.salesPoint());
        return req;
    }
}
