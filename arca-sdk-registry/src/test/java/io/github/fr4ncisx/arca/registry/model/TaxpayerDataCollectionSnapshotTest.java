package io.github.fr4ncisx.arca.registry.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates immutable collection snapshots in taxpayer registry data.
 *
 * @author fr4ncisx
 * @since 1.2.4
 */
@SuppressWarnings("null")
class TaxpayerDataCollectionSnapshotTest {

    /**
     * Verifies that all registry collection fields are copied and made immutable.
     */
    @Test
    void snapshotsAllTaxpayerCollections() {
        TaxpayerAddress address = new TaxpayerAddress(null, null, null, null, null, null, null, null, null);
        TaxpayerTax tax = new TaxpayerTax(null, null, null, null, null);
        TaxpayerActivity activity = new TaxpayerActivity(null, null, null, null, null);
        TaxpayerRegime regime = new TaxpayerRegime(null, null, null, null, null, null);
        List<TaxpayerAddress> addresses = new ArrayList<>(List.of(address));
        List<TaxpayerTax> taxes = new ArrayList<>(List.of(tax));
        List<TaxpayerActivity> activities = new ArrayList<>(List.of(activity));
        List<TaxpayerRegime> regimes = new ArrayList<>(List.of(regime));
        TaxpayerData taxpayer = new TaxpayerData(
                20333333334L,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                addresses, taxes, activities, regimes);

        addresses.clear();
        taxes.clear();
        activities.clear();
        regimes.clear();

        assertThat(taxpayer.addresses()).containsExactly(address);
        assertThat(taxpayer.taxes()).containsExactly(tax);
        assertThat(taxpayer.activities()).containsExactly(activity);
        assertThat(taxpayer.regimes()).containsExactly(regime);
        assertThatThrownBy(() -> taxpayer.addresses().add(address))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
