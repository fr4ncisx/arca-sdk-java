package io.github.fr4ncisx.arca.wsfev1.internal.usecase.salespoint;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.model.salespoint.SalesPoint;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SalesPointMapper}.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
class SalesPointMapperTest {

    @Test
    void toDomainResponseMapsSalesPointsCorrectly() {
        FEPtoVentaResponse result = new FEPtoVentaResponse();
        
        PtoVenta p1 = new PtoVenta();
        p1.setNro(1);
        p1.setEmisionTipo("RECE");
        p1.setBloqueado("N");
        p1.setFchBaja("null");
        
        PtoVenta p2 = new PtoVenta();
        p2.setNro(2);
        p2.setEmisionTipo("RECE");
        p2.setBloqueado("S");
        p2.setFchBaja("20260707");

        ArrayOfPtoVenta array = new ArrayOfPtoVenta();
        array.getPtoVenta().add(p1);
        array.getPtoVenta().add(p2);

        result.setResultGet(array);

        List<io.github.fr4ncisx.arca.wsfev1.model.salespoint.SalesPoint> response = SalesPointMapper.toDomainResponse(result);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).number()).isEqualTo(1);
        assertThat(response.get(0).emissionType()).isEqualTo("RECE");
        assertThat(response.get(0).blocked()).isFalse();
        assertThat(response.get(0).dropDate()).isEmpty();

        assertThat(response.get(1).number()).isEqualTo(2);
        assertThat(response.get(1).emissionType()).isEqualTo("RECE");
        assertThat(response.get(1).blocked()).isTrue();
        assertThat(response.get(1).dropDate()).hasValue(LocalDate.of(2026, 7, 7));
    }

    @Test
    void toDomainResponseThrowsExceptionOnErrors() {
        FEPtoVentaResponse result = new FEPtoVentaResponse();
        ArrayOfErr errorsArray = new ArrayOfErr();
        Err err = new Err();
        err.setCode(500);
        err.setMsg("Internal Server Error");
        errorsArray.getErr().add(err);
        result.setErrors(errorsArray);

        assertThatThrownBy(() -> SalesPointMapper.toDomainResponse(result))
                .isInstanceOf(ArcaSoapException.class)
                .hasMessageContaining("ARCA SOAP Error [500]");
    }
}
