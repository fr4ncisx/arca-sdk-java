package io.github.fr4ncisx.arca.wsfev1.internal.usecase.salespoint;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.Err;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEPtoVentaResponse;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.PtoVenta;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.common.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.salespoint.SalesPoint;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Encapsulated package-private translator for Sales Point requests and responses.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
final class SalesPointMapper {

    private SalesPointMapper() {
    }

    static List<SalesPoint> toDomainResponse(FEPtoVentaResponse result) {
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
                Optional<LocalDate> dropDate = Optional.empty();
                if (p.getFchBaja() != null && !p.getFchBaja().trim().isEmpty() && !"null".equalsIgnoreCase(p.getFchBaja().trim())) {
                    try {
                        dropDate = Optional.ofNullable(CommonMapper.parseDate(p.getFchBaja()));
                    } catch (Exception e) {
                        throw new ArcaSoapException("Failed to parse sales point drop date: " + p.getFchBaja(), e);
                    }
                }
                boolean blocked = "S".equalsIgnoreCase(p.getBloqueado());
                list.add(new SalesPoint(p.getNro(), p.getEmisionTipo(), blocked, dropDate));
            }
        }
        return Collections.unmodifiableList(list);
    }
}
