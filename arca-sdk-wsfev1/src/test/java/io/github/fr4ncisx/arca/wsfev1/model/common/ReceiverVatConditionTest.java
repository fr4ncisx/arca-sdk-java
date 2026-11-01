package io.github.fr4ncisx.arca.wsfev1.model.common;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ReceiverVatCondition}.
 *
 * @author fr4ncisx
 * @since 1.3.0
 */
class ReceiverVatConditionTest {

    @Test
    void mapsAllOfficialArcaCodesCorrectly() {
        assertThat(ReceiverVatCondition.fromCode(1)).isEqualTo(ReceiverVatCondition.IVA_RESPONSABLE_INSCRIPTO);
        assertThat(ReceiverVatCondition.fromCode(4)).isEqualTo(ReceiverVatCondition.IVA_SUJETO_EXENTO);
        assertThat(ReceiverVatCondition.fromCode(5)).isEqualTo(ReceiverVatCondition.CONSUMIDOR_FINAL);
        assertThat(ReceiverVatCondition.fromCode(6)).isEqualTo(ReceiverVatCondition.RESPONSABLE_MONOTRIBUTO);
        assertThat(ReceiverVatCondition.fromCode(7)).isEqualTo(ReceiverVatCondition.SUJETO_NO_CATEGORIZADO);
        assertThat(ReceiverVatCondition.fromCode(8)).isEqualTo(ReceiverVatCondition.PROVEEDOR_DEL_EXTERIOR);
        assertThat(ReceiverVatCondition.fromCode(9)).isEqualTo(ReceiverVatCondition.CLIENTE_DEL_EXTERIOR);
        assertThat(ReceiverVatCondition.fromCode(10)).isEqualTo(ReceiverVatCondition.IVA_LIBERADO_LEY_19640);
        assertThat(ReceiverVatCondition.fromCode(13)).isEqualTo(ReceiverVatCondition.MONOTRIBUTISTA_SOCIAL);
        assertThat(ReceiverVatCondition.fromCode(15)).isEqualTo(ReceiverVatCondition.IVA_NO_ALCANZADO);
        assertThat(ReceiverVatCondition.fromCode(16)).isEqualTo(ReceiverVatCondition.MONOTRIBUTO_TRABAJADOR_INDEPENDIENTE_PROMOVIDO);
    }

    @Test
    void exposesCorrectCodeAndDescription() {
        ReceiverVatCondition condition = ReceiverVatCondition.IVA_RESPONSABLE_INSCRIPTO;
        assertThat(condition.code()).isEqualTo(1);
        assertThat(condition.description()).isEqualTo("IVA Responsable Inscripto");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 2, 3, 11, 12, 14, 999})
    void rejectsInvalidOrUnknownCodes(int invalidCode) {
        assertThatThrownBy(() -> ReceiverVatCondition.fromCode(invalidCode))
                .isInstanceOf(ArcaValidationException.class)
                .hasMessageContaining("Unknown or unsupported receiver VAT condition code: " + invalidCode);
    }
}
