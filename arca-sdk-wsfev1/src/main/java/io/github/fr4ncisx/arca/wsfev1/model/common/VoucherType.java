package io.github.fr4ncisx.arca.wsfev1.model.common;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

/**
 * Official ARCA voucher types and their corresponding codes.
 * <p>
 * This catalog defines standard taxpayer document types, such as invoices,
 * debit notes, and credit notes, mapped to their official codes.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public enum VoucherType {

    INVOICE_A(1, "Factura A"),
    DEBIT_NOTE_A(2, "Nota de Débito A"),
    CREDIT_NOTE_A(3, "Nota de Crédito A"),
    RECEIPT_A(4, "Recibo A"),
    INVOICE_B(6, "Factura B"),
    DEBIT_NOTE_B(7, "Nota de Débito B"),
    CREDIT_NOTE_B(8, "Nota de Crédito B"),
    RECEIPT_B(9, "Recibo B"),
    INVOICE_C(11, "Factura C"),
    DEBIT_NOTE_C(12, "Nota de Débito C"),
    CREDIT_NOTE_C(13, "Nota de Crédito C"),
    RECEIPT_C(15, "Recibo C"),
    INVOICE_M(51, "Factura M"),
    DEBIT_NOTE_M(52, "Nota de Débito M"),
    CREDIT_NOTE_M(53, "Nota de Crédito M"),
    RECEIPT_M(54, "Recibo M"),
    
    // MiPyME (FCE)
    INVOICE_A_MIPYME(201, "Factura de Crédito Electrónica MiPyME A"),
    DEBIT_NOTE_A_MIPYME(202, "Nota de Débito Electrónica MiPyME A"),
    CREDIT_NOTE_A_MIPYME(203, "Nota de Crédito Electrónica MiPyME A"),
    INVOICE_B_MIPYME(206, "Factura de Crédito Electrónica MiPyME B"),
    DEBIT_NOTE_B_MIPYME(207, "Nota de Débito Electrónica MiPyME B"),
    CREDIT_NOTE_B_MIPYME(208, "Nota de Crédito Electrónica MiPyME B"),
    INVOICE_C_MIPYME(211, "Factura de Crédito Electrónica MiPyME C"),
    DEBIT_NOTE_C_MIPYME(212, "Nota de Débito Electrónica MiPyME C"),
    CREDIT_NOTE_C_MIPYME(213, "Nota de Crédito Electrónica MiPyME C");

    private final int code;
    private final String description;

    VoucherType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Returns the official ARCA numeric code for this voucher type.
     *
     * @return the numeric code
     */
    public int code() {
        return code;
    }

    /**
     * Returns the descriptive name of this voucher type.
     *
     * @return the description string
     */
    public String description() {
        return description;
    }

    /**
     * Resolves the {@link VoucherType} corresponding to the official ARCA code.
     *
     * @param code the numeric code to look up
     * @return the matching VoucherType
     * @throws ArcaValidationException if the code is not recognized
     */
    public static VoucherType fromCode(int code) {
        for (VoucherType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new ArcaValidationException("Unknown ARCA voucher type code: " + code);
    }
}
