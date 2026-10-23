package test.support.homologation;

import io.github.fr4ncisx.arca.client.spi.ArcaClient;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeRequest;
import io.github.fr4ncisx.arca.wsfev1.model.cae.CaeVatLine;
import io.github.fr4ncisx.arca.wsfev1.model.common.ConceptType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VatType;
import io.github.fr4ncisx.arca.wsfev1.model.common.VoucherType;
import io.github.fr4ncisx.arca.wsfev1.model.lastvoucher.LastVoucherRequest;
import io.github.fr4ncisx.arca.wsfev1.model.voucher.VoucherConsultRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorizes one explicitly configured homologation voucher and reconciles its identity.
 * <p>
 * Requires a reserved sales point and ARCA_TEST_WRITE_ENABLED=true. The voucher
 * number is supplied by the operator and is never advanced automatically on reruns.
 * Production endpoints are never used.
 *
 * @author fr4ncisx
 * @since 1.2.4
 */
@EnabledIfEnvironmentVariable(named = "ARCA_TEST_WRITE_ENABLED", matches = "true")
class RealHomologationAuthorizationIntegrationTest {

    /**
     * Queries an existing matching voucher or submits the explicitly configured next number once.
     * <p>
     * A transport error triggers a query, never an automatic resubmission.
     */
    @Test
    void authorizesOnceAndQueriesTheSameVoucher() {
        int salesPoint = Integer.parseInt(required("ARCA_TEST_SALES_POINT"));
        VoucherType type = VoucherType.fromCode(Integer.parseInt(required("ARCA_TEST_VOUCHER_TYPE")));
        long number = Long.parseLong(required("ARCA_TEST_VOUCHER_NUMBER"));
        double net = Double.parseDouble(required("ARCA_TEST_NET_AMOUNT"));
        double vat = Double.parseDouble(required("ARCA_TEST_VAT_AMOUNT"));
        assertThat(Double.isFinite(net) && net > 0 && Double.isFinite(vat) && vat >= 0)
                .as("finite non-negative test amounts").isTrue();
        List<CaeVatLine> lines = vat == 0 ? List.of() : List.of(new CaeVatLine(
                VatType.fromCode(Integer.parseInt(required("ARCA_TEST_VAT_TYPE"))), net, vat));
        CaeRequest request = new CaeRequest(type, salesPoint, number, ConceptType.PRODUCTS,
                Cuit.parse(required("ARCA_TEST_RECEIVER_CUIT")), net, 0, 0, vat, net + vat,
                LocalDate.parse(required("ARCA_TEST_VOUCHER_DATE")), lines);
        ArcaClient client = RealHomologationIntegrationTest.createClient(true);
        long lastNumber = client.wsfev1().getLastVoucher(new LastVoucherRequest(salesPoint, type)).lastNumber();
        assertThat(lastNumber >= number - 1).as("explicit voucher number follows the remote sequence").isTrue();
        String expectedCae = null;
        if (lastNumber == number - 1) {
            try {
                var result = client.wsfev1().requestCae(request);
                assertThat(result.approved()).as("homologation authorization approved").isTrue();
                expectedCae = result.cae();
            } catch (ArcaSoapException exception) {
                System.out.println("Authorization transport failed; reconciling without resubmission");
            }
        } else {
            System.out.println("Reconciling an existing homologation voucher without resubmission");
        }
        var response = client.wsfev1().getVoucher(new VoucherConsultRequest(salesPoint, type, number));
        assertThat(response.detail() != null).as("voucher query returned an authorization").isTrue();
        var detail = Objects.requireNonNull(response.detail());
        assertThat(detail.number() == number && detail.salesPoint() == salesPoint && detail.voucherType() == type)
                .as("queried voucher identity").isTrue();
        assertThat(detail.customerCuit().equals(request.customerCuit())
                && Math.abs(detail.netTaxed() - net) < 0.001 && Math.abs(detail.vatTotal() - vat) < 0.001
                && Math.abs(detail.total() - request.total()) < 0.001)
                .as("queried voucher recipient and amounts").isTrue();
        assertThat("A".equals(detail.result()) && !detail.cae().isBlank())
                .as("queried authorization status").isTrue();
        if (expectedCae != null) {
            assertThat(expectedCae.equals(detail.cae())).as("authorization and query CAE match").isTrue();
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        assertThat(value != null && !value.isBlank()).as(name + " configured").isTrue();
        return Objects.requireNonNull(value);
    }
}
