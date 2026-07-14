package io.github.fr4ncisx.arca.wsfev1.internal.usecase.catalog;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.model.catalog.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CatalogMapper}.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
class CatalogMapperTest {

    @Test
    void toVoucherTypesSucceeds() {
        var response = new CbteTipoResponse();
        var resultGet = new ArrayOfCbteTipo();
        var type = new CbteTipo();
        type.setId(1);
        type.setDesc("Factura A");
        type.setFchDesde("20200101");
        type.setFchHasta("20301231");
        resultGet.getCbteTipo().add(type);
        response.setResultGet(resultGet);

        List<VoucherTypeDetail> list = CatalogMapper.toVoucherTypes(response);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).code()).isEqualTo(1);
        assertThat(list.get(0).description()).isEqualTo("Factura A");
        assertThat(list.get(0).since()).contains(LocalDate.of(2020, 1, 1));
        assertThat(list.get(0).until()).contains(LocalDate.of(2030, 12, 31));
    }

    @Test
    void toVoucherTypesPropagatesSoapErrors() {
        var response = new CbteTipoResponse();
        var errors = new ArrayOfErr();
        var err = new Err();
        err.setCode(100);
        err.setMsg("Invalid token");
        errors.getErr().add(err);
        response.setErrors(errors);

        assertThatThrownBy(() -> CatalogMapper.toVoucherTypes(response))
                .isInstanceOf(ArcaSoapException.class)
                .hasMessageContaining("Invalid token");
    }

    @Test
    void toDocumentTypesSucceeds() {
        var response = new DocTipoResponse();
        var resultGet = new ArrayOfDocTipo();
        var type = new DocTipo();
        type.setId(80);
        type.setDesc("CUIT");
        type.setFchDesde("20100101");
        type.setFchHasta(null);
        resultGet.getDocTipo().add(type);
        response.setResultGet(resultGet);

        List<DocumentTypeInfo> list = CatalogMapper.toDocumentTypes(response);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).code()).isEqualTo(80);
        assertThat(list.get(0).description()).isEqualTo("CUIT");
        assertThat(list.get(0).since()).contains(LocalDate.of(2010, 1, 1));
        assertThat(list.get(0).until()).isEmpty();
    }

    @Test
    void toVatTypesSucceeds() {
        var response = new IvaTipoResponse();
        var resultGet = new ArrayOfIvaTipo();
        var type = new IvaTipo();
        type.setId("5");
        type.setDesc("21%");
        type.setFchDesde("20150101");
        type.setFchHasta(null);
        resultGet.getIvaTipo().add(type);
        response.setResultGet(resultGet);

        List<VatTypeInfo> list = CatalogMapper.toVatTypes(response);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).code()).isEqualTo(5);
        assertThat(list.get(0).description()).isEqualTo("21%");
        assertThat(list.get(0).since()).contains(LocalDate.of(2015, 1, 1));
        assertThat(list.get(0).until()).isEmpty();
    }

    @Test
    void toCurrenciesSucceeds() {
        var response = new MonedaResponse();
        var resultGet = new ArrayOfMoneda();
        var type = new Moneda();
        type.setId("DOL");
        type.setDesc("Dolar");
        type.setFchDesde("20000101");
        type.setFchHasta(null);
        resultGet.getMoneda().add(type);
        response.setResultGet(resultGet);

        List<CurrencyInfo> list = CatalogMapper.toCurrencies(response);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).code()).isEqualTo("DOL");
        assertThat(list.get(0).description()).isEqualTo("Dolar");
        assertThat(list.get(0).since()).contains(LocalDate.of(2000, 1, 1));
        assertThat(list.get(0).until()).isEmpty();
    }

    @Test
    void toExchangeRateSucceeds() {
        var response = new FECotizacionResponse();
        var item = new Cotizacion();
        item.setMonId("DOL");
        item.setMonCotiz(102.5);
        item.setFchCotiz("20260709");
        response.setResultGet(item);

        ExchangeRate rate = CatalogMapper.toExchangeRate(response);

        assertThat(rate.currencyId()).isEqualTo("DOL");
        assertThat(rate.rate()).isEqualTo(102.5);
        assertThat(rate.date()).contains(LocalDate.of(2026, 7, 9));
    }

    @Test
    void toExchangeRateThrowsOnNullResultGet() {
        var response = new FECotizacionResponse();
        response.setResultGet(null);

        assertThatThrownBy(() -> CatalogMapper.toExchangeRate(response))
                .isInstanceOf(ArcaValidationException.class);
    }

    @Test
    void toConceptTypesSucceeds() {
        var response = new ConceptoTipoResponse();
        var resultGet = new ArrayOfConceptoTipo();
        var type = new ConceptoTipo();
        type.setId(1);
        type.setDesc("Productos");
        type.setFchDesde("20100101");
        type.setFchHasta(null);
        resultGet.getConceptoTipo().add(type);
        response.setResultGet(resultGet);

        List<ConceptTypeInfo> list = CatalogMapper.toConceptTypes(response);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).code()).isEqualTo(1);
        assertThat(list.get(0).description()).isEqualTo("Productos");
        assertThat(list.get(0).since()).contains(LocalDate.of(2010, 1, 1));
        assertThat(list.get(0).until()).isEmpty();
    }

    @Test
    void toMaxRecordsSucceeds() {
        var response = new FERegXReqResponse();
        response.setRegXReq(250);

        int max = CatalogMapper.toMaxRecords(response);

        assertThat(max).isEqualTo(250);
    }

    @Test
    void toMaxRecordsThrowsOnInvalidCount() {
        var response = new FERegXReqResponse();
        response.setRegXReq(0);

        assertThatThrownBy(() -> CatalogMapper.toMaxRecords(response))
                .isInstanceOf(ArcaValidationException.class);
    }

    @Test
    void toOptionalFieldTypesSucceeds() {
        var response = new OpcionalTipoResponse();
        var resultGet = new ArrayOfOpcionalTipo();
        var type = new OpcionalTipo();
        type.setId("01");
        type.setDesc("Ingresos Brutos");
        type.setFchDesde("20200101");
        type.setFchHasta("20301231");
        resultGet.getOpcionalTipo().add(type);
        response.setResultGet(resultGet);

        List<OptionalFieldTypeInfo> list = CatalogMapper.toOptionalFieldTypes(response);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo("01");
        assertThat(list.get(0).description()).isEqualTo("Ingresos Brutos");
        assertThat(list.get(0).since()).contains(LocalDate.of(2020, 1, 1));
        assertThat(list.get(0).until()).contains(LocalDate.of(2030, 12, 31));
    }

    @Test
    void toCountriesSucceeds() {
        var response = new FEPaisResponse();
        var resultGet = new ArrayOfPaisTipo();
        var type = new PaisTipo();
        type.setId((short) 200);
        type.setDesc("Argentina");
        resultGet.getPaisTipo().add(type);
        response.setResultGet(resultGet);

        List<CountryInfo> list = CatalogMapper.toCountries(response);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo((short) 200);
        assertThat(list.get(0).description()).isEqualTo("Argentina");
    }

    @Test
    void toTaxTypesSucceeds() {
        var response = new FETributoResponse();
        var resultGet = new ArrayOfTributoTipo();
        var type = new TributoTipo();
        type.setId((short) 1);
        type.setDesc("Impuestos Nacionales");
        type.setFchDesde("20200101");
        type.setFchHasta("20301231");
        resultGet.getTributoTipo().add(type);
        response.setResultGet(resultGet);

        List<TaxTypeInfo> list = CatalogMapper.toTaxTypes(response);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo((short) 1);
        assertThat(list.get(0).description()).isEqualTo("Impuestos Nacionales");
        assertThat(list.get(0).since()).contains(LocalDate.of(2020, 1, 1));
        assertThat(list.get(0).until()).contains(LocalDate.of(2030, 12, 31));
    }

    @Test
    void toActivitiesSucceeds() {
        var response = new FEActividadesResponse();
        var resultGet = new ArrayOfActividadesTipo();
        var type = new ActividadesTipo();
        type.setId(620100L);
        type.setOrden((short) 1);
        type.setDesc("Servicios de software");
        resultGet.getActividadesTipo().add(type);
        response.setResultGet(resultGet);

        List<ActivityInfo> list = CatalogMapper.toActivities(response);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo(620100L);
        assertThat(list.get(0).order()).isEqualTo((short) 1);
        assertThat(list.get(0).description()).isEqualTo("Servicios de software");
    }

    @Test
    void toReceiverVatConditionsSucceeds() {
        var response = new CondicionIvaReceptorResponse();
        var resultGet = new ArrayOfCondicionIvaReceptor();
        var type = new CondicionIvaReceptor();
        type.setId(1);
        type.setDesc("Responsable Inscripto");
        type.setCmpClase("A");
        resultGet.getCondicionIvaReceptor().add(type);
        response.setResultGet(resultGet);

        List<VatConditionInfo> list = CatalogMapper.toReceiverVatConditions(response);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo(1);
        assertThat(list.get(0).description()).isEqualTo("Responsable Inscripto");
        assertThat(list.get(0).voucherClass()).isEqualTo("A");
    }
}
