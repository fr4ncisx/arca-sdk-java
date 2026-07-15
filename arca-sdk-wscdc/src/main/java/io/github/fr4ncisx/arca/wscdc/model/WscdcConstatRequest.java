package io.github.fr4ncisx.arca.wscdc.model;

import io.github.fr4ncisx.arca.core.tax.Cuit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Represents a request to validate the authenticity of a voucher with the WSCDC service.
 *
 * @param voucherMode the mode of the voucher (e.g. "CAE", "CAEA")
 * @param issuerCuit the CUIT of the issuer
 * @param salesPoint the sales point of the voucher
 * @param voucherType the type of the voucher
 * @param voucherNumber the number of the voucher
 * @param voucherDate the date of the voucher
 * @param totalAmount the total amount of the voucher
 * @param authorizationCode the authorization code (CAE or CAEA) of the voucher
 * @param receiverDocType the identification document type code of the receiver
 * @param receiverDocNumber the identification document number of the receiver
 * @param optionalFields optional fields
 * @author fr4ncisx
 * @since 0.9.0
 */
public record WscdcConstatRequest(
    String voucherMode,
    Cuit issuerCuit,
    int salesPoint,
    int voucherType,
    long voucherNumber,
    LocalDate voucherDate,
    BigDecimal totalAmount,
    String authorizationCode,
    @Nullable String receiverDocType,
    @Nullable String receiverDocNumber,
    List<WscdcOptionalField> optionalFields
) {
    /**
     * Compact constructor to validate required parameters.
     */
    public WscdcConstatRequest {
        java.util.Objects.requireNonNull(voucherMode, "voucherMode must not be null");
        java.util.Objects.requireNonNull(issuerCuit, "issuerCuit must not be null");
        java.util.Objects.requireNonNull(voucherDate, "voucherDate must not be null");
        java.util.Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        java.util.Objects.requireNonNull(authorizationCode, "authorizationCode must not be null");
        java.util.Objects.requireNonNull(optionalFields, "optionalFields must not be null");
    }
}
