package io.github.fr4ncisx.arca.wsfev1.model.common;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

/**
 * Official ARCA VAT (IVA) rate types.
 * <p>
 * This catalog maps standard tax rates used in Argentina to their official numeric codes
 * required by ARCA.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public enum VatType {

    VAT_0(3, 0.0, "0%"),
    VAT_10_5(4, 10.5, "10.5%"),
    VAT_21(5, 21.0, "21%"),
    VAT_27(6, 27.0, "27%"),
    VAT_5(8, 5.0, "5%"),
    VAT_2_5(9, 2.5, "2.5%");

    private final int code;
    private final double rate;
    private final String description;

    VatType(int code, double rate, String description) {
        this.code = code;
        this.rate = rate;
        this.description = description;
    }

    /**
     * Returns the official ARCA numeric code for this VAT type.
     *
     * @return the numeric code
     */
    public int code() {
        return code;
    }

    /**
     * Returns the percentage tax rate.
     *
     * @return the rate as a percentage (e.g., 21.0)
     */
    public double rate() {
        return rate;
    }

    /**
     * Returns the description of this VAT type.
     *
     * @return the description string
     */
    public String description() {
        return description;
    }

    /**
     * Resolves the {@link VatType} corresponding to the official ARCA code.
     *
     * @param code the numeric code to look up
     * @return the matching VatType
     * @throws ArcaValidationException if the code is not recognized
     */
    public static VatType fromCode(int code) {
        for (VatType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new ArcaValidationException("Unknown ARCA VAT type code: " + code);
    }
}
