package io.github.fr4ncisx.arca.wsmtxca.model;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates collection snapshots in WSMTXCA request and response models.
 *
 * @author fr4ncisx
 * @since 1.2.4
 */
@SuppressWarnings("null")
class WsmtxcaCollectionSnapshotTest {

    /**
     * Verifies that voucher request collections are copied and made immutable.
     */
    @Test
    void snapshotsAllVoucherRequestCollections() {
        ItemDetail item = new ItemDetail(null, null, null, "item", null, (short) 1,
                null, null, (short) 1, null, BigDecimal.ONE);
        TaxDetail vat = new TaxDetail((short) 5, BigDecimal.ONE, BigDecimal.ZERO);
        OtherTaxDetail tax = new OtherTaxDetail((short) 1, "tax", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        AssociatedVoucher associatedVoucher = new AssociatedVoucher((short) 1, 1, 1, null);
        List<ItemDetail> items = new ArrayList<>(List.of(item));
        List<TaxDetail> vats = new ArrayList<>(List.of(vat));
        List<OtherTaxDetail> taxes = new ArrayList<>(List.of(tax));
        List<AssociatedVoucher> associated = new ArrayList<>(List.of(associatedVoucher));

        WsmtxcaVoucherRequest request = new WsmtxcaVoucherRequest(
                (short) 1, 1, 1, null, null, null, null, null, null, null,
                BigDecimal.ZERO, null, BigDecimal.ONE, "PES", null, null, null, (short) 1,
                null, null, null, items, vats, taxes, associated);

        items.clear();
        vats.clear();
        taxes.clear();
        associated.clear();

        assertThat(request.items()).containsExactly(item);
        assertThat(request.vatSubtotals()).containsExactly(vat);
        assertThat(request.otherTaxes()).containsExactly(tax);
        assertThat(request.associatedVouchers()).containsExactly(associatedVoucher);
        assertThatThrownBy(() -> request.items().add(item)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> request.vatSubtotals().add(vat)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> request.otherTaxes().add(tax)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> request.associatedVouchers().add(associatedVoucher))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * Verifies that voucher response collections are copied and made immutable.
     */
    @Test
    void snapshotsVoucherResponseCollections() {
        WsmtxcaError error = new WsmtxcaError(1, "error");
        WsmtxcaError observation = new WsmtxcaError(2, "observation");
        List<WsmtxcaError> errors = new ArrayList<>(List.of(error));
        List<WsmtxcaError> observations = new ArrayList<>(List.of(observation));

        WsmtxcaVoucherResponse voucherResponse = new WsmtxcaVoucherResponse(
                (short) 1, 1, 1, null, null, null, null, "A", errors, observations);
        WsmtxcaVoucherConsultResponse consultResponse = new WsmtxcaVoucherConsultResponse(
                null, null, null, errors);
        WsmtxcaLastVoucherResponse lastVoucherResponse = new WsmtxcaLastVoucherResponse(
                1, (short) 1, 1, errors);

        errors.clear();
        observations.clear();

        assertThat(voucherResponse.errors()).containsExactly(error);
        assertThat(voucherResponse.observations()).containsExactly(observation);
        assertThat(consultResponse.errors()).containsExactly(error);
        assertThat(lastVoucherResponse.errors()).containsExactly(error);
        assertThatThrownBy(() -> voucherResponse.errors().add(error))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * Rejects null entries in voucher request collections at construction time.
     */
    @Test
    void rejectsNullRequestCollectionEntries() {
        ItemDetail item = new ItemDetail(null, null, null, "item", null, (short) 1,
                null, null, (short) 1, null, BigDecimal.ONE);
        List<ItemDetail> items = Arrays.asList(item, null);

        assertThatThrownBy(() -> new WsmtxcaVoucherRequest(
                (short) 1, 1, 1, null, null, null, null, null, null, null,
                BigDecimal.ZERO, null, BigDecimal.ONE, "PES", null, null, null, (short) 1,
                null, null, null, items, List.of(), List.of(), List.of()))
                .isInstanceOf(ArcaValidationException.class);
    }
}
