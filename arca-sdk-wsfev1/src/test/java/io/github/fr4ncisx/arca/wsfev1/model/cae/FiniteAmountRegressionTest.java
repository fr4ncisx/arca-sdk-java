package io.github.fr4ncisx.arca.wsfev1.model.cae;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsfev1.model.caea.CaeaReportDetail;
import io.github.fr4ncisx.arca.wsfev1.model.catalog.ExchangeRate;
import io.github.fr4ncisx.arca.wsfev1.model.common.ConceptType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks finite CAEA amounts and positive finite exchange rates.
 *
 * @author fr4ncisx
 * @since 1.2.4
 */
class FiniteAmountRegressionTest {

    /**
     * Rejects each non-finite value independently in every CAEA amount.
     */
    @Test
    void rejectsNonFiniteCaeaAmounts() {
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            for (int field = 0; field < 4; field++) {
                double[] amounts = {121.0, 100.0, 0.0, 21.0};
                amounts[field] = invalid;
                assertThatThrownBy(() -> detail(amounts))
                        .isInstanceOf(ArcaValidationException.class);
            }
        }
    }

    /**
     * Preserves valid zero and decimal amounts and rejects a negative total.
     */
    @Test
    void preservesValidCaeaAmounts() {
        assertThat(detail(new double[]{0, 0, 0, 0}).total()).isZero();
        assertThat(detail(new double[]{121.12, 100.1, 0, 21.02}).total()).isEqualTo(121.12);
        assertThatThrownBy(() -> detail(new double[]{-1, 0, 0, 0}))
                .isInstanceOf(ArcaValidationException.class);
    }

    /**
     * Rejects non-finite and non-positive rates while retaining valid rates.
     */
    @Test
    void rejectsInvalidExchangeRates() {
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, 0, -1}) {
            assertThatThrownBy(() -> new ExchangeRate("DOL", invalid, null))
                    .isInstanceOf(ArcaValidationException.class);
        }
        assertThat(new ExchangeRate("DOL", 1234.56, null).rate()).isEqualTo(1234.56);
    }

    private static CaeaReportDetail detail(double[] amounts) {
        return new CaeaReportDetail(ConceptType.PRODUCTS, Cuit.parse("20-33333333-4"),
                1, LocalDate.of(2026, 10, 22), amounts[0], amounts[1], amounts[2], amounts[3], List.of());
    }
}
