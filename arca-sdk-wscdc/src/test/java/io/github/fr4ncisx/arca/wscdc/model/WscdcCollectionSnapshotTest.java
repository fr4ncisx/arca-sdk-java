package io.github.fr4ncisx.arca.wscdc.model;

import io.github.fr4ncisx.arca.core.tax.Cuit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates collection snapshots in WSCDC request and response models.
 *
 * @author fr4ncisx
 * @since 1.2.4
 */
class WscdcCollectionSnapshotTest {

    /**
     * Verifies that request optional fields are copied and made immutable.
     */
    @Test
    void snapshotsRequestOptionalFields() {
        WscdcOptionalField field = new WscdcOptionalField("1", "value");
        List<WscdcOptionalField> fields = new ArrayList<>(List.of(field));
        WscdcConstatRequest request = new WscdcConstatRequest(
                "CAE", Cuit.parse("20-33333333-4"), 1, 1, 1,
                LocalDate.of(2026, 10, 22), BigDecimal.ONE, "authorization", null, null, fields);

        fields.clear();

        assertThat(request.optionalFields()).containsExactly(field);
        assertThatThrownBy(() -> request.optionalFields().add(field))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * Verifies that response observations and errors are copied and made immutable.
     */
    @Test
    void snapshotsResponseCollections() {
        WscdcObservation observation = new WscdcObservation(1, "observation");
        WscdcError error = new WscdcError(2, "error");
        List<WscdcObservation> observations = new ArrayList<>(List.of(observation));
        List<WscdcError> errors = new ArrayList<>(List.of(error));
        WscdcConstatResponse response = new WscdcConstatResponse(
                "A", LocalDate.of(2026, 10, 22), observations, errors);

        observations.clear();
        errors.clear();

        assertThat(response.observations()).containsExactly(observation);
        assertThat(response.errors()).containsExactly(error);
        assertThatThrownBy(() -> response.errors().add(error))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
