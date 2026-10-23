package io.github.fr4ncisx.arca.wsfev1.model.cae;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsfev1.model.common.ConceptType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VatType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates immutable CAE request inputs and finite monetary values.
 *
 * @author fr4ncisx
 * @since 1.2.4
 */
class CaeRequestTest {

    /**
     * Rejects non-finite amounts before they can enter a CAE payload.
     */
    @Test
    void rejectsNonFiniteMonetaryAmounts() {
        double[][] invalidAmounts = {
                {Double.NaN, 0, 0, 0, Double.NaN},
                {0, Double.NaN, 0, 0, Double.NaN},
                {0, 0, Double.NaN, 0, Double.NaN},
                {0, 0, 0, Double.NaN, Double.NaN},
                {0, 0, 0, 0, Double.NaN},
                {Double.POSITIVE_INFINITY, 0, 0, 0, Double.POSITIVE_INFINITY},
                {Double.NEGATIVE_INFINITY, 0, 0, 0, Double.NEGATIVE_INFINITY}
        };

        for (double[] amounts : invalidAmounts) {
            assertThatThrownBy(() -> createRequest(amounts[0], amounts[1], amounts[2], amounts[3], amounts[4], List.of()))
                    .isInstanceOf(ArcaValidationException.class);
        }
    }

    /**
     * Keeps an immutable snapshot of VAT lines and rejects null entries.
     */
    @Test
    void snapshotsVatLinesAndRejectsNullEntries() {
        CaeVatLine vatLine = new CaeVatLine(VatType.fromCode(5), 100.0, 21.0);
        List<CaeVatLine> mutableLines = new ArrayList<>(List.of(vatLine));
        CaeRequest request = createRequest(100.0, 0.0, 0.0, 21.0, 121.0, mutableLines);

        mutableLines.clear();

        assertThat(request.vatLines()).containsExactly(vatLine);
        assertThatThrownBy(() -> request.vatLines().add(vatLine))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> createRequest(
                100.0,
                0.0,
                0.0,
                21.0,
                121.0,
                Arrays.asList(vatLine, null)))
                .isInstanceOf(ArcaValidationException.class);
    }

    /**
     * Rejects non-finite values in individual VAT lines.
     */
    @Test
    void rejectsNonFiniteVatLineAmounts() {
        assertThatThrownBy(() -> new CaeVatLine(VatType.fromCode(5), Double.NaN, 0.0))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> new CaeVatLine(VatType.fromCode(5), 0.0, Double.POSITIVE_INFINITY))
                .isInstanceOf(ArcaValidationException.class);
    }

    /**
     * Rejects overflow in the computed total while retaining finite zero and decimal amounts.
     */
    @Test
    void rejectsComputedOverflowAndPreservesFiniteAmounts() {
        assertThatThrownBy(() -> createRequest(
                Double.MAX_VALUE, Double.MAX_VALUE, 0, 0, Double.MAX_VALUE, List.of()))
                .isInstanceOf(ArcaValidationException.class);
        assertThat(createRequest(0, 0, 0, 0, 0, List.of()).total()).isZero();
        assertThat(createRequest(100.1, 0, 0, 21.02, 121.12, List.of()).total()).isEqualTo(121.12);
    }

    private static CaeRequest createRequest(
            double netTaxed,
            double netUntaxed,
            double exempted,
            double vatTotal,
            double total,
            List<CaeVatLine> vatLines) {
        return new CaeRequest(
                VoucherType.INVOICE_A,
                1,
                1,
                ConceptType.PRODUCTS,
                Cuit.parse("20-33333333-4"),
                netTaxed,
                netUntaxed,
                exempted,
                vatTotal,
                total,
                LocalDate.of(2026, 10, 22),
                vatLines);
    }
}
