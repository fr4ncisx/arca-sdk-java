package consumer.reference;

import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeRequest;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeVatLine;
import io.github.fr4ncisx.arca.wsfev1.model.common.ConceptType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VatType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Exercises the public DTO contract from a separately compiled Java consumer.
 *
 * @author fr4ncisx
 * @since 1.2.4
 */
public final class JavaConsumer {
    private JavaConsumer() {
    }

    /**
     * Checks snapshot isolation and the supported mutable-copy editing pattern.
     *
     * @return whether the consumer observes the documented DTO contract.
     */
    public static boolean verify() {
        var lines = new ArrayList<CaeVatLine>();
        lines.add(new CaeVatLine(VatType.VAT_21, 100, 21));
        var request = new CaeRequest(VoucherType.INVOICE_A, 1, 1, ConceptType.PRODUCTS,
                Cuit.parse("20-33333333-4"), 100, 0, 0, 21, 121, LocalDate.of(2026, 10, 22), lines);
        lines.clear();
        var edited = new ArrayList<>(request.vatLines());
        edited.clear();
        return request.vatLines().size() == 1 && edited.isEmpty() && request.total() == 121;
    }
}
