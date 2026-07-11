package io.github.fr4ncisx.arca.wsfev1.internal.usecase.catalog;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.model.catalog.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for catalog use cases.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
@SuppressWarnings("null")
class CatalogUseCasesTest {

    private ArcaConfig config;
    private AuthProvider authProvider;
    private ArcaAccessTicket ticket;

    @BeforeEach
    void setUp() {
        config = new ArcaConfig(
                Cuit.parse("20-33333333-4"),
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(2),
                Duration.ofSeconds(2)
        );
        ticket = new ArcaAccessTicket("token-val", "sign-val", Instant.now(), Instant.now().plusSeconds(300));
        authProvider = service -> ticket;
    }

    @Test
    void getVoucherTypesUseCaseSucceeds() {
        var response = new CbteTipoResponse();
        var resultGet = new ArrayOfCbteTipo();
        var item = new CbteTipo();
        item.setId(1);
        item.setDesc("Factura A");
        resultGet.getCbteTipo().add(item);
        response.setResultGet(resultGet);

        var capturedRequest = new AtomicReference<FEParamGetTiposCbte>();
        ArcaSoapPort<FEParamGetTiposCbte, CbteTipoResponse> port = req -> {
            capturedRequest.set(req);
            return response;
        };

        var useCase = new GetVoucherTypesUseCase(config, authProvider, port);
        List<VoucherTypeDetail> list = useCase.execute();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).code()).isEqualTo(1);
        assertThat(list.get(0).description()).isEqualTo("Factura A");
        assertThat(capturedRequest.get()).isNotNull();
        assertThat(capturedRequest.get().getAuth().getToken()).isEqualTo("token-val");
    }

    @Test
    void getDocumentTypesUseCaseSucceeds() {
        var response = new DocTipoResponse();
        var resultGet = new ArrayOfDocTipo();
        var item = new DocTipo();
        item.setId(80);
        item.setDesc("CUIT");
        resultGet.getDocTipo().add(item);
        response.setResultGet(resultGet);

        var capturedRequest = new AtomicReference<FEParamGetTiposDoc>();
        ArcaSoapPort<FEParamGetTiposDoc, DocTipoResponse> port = req -> {
            capturedRequest.set(req);
            return response;
        };

        var useCase = new GetDocumentTypesUseCase(config, authProvider, port);
        List<DocumentTypeInfo> list = useCase.execute();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).code()).isEqualTo(80);
        assertThat(list.get(0).description()).isEqualTo("CUIT");
    }

    @Test
    void getVatTypesUseCaseSucceeds() {
        var response = new IvaTipoResponse();
        var resultGet = new ArrayOfIvaTipo();
        var item = new IvaTipo();
        item.setId("5");
        item.setDesc("21%");
        resultGet.getIvaTipo().add(item);
        response.setResultGet(resultGet);

        var capturedRequest = new AtomicReference<FEParamGetTiposIva>();
        ArcaSoapPort<FEParamGetTiposIva, IvaTipoResponse> port = req -> {
            capturedRequest.set(req);
            return response;
        };

        var useCase = new GetVatTypesUseCase(config, authProvider, port);
        List<VatTypeInfo> list = useCase.execute();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).code()).isEqualTo(5);
    }

    @Test
    void getCurrenciesUseCaseSucceeds() {
        var response = new MonedaResponse();
        var resultGet = new ArrayOfMoneda();
        var item = new Moneda();
        item.setId("DOL");
        item.setDesc("Dolar");
        resultGet.getMoneda().add(item);
        response.setResultGet(resultGet);

        var capturedRequest = new AtomicReference<FEParamGetTiposMonedas>();
        ArcaSoapPort<FEParamGetTiposMonedas, MonedaResponse> port = req -> {
            capturedRequest.set(req);
            return response;
        };

        var useCase = new GetCurrenciesUseCase(config, authProvider, port);
        List<CurrencyInfo> list = useCase.execute();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).code()).isEqualTo("DOL");
    }

    @Test
    void getExchangeRateUseCaseSucceeds() {
        var response = new FECotizacionResponse();
        var item = new Cotizacion();
        item.setMonId("DOL");
        item.setMonCotiz(102.5);
        response.setResultGet(item);

        var capturedRequest = new AtomicReference<FEParamGetCotizacion>();
        ArcaSoapPort<FEParamGetCotizacion, FECotizacionResponse> port = req -> {
            capturedRequest.set(req);
            return response;
        };

        var useCase = new GetExchangeRateUseCase(config, authProvider, port);
        ExchangeRate rate = useCase.execute("DOL");

        assertThat(rate).isNotNull();
        assertThat(rate.currencyId()).isEqualTo("DOL");
        assertThat(rate.rate()).isEqualTo(102.5);
        assertThat(capturedRequest.get().getMonId()).isEqualTo("DOL");
    }

    @Test
    void getExchangeRateUseCaseThrowsOnNullCurrencyId() {
        ArcaSoapPort<FEParamGetCotizacion, FECotizacionResponse> port = req -> new FECotizacionResponse();
        var useCase = new GetExchangeRateUseCase(config, authProvider, port);

        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(ArcaValidationException.class);
        assertThatThrownBy(() -> useCase.execute("   "))
                .isInstanceOf(ArcaValidationException.class);
    }

    @Test
    void getMaxRecordsUseCaseSucceeds() {
        var response = new FERegXReqResponse();
        response.setRegXReq(250);

        var capturedRequest = new AtomicReference<FECompTotXRequest>();
        ArcaSoapPort<FECompTotXRequest, FERegXReqResponse> port = req -> {
            capturedRequest.set(req);
            return response;
        };

        var useCase = new GetMaxRecordsUseCase(config, authProvider, port);
        int max = useCase.execute();

        assertThat(max).isEqualTo(250);
    }

    @Test
    void getConceptTypesUseCaseSucceeds() {
        var response = new ConceptoTipoResponse();
        var resultGet = new ArrayOfConceptoTipo();
        var item = new ConceptoTipo();
        item.setId(1);
        item.setDesc("Productos");
        resultGet.getConceptoTipo().add(item);
        response.setResultGet(resultGet);

        var capturedRequest = new AtomicReference<FEParamGetTiposConcepto>();
        ArcaSoapPort<FEParamGetTiposConcepto, ConceptoTipoResponse> port = req -> {
            capturedRequest.set(req);
            return response;
        };

        var useCase = new GetConceptTypesUseCase(config, authProvider, port);
        List<ConceptTypeInfo> list = useCase.execute();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).code()).isEqualTo(1);
        assertThat(list.get(0).description()).isEqualTo("Productos");
    }
}
