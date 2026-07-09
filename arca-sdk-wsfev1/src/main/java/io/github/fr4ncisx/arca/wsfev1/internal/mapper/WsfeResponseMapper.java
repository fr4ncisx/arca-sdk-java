package io.github.fr4ncisx.arca.wsfev1.internal.mapper;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FERecuperaLastCbteResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECAEResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECompConsultaResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEPtoVentaResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.Err;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECAEDetResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.Obs;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECompConsResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.AlicIva;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.PtoVenta;
import io.github.fr4ncisx.arca.wsfev1.model.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Translator for maps JAXB SOAP structures into public domain response records.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public final class WsfeResponseMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private WsfeResponseMapper() {
    }

    /**
     * Translates a FERecuperaLastCbteResponse from ARCA to LastVoucherResponse.
     *
     * @param result  the SOAP response result
     * @param request the original public request
     * @return the domain response record
     * @throws ArcaSoapException if the result is nillable or contains errors
     */
    public static LastVoucherResponse toDomainResponse(
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

    /**
     * Translates a FECAEResponse from ARCA to CaeResponse.
     *
     * @param result the SOAP response result
     * @return the domain response record
     * @throws ArcaSoapException if the result is nillable or fails to parse
     */
    public static CaeResponse toDomainResponse(FECAEResponse result) {
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
                        caeExpiration = LocalDate.parse(det.getCAEFchVto().trim(), DATE_FORMATTER);
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

    /**
     * Translates a FECompConsultaResponse from ARCA to VoucherConsultResponse.
     *
     * @param result the SOAP response result
     * @return the domain response record
     * @throws ArcaSoapException if the result is nillable
     */
    public static VoucherConsultResponse toDomainResponse(FECompConsultaResponse result) {
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
                    date = LocalDate.parse(soapDetail.getCbteFch().trim(), DATE_FORMATTER);
                } catch (Exception e) {
                    throw new ArcaSoapException("Failed to parse voucher date: " + soapDetail.getCbteFch(), e);
                }
            }

            LocalDate expDate = null;
            if (soapDetail.getFchVto() != null && !soapDetail.getFchVto().trim().isEmpty()) {
                try {
                    expDate = LocalDate.parse(soapDetail.getFchVto().trim(), DATE_FORMATTER);
                } catch (Exception e) {
                    throw new ArcaSoapException("Failed to parse CAE expiration date: " + soapDetail.getFchVto(), e);
                }
            }

            List<io.github.fr4ncisx.arca.wsfev1.model.CaeVatLine> vatLines = new ArrayList<>();
            if (soapDetail.getIva() != null && soapDetail.getIva().getAlicIva() != null) {
                for (AlicIva alic : soapDetail.getIva().getAlicIva()) {
                    vatLines.add(new io.github.fr4ncisx.arca.wsfev1.model.CaeVatLine(
                            io.github.fr4ncisx.arca.wsfev1.model.VatType.fromCode(alic.getId()),
                            alic.getBaseImp(),
                            alic.getImporte()
                    ));
                }
            }

            detail = new VoucherDetail(
                    io.github.fr4ncisx.arca.wsfev1.model.VoucherType.fromCode(soapDetail.getCbteTipo()),
                    soapDetail.getPtoVta(),
                    soapDetail.getCbteDesde(),
                    io.github.fr4ncisx.arca.wsfev1.model.ConceptType.fromCode(soapDetail.getConcepto()),
                    new io.github.fr4ncisx.arca.core.tax.Cuit(soapDetail.getDocNro()),
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

        return new VoucherConsultResponse(java.util.Optional.ofNullable(detail), errors);
    }

    /**
     * Translates a FEPtoVentaResponse from ARCA to a list of SalesPoint domain records.
     *
     * @param result the SOAP response result
     * @return the list of sales points
     * @throws ArcaSoapException if the result is nillable or contains errors
     */
    public static List<SalesPoint> toDomainResponse(FEPtoVentaResponse result) {
        if (result == null) {
            throw new ArcaSoapException("Received empty result from ARCA SOAP service");
        }

        if (result.getErrors() != null && result.getErrors().getErr() != null
                && !result.getErrors().getErr().isEmpty()) {
            Err error = result.getErrors().getErr().get(0);
            throw new ArcaSoapException("ARCA SOAP Error [" + error.getCode() + "]: " + error.getMsg());
        }

        List<SalesPoint> list = new ArrayList<>();
        if (result.getResultGet() != null && result.getResultGet().getPtoVenta() != null) {
            for (PtoVenta p : result.getResultGet().getPtoVenta()) {
                java.util.Optional<LocalDate> dropDate = java.util.Optional.empty();
                if (p.getFchBaja() != null && !p.getFchBaja().trim().isEmpty() && !"null".equalsIgnoreCase(p.getFchBaja().trim())) {
                    try {
                        dropDate = java.util.Optional.of(LocalDate.parse(p.getFchBaja().trim(), DATE_FORMATTER));
                    } catch (Exception e) {
                        throw new ArcaSoapException("Failed to parse sales point drop date: " + p.getFchBaja(), e);
                    }
                }
                boolean blocked = "S".equalsIgnoreCase(p.getBloqueado());
                list.add(new SalesPoint(p.getNro(), p.getEmisionTipo(), blocked, dropDate));
            }
        }
        return java.util.Collections.unmodifiableList(list);
    }
}
