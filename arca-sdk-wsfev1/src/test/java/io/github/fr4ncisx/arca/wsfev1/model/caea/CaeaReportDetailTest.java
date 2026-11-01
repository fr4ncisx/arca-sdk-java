package io.github.fr4ncisx.arca.wsfev1.model.caea;

import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeVatLine;
import io.github.fr4ncisx.arca.wsfev1.model.common.ConceptType;
import io.github.fr4ncisx.arca.wsfev1.model.common.ReceiverVatCondition;
import io.github.fr4ncisx.arca.wsfev1.model.common.VatType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CaeaReportDetail}.
 *
 * @author fr4ncisx
 * @since 1.3.0
 */
class CaeaReportDetailTest {

    @Test
    void supportsExplicitReceiverVatCondition() {
        CaeaReportDetail detail = new CaeaReportDetail(
                ConceptType.PRODUCTS,
                Cuit.parse("20-33333333-4"),
                50L,
                LocalDate.of(2026, 11, 1),
                121.0,
                100.0,
                0.0,
                21.0,
                List.of(new CaeVatLine(VatType.VAT_21, 100.0, 21.0)),
                ReceiverVatCondition.IVA_RESPONSABLE_INSCRIPTO
        );

        assertThat(detail.receiverVatCondition()).isEqualTo(ReceiverVatCondition.IVA_RESPONSABLE_INSCRIPTO);
    }

    @Test
    void defaultsReceiverVatConditionToNullWhenUsingLegacyConstructor() {
        CaeaReportDetail detail = new CaeaReportDetail(
                ConceptType.PRODUCTS,
                Cuit.parse("20-33333333-4"),
                50L,
                LocalDate.of(2026, 11, 1),
                121.0,
                100.0,
                0.0,
                21.0,
                List.of(new CaeVatLine(VatType.VAT_21, 100.0, 21.0))
        );

        assertThat(detail.receiverVatCondition()).isNull();
    }
}
