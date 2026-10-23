package io.github.fr4ncisx.arca.wsfev1.model.voucher;

import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeVatLine;
import io.github.fr4ncisx.arca.wsfev1.model.common.AfipError;
import io.github.fr4ncisx.arca.wsfev1.model.common.ConceptType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VatType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates immutable collection snapshots in voucher query models.
 *
 * @author fr4ncisx
 * @since 1.2.4
 */
class VoucherCollectionSnapshotTest {

    /**
     * Verifies that voucher details and query responses snapshot their input lists.
     */
    @Test
    void snapshotsVoucherDetailAndResponseCollections() {
        CaeVatLine vatLine = new CaeVatLine(VatType.fromCode(5), 100.0, 21.0);
        AfipError error = new AfipError(1, "error");
        List<CaeVatLine> vatLines = new ArrayList<>(List.of(vatLine));
        List<AfipError> errors = new ArrayList<>(List.of(error));
        VoucherDetail detail = new VoucherDetail(
                VoucherType.INVOICE_A,
                1,
                1,
                ConceptType.PRODUCTS,
                Cuit.parse("20-33333333-4"),
                100.0,
                0.0,
                0.0,
                21.0,
                121.0,
                LocalDate.of(2026, 10, 22),
                "cae",
                LocalDate.of(2026, 11, 22),
                "A",
                vatLines);
        VoucherConsultResponse response = new VoucherConsultResponse(detail, errors);

        vatLines.clear();
        errors.clear();

        assertThat(detail.vatLines()).containsExactly(vatLine);
        assertThat(response.errors()).containsExactly(error);
        assertThatThrownBy(() -> detail.vatLines().add(vatLine))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> response.errors().add(error))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * Verifies that invalid monetary values returned in voucher details are rejected.
     */
    @Test
    void rejectsNonFiniteVoucherAmounts() {
        assertThatThrownBy(() -> new VoucherDetail(
                VoucherType.INVOICE_A,
                1,
                1,
                ConceptType.PRODUCTS,
                Cuit.parse("20-33333333-4"),
                Double.NaN,
                0.0,
                0.0,
                0.0,
                Double.NaN,
                LocalDate.of(2026, 10, 22),
                "cae",
                LocalDate.of(2026, 11, 22),
                "A",
                List.of()))
                .isInstanceOf(ArcaValidationException.class);
    }
}
