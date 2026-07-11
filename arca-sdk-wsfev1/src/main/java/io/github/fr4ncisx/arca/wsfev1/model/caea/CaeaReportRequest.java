package io.github.fr4ncisx.arca.wsfev1.model.caea;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;

import java.util.List;

/**
 * Domain record representing a request to report a batch of vouchers issued under a CAEA.
 * <p>
 * This request specifies the active CAEA code, target sales point, and voucher type, along
 * with the list of vouchers reported under contingency.
 *
 * @param caea        the active CAEA code
 * @param salesPoint  the sales point associated with the vouchers
 * @param voucherType the type of the vouchers
 * @param details     the list of reported voucher details
 * @author fr4ncisx
 * @since 0.4.0-M1
 */
public record CaeaReportRequest(
        String caea,
        int salesPoint,
        VoucherType voucherType,
        List<CaeaReportDetail> details
) {

    /**
     * Standard compact constructor validating CAEA reporting request parameters.
     */
    public CaeaReportRequest {
        if (caea == null || caea.strip().isEmpty()) {
            throw new ArcaValidationException("caea must not be null or empty");
        }
        if (salesPoint <= 0) {
            throw new ArcaValidationException("salesPoint must be strictly positive");
        }
        if (voucherType == null) {
            throw new ArcaValidationException("voucherType must not be null");
        }
        if (details == null || details.isEmpty()) {
            throw new ArcaValidationException("details list must not be null or empty");
        }
        details = List.copyOf(details);
    }
}
