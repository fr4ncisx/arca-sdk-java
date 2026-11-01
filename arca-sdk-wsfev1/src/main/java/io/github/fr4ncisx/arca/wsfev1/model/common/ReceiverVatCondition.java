package io.github.fr4ncisx.arca.wsfev1.model.common;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

import java.util.HashMap;
import java.util.Map;

/**
 * Official ARCA receiver VAT (IVA) conditions enforced under General Resolution 5616 (RG 5616).
 * <p>
 * Maps standard taxpayer receiver categories in Argentina to their official numeric identifiers
 * required by WSFEv1 electronic billing operations.
 *
 * @author fr4ncisx
 * @since 1.3.0
 */
public enum ReceiverVatCondition {

    IVA_RESPONSABLE_INSCRIPTO(1, "IVA Responsable Inscripto"),
    IVA_SUJETO_EXENTO(4, "IVA Sujeto Exento"),
    CONSUMIDOR_FINAL(5, "Consumidor Final"),
    RESPONSABLE_MONOTRIBUTO(6, "Responsable Monotributo"),
    SUJETO_NO_CATEGORIZADO(7, "Sujeto No Categorizado"),
    PROVEEDOR_DEL_EXTERIOR(8, "Proveedor del Exterior"),
    CLIENTE_DEL_EXTERIOR(9, "Cliente del Exterior"),
    IVA_LIBERADO_LEY_19640(10, "IVA Liberado - Ley Nº 19.640"),
    MONOTRIBUTISTA_SOCIAL(13, "Monotributista Social"),
    IVA_NO_ALCANZADO(15, "IVA No Alcanzado"),
    MONOTRIBUTO_TRABAJADOR_INDEPENDIENTE_PROMOVIDO(16, "Monotributo Trabajador Independiente Promovido");

    private static final Map<Integer, ReceiverVatCondition> CODE_MAP = new HashMap<>();

    static {
        for (ReceiverVatCondition condition : values()) {
            CODE_MAP.put(condition.code, condition);
        }
    }

    private final int code;
    private final String description;

    ReceiverVatCondition(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Returns the official ARCA numeric code for this receiver VAT condition.
     *
     * @return the numeric code
     */
    public int code() {
        return code;
    }

    /**
     * Returns the description of this receiver VAT condition.
     *
     * @return the description string
     */
    public String description() {
        return description;
    }

    /**
     * Resolves a {@link ReceiverVatCondition} instance corresponding to the given numeric code.
     *
     * @param code the numeric code from ARCA
     * @return the matching ReceiverVatCondition
     * @throws ArcaValidationException if the code does not map to any known condition
     */
    public static ReceiverVatCondition fromCode(int code) {
        ReceiverVatCondition condition = CODE_MAP.get(code);
        if (condition == null) {
            throw new ArcaValidationException("Unknown or unsupported receiver VAT condition code: " + code);
        }
        return condition;
    }
}
