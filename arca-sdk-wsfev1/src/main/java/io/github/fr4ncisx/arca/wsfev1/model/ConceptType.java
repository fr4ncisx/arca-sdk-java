package io.github.fr4ncisx.arca.wsfev1.model;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

/**
 * Official ARCA concept types for electronic invoicing.
 * <p>
 * This catalog defines the category of products or services sold in
 * a voucher, mapped to their official codes.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public enum ConceptType {

    PRODUCTS(1, "Productos"),
    SERVICES(2, "Servicios"),
    MIXED(3, "Productos y Servicios");

    private final int code;
    private final String description;

    ConceptType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Returns the official ARCA numeric code for this concept type.
     *
     * @return the numeric code
     */
    public int code() {
        return code;
    }

    /**
     * Returns the descriptive name of this concept type.
     *
     * @return the description string
     */
    public String description() {
        return description;
    }

    /**
     * Resolves the {@link ConceptType} corresponding to the official ARCA code.
     *
     * @param code the numeric code to look up
     * @return the matching ConceptType
     * @throws ArcaValidationException if the code is not recognized
     */
    public static ConceptType fromCode(int code) {
        for (ConceptType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new ArcaValidationException("Unknown ARCA concept type code: " + code);
    }
}
