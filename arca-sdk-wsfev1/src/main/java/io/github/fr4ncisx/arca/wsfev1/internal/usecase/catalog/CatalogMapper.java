package io.github.fr4ncisx.arca.wsfev1.internal.usecase.catalog;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.common.CommonMapper;
import io.github.fr4ncisx.arca.wsfev1.model.catalog.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Encapsulated package-private translator for WSFEv1 electronic invoicing catalogs.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
final class CatalogMapper {

    private CatalogMapper() {
    }

    private static void checkErrors(ArrayOfErr errorsContainer) {
        if (errorsContainer != null && errorsContainer.getErr() != null && !errorsContainer.getErr().isEmpty()) {
            Err error = errorsContainer.getErr().get(0);
            throw new ArcaSoapException("ARCA SOAP Error [" + error.getCode() + "]: " + error.getMsg());
        }
    }

    static List<VoucherTypeDetail> toVoucherTypes(CbteTipoResponse response) {
        if (response == null) {
            throw new ArcaSoapException("Received empty response from ARCA SOAP service");
        }
        checkErrors(response.getErrors());

        List<VoucherTypeDetail> list = new ArrayList<>();
        if (response.getResultGet() != null && response.getResultGet().getCbteTipo() != null) {
            for (CbteTipo item : response.getResultGet().getCbteTipo()) {
                list.add(new VoucherTypeDetail(
                        item.getId(),
                        item.getDesc(),
                        Optional.ofNullable(CommonMapper.parseDate(item.getFchDesde())),
                        Optional.ofNullable(CommonMapper.parseDate(item.getFchHasta()))
                ));
            }
        }
        return Collections.unmodifiableList(list);
    }

    static List<DocumentTypeInfo> toDocumentTypes(DocTipoResponse response) {
        if (response == null) {
            throw new ArcaSoapException("Received empty response from ARCA SOAP service");
        }
        checkErrors(response.getErrors());

        List<DocumentTypeInfo> list = new ArrayList<>();
        if (response.getResultGet() != null && response.getResultGet().getDocTipo() != null) {
            for (DocTipo item : response.getResultGet().getDocTipo()) {
                list.add(new DocumentTypeInfo(
                        item.getId(),
                        item.getDesc(),
                        Optional.ofNullable(CommonMapper.parseDate(item.getFchDesde())),
                        Optional.ofNullable(CommonMapper.parseDate(item.getFchHasta()))
                ));
            }
        }
        return Collections.unmodifiableList(list);
    }

    static List<VatTypeInfo> toVatTypes(IvaTipoResponse response) {
        if (response == null) {
            throw new ArcaSoapException("Received empty response from ARCA SOAP service");
        }
        checkErrors(response.getErrors());

        List<VatTypeInfo> list = new ArrayList<>();
        if (response.getResultGet() != null && response.getResultGet().getIvaTipo() != null) {
            for (IvaTipo item : response.getResultGet().getIvaTipo()) {
                int code;
                try {
                    code = Integer.parseInt(item.getId().trim());
                } catch (NumberFormatException e) {
                    throw new ArcaSoapException("Failed to parse VAT type code: " + item.getId(), e);
                }
                list.add(new VatTypeInfo(
                        code,
                        item.getDesc(),
                        Optional.ofNullable(CommonMapper.parseDate(item.getFchDesde())),
                        Optional.ofNullable(CommonMapper.parseDate(item.getFchHasta()))
                ));
            }
        }
        return Collections.unmodifiableList(list);
    }

    static List<CurrencyInfo> toCurrencies(MonedaResponse response) {
        if (response == null) {
            throw new ArcaSoapException("Received empty response from ARCA SOAP service");
        }
        checkErrors(response.getErrors());

        List<CurrencyInfo> list = new ArrayList<>();
        if (response.getResultGet() != null && response.getResultGet().getMoneda() != null) {
            for (Moneda item : response.getResultGet().getMoneda()) {
                list.add(new CurrencyInfo(
                        item.getId(),
                        item.getDesc(),
                        Optional.ofNullable(CommonMapper.parseDate(item.getFchDesde())),
                        Optional.ofNullable(CommonMapper.parseDate(item.getFchHasta()))
                ));
            }
        }
        return Collections.unmodifiableList(list);
    }

    static ExchangeRate toExchangeRate(FECotizacionResponse response) {
        if (response == null) {
            throw new ArcaSoapException("Received empty response from ARCA SOAP service");
        }
        checkErrors(response.getErrors());

        Cotizacion item = response.getResultGet();
        if (item == null) {
            throw new ArcaValidationException("Currency ID not found on ARCA server");
        }
        return new ExchangeRate(
                item.getMonId(),
                item.getMonCotiz(),
                Optional.ofNullable(CommonMapper.parseDate(item.getFchCotiz()))
        );
    }

    static List<ConceptTypeInfo> toConceptTypes(ConceptoTipoResponse response) {
        if (response == null) {
            throw new ArcaSoapException("Received empty response from ARCA SOAP service");
        }
        checkErrors(response.getErrors());

        List<ConceptTypeInfo> list = new ArrayList<>();
        if (response.getResultGet() != null && response.getResultGet().getConceptoTipo() != null) {
            for (ConceptoTipo item : response.getResultGet().getConceptoTipo()) {
                list.add(new ConceptTypeInfo(
                        item.getId(),
                        item.getDesc(),
                        Optional.ofNullable(CommonMapper.parseDate(item.getFchDesde())),
                        Optional.ofNullable(CommonMapper.parseDate(item.getFchHasta()))
                ));
            }
        }
        return Collections.unmodifiableList(list);
    }

    static List<OptionalFieldTypeInfo> toOptionalFieldTypes(OpcionalTipoResponse response) {
        if (response == null) {
            throw new ArcaSoapException("Received empty response from ARCA SOAP service");
        }
        checkErrors(response.getErrors());

        List<OptionalFieldTypeInfo> list = new ArrayList<>();
        if (response.getResultGet() != null && response.getResultGet().getOpcionalTipo() != null) {
            for (OpcionalTipo item : response.getResultGet().getOpcionalTipo()) {
                list.add(new OptionalFieldTypeInfo(
                        item.getId(),
                        item.getDesc(),
                        Optional.ofNullable(CommonMapper.parseDate(item.getFchDesde())),
                        Optional.ofNullable(CommonMapper.parseDate(item.getFchHasta()))
                ));
            }
        }
        return Collections.unmodifiableList(list);
    }

    static List<CountryInfo> toCountries(FEPaisResponse response) {
        if (response == null) {
            throw new ArcaSoapException("Received empty response from ARCA SOAP service");
        }
        checkErrors(response.getErrors());

        List<CountryInfo> list = new ArrayList<>();
        if (response.getResultGet() != null && response.getResultGet().getPaisTipo() != null) {
            for (PaisTipo item : response.getResultGet().getPaisTipo()) {
                list.add(new CountryInfo(
                        item.getId(),
                        item.getDesc()
                ));
            }
        }
        return Collections.unmodifiableList(list);
    }

    static List<TaxTypeInfo> toTaxTypes(FETributoResponse response) {
        if (response == null) {
            throw new ArcaSoapException("Received empty response from ARCA SOAP service");
        }
        checkErrors(response.getErrors());

        List<TaxTypeInfo> list = new ArrayList<>();
        if (response.getResultGet() != null && response.getResultGet().getTributoTipo() != null) {
            for (TributoTipo item : response.getResultGet().getTributoTipo()) {
                list.add(new TaxTypeInfo(
                        item.getId(),
                        item.getDesc(),
                        Optional.ofNullable(CommonMapper.parseDate(item.getFchDesde())),
                        Optional.ofNullable(CommonMapper.parseDate(item.getFchHasta()))
                ));
            }
        }
        return Collections.unmodifiableList(list);
    }

    static List<ActivityInfo> toActivities(FEActividadesResponse response) {
        if (response == null) {
            throw new ArcaSoapException("Received empty response from ARCA SOAP service");
        }
        checkErrors(response.getErrors());

        List<ActivityInfo> list = new ArrayList<>();
        if (response.getResultGet() != null && response.getResultGet().getActividadesTipo() != null) {
            for (ActividadesTipo item : response.getResultGet().getActividadesTipo()) {
                list.add(new ActivityInfo(
                        item.getId(),
                        item.getOrden(),
                        item.getDesc()
                ));
            }
        }
        return Collections.unmodifiableList(list);
    }

    static List<VatConditionInfo> toReceiverVatConditions(CondicionIvaReceptorResponse response) {
        if (response == null) {
            throw new ArcaSoapException("Received empty response from ARCA SOAP service");
        }
        checkErrors(response.getErrors());

        List<VatConditionInfo> list = new ArrayList<>();
        if (response.getResultGet() != null && response.getResultGet().getCondicionIvaReceptor() != null) {
            for (CondicionIvaReceptor item : response.getResultGet().getCondicionIvaReceptor()) {
                list.add(new VatConditionInfo(
                        item.getId(),
                        item.getDesc(),
                        item.getCmpClase()
                ));
            }
        }
        return Collections.unmodifiableList(list);
    }

    static int toMaxRecords(FERegXReqResponse response) {
        if (response == null) {
            throw new ArcaSoapException("Received empty response from ARCA SOAP service");
        }
        checkErrors(response.getErrors());

        int regXReq = response.getRegXReq();
        if (regXReq <= 0) {
            throw new ArcaValidationException("ARCA returned invalid max records count: " + regXReq);
        }
        return regXReq;
    }
}
