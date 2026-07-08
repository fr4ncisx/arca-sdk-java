package io.github.fr4ncisx.arca.wsfev1.internal.mapper;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.model.CaeResponse;
import io.github.fr4ncisx.arca.wsfev1.model.AfipError;
import io.github.fr4ncisx.arca.wsfev1.model.LastVoucherRequest;
import io.github.fr4ncisx.arca.wsfev1.model.LastVoucherResponse;

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
}
