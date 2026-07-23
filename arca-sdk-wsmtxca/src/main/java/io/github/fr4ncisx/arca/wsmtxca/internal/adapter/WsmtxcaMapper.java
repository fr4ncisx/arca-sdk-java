package io.github.fr4ncisx.arca.wsmtxca.internal.adapter;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ArrayCodigosDescripcionesType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ArrayComprobantesAsociadosType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ArrayItemsType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ArrayOtrosTributosType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ArraySubtotalesIVAType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.AuthRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.AutorizarComprobanteRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.AutorizarComprobanteResponseType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.CodigoDescripcionType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ComprobanteAsociadoType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ComprobanteCAEResponseType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ComprobanteType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultaComprobanteRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultaUltimoComprobanteAutorizadoRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarComprobanteRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarComprobanteResponseType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarUltimoComprobanteAutorizadoRequestType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ConsultarUltimoComprobanteAutorizadoResponseType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.ItemType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.OtroTributoType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.SiNoSimpleType;
import io.github.fr4ncisx.arca.wsmtxca.internal.generated.SubtotalIVAType;
import io.github.fr4ncisx.arca.wsmtxca.model.AssociatedVoucher;
import io.github.fr4ncisx.arca.wsmtxca.model.ItemDetail;
import io.github.fr4ncisx.arca.wsmtxca.model.OtherTaxDetail;
import io.github.fr4ncisx.arca.wsmtxca.model.TaxDetail;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaError;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaLastVoucherRequest;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaLastVoucherResponse;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherConsultRequest;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherConsultResponse;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherRequest;
import io.github.fr4ncisx.arca.wsmtxca.model.WsmtxcaVoucherResponse;
import org.jspecify.annotations.Nullable;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WsmtxcaMapper {

    private WsmtxcaMapper() {
    }

    public static AuthRequestType toAuthRequest(ArcaAccessTicket ticket, Cuit companyCuit) {
        if (ticket == null) {
            throw new ArcaValidationException("ticket must not be null");
        }
        if (companyCuit == null) {
            throw new ArcaValidationException("companyCuit must not be null");
        }
        AuthRequestType auth = new AuthRequestType();
        auth.setToken(ticket.token());
        auth.setSign(ticket.sign());
        auth.setCuitRepresentada(companyCuit.number());
        return auth;
    }

    public static ConsultarUltimoComprobanteAutorizadoRequestType toLastVoucherRequest(
            ArcaAccessTicket ticket, Cuit companyCuit, WsmtxcaLastVoucherRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        AuthRequestType auth = toAuthRequest(ticket, companyCuit);

        ConsultaUltimoComprobanteAutorizadoRequestType inner = new ConsultaUltimoComprobanteAutorizadoRequestType();
        inner.setNumeroPuntoVenta(request.salesPoint());
        inner.setCodigoTipoComprobante(request.voucherType());

        ConsultarUltimoComprobanteAutorizadoRequestType req = new ConsultarUltimoComprobanteAutorizadoRequestType();
        req.setAuthRequest(auth);
        req.setConsultaUltimoComprobanteAutorizadoRequest(inner);
        return req;
    }

    public static WsmtxcaLastVoucherResponse toLastVoucherResponse(ConsultarUltimoComprobanteAutorizadoResponseType response) {
        if (response == null) {
            throw new ArcaValidationException("response must not be null");
        }
        long lastNumber = response.getNumeroComprobante() != null ? response.getNumeroComprobante().longValue() : 0L;
        return new WsmtxcaLastVoucherResponse(
                0,
                (short) 0,
                lastNumber,
                mapErrors(response.getArrayErrores())
        );
    }

    public static ConsultarComprobanteRequestType toVoucherConsultRequest(
            ArcaAccessTicket ticket, Cuit companyCuit, WsmtxcaVoucherConsultRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        AuthRequestType auth = toAuthRequest(ticket, companyCuit);

        ConsultaComprobanteRequestType inner = new ConsultaComprobanteRequestType();
        inner.setCodigoTipoComprobante(request.voucherType());
        inner.setNumeroPuntoVenta(request.salesPoint());
        inner.setNumeroComprobante((int) request.voucherNumber());

        ConsultarComprobanteRequestType req = new ConsultarComprobanteRequestType();
        req.setAuthRequest(auth);
        req.setConsultaComprobanteRequest(inner);
        return req;
    }

    public static WsmtxcaVoucherConsultResponse toVoucherConsultResponse(ConsultarComprobanteResponseType response) {
        if (response == null) {
            throw new ArcaValidationException("response must not be null");
        }
        WsmtxcaVoucherRequest voucher = null;
        String authCode = null;
        String authType = null;
        if (response.getComprobante() != null) {
            ComprobanteType comp = response.getComprobante();
            voucher = toVoucherRequestModel(comp);
            if (comp.getCodigoAutorizacion() != null) {
                authCode = String.valueOf(comp.getCodigoAutorizacion());
            }
            if (comp.getCodigoTipoAutorizacion() != null) {
                authType = comp.getCodigoTipoAutorizacion().value();
            }
        }
        return new WsmtxcaVoucherConsultResponse(
                voucher,
                authCode,
                authType,
                mapErrors(response.getArrayErrores())
        );
    }

    public static AutorizarComprobanteRequestType toAuthorizeRequest(
            ArcaAccessTicket ticket, Cuit companyCuit, WsmtxcaVoucherRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        AuthRequestType auth = toAuthRequest(ticket, companyCuit);
        ComprobanteType comp = toComprobanteType(request);

        AutorizarComprobanteRequestType req = new AutorizarComprobanteRequestType();
        req.setAuthRequest(auth);
        req.setComprobanteCAERequest(comp);
        return req;
    }

    public static WsmtxcaVoucherResponse toVoucherResponse(AutorizarComprobanteResponseType response) {
        if (response == null) {
            throw new ArcaValidationException("response must not be null");
        }
        short type = 0;
        int salesPoint = 0;
        long num = 0;
        String authCode = null;
        String authType = "CAE";
        LocalDate expiration = null;
        LocalDate issue = null;

        ComprobanteCAEResponseType out = response.getComprobanteResponse();
        if (out != null) {
            type = out.getCodigoTipoComprobante();
            salesPoint = out.getNumeroPuntoVenta();
            num = out.getNumeroComprobante();
            authCode = String.valueOf(out.getCAE());
            expiration = toLocalDate(out.getFechaVencimientoCAE());
            issue = toLocalDate(out.getFechaEmision());
        }

        String status = response.getResultado() != null ? response.getResultado().value() : "R";

        return new WsmtxcaVoucherResponse(
                type,
                salesPoint,
                num,
                authCode,
                authType,
                expiration,
                issue,
                status,
                mapErrors(response.getArrayErrores()),
                mapErrors(response.getArrayObservaciones())
        );
    }

    private static ComprobanteType toComprobanteType(WsmtxcaVoucherRequest request) {
        ComprobanteType comp = new ComprobanteType();
        comp.setCodigoTipoComprobante(request.voucherType());
        comp.setNumeroPuntoVenta(request.salesPoint());
        comp.setNumeroComprobante((int) request.voucherNumber());
        comp.setFechaEmision(toXmlCalendar(request.issueDate()));
        comp.setCodigoTipoDocumento(request.docType());
        comp.setNumeroDocumento(request.docNumber());
        comp.setCondicionIVAReceptor(request.receiverVatCondition());
        comp.setImporteGravado(request.taxableAmount());
        comp.setImporteNoGravado(request.nonTaxableAmount());
        comp.setImporteExento(request.exemptAmount());
        comp.setImporteSubtotal(request.subtotalAmount());
        comp.setImporteOtrosTributos(request.otherTaxesAmount());
        comp.setImporteTotal(request.totalAmount());
        comp.setCodigoMoneda(request.currencyId());
        comp.setCotizacionMoneda(request.exchangeRate());
        comp.setCancelaEnMismaMonedaExtranjera(request.cancellationSameCurrency() != null ?
                SiNoSimpleType.fromValue(request.cancellationSameCurrency()) : null);
        comp.setObservaciones(request.comments());
        comp.setCodigoConcepto(request.concept());
        comp.setFechaServicioDesde(toXmlCalendar(request.serviceStartDate()));
        comp.setFechaServicioHasta(toXmlCalendar(request.serviceEndDate()));
        comp.setFechaVencimientoPago(toXmlCalendar(request.paymentDueDate()));

        if (request.items() != null && !request.items().isEmpty()) {
            ArrayItemsType itemsArray = new ArrayItemsType();
            for (ItemDetail itemModel : request.items()) {
                ItemType item = new ItemType();
                item.setUnidadesMtx(itemModel.unitsMtx());
                item.setCodigoMtx(itemModel.gtin());
                item.setCodigo(itemModel.internalCode());
                item.setDescripcion(itemModel.description());
                item.setCantidad(itemModel.quantity());
                item.setCodigoUnidadMedida(itemModel.unitOfMeasureCode());
                item.setPrecioUnitario(itemModel.unitPrice());
                item.setImporteBonificacion(itemModel.discountAmount());
                item.setCodigoCondicionIVA(itemModel.vatConditionCode());
                item.setImporteIVA(itemModel.vatAmount());
                item.setImporteItem(itemModel.itemAmount());
                itemsArray.getItem().add(item);
            }
            comp.setArrayItems(itemsArray);
        }

        if (request.vatSubtotals() != null && !request.vatSubtotals().isEmpty()) {
            ArraySubtotalesIVAType vatArray = new ArraySubtotalesIVAType();
            for (TaxDetail vatModel : request.vatSubtotals()) {
                SubtotalIVAType sub = new SubtotalIVAType();
                sub.setCodigo(vatModel.vatConditionId());
                sub.setImporte(vatModel.taxAmount());
                vatArray.getSubtotalIVA().add(sub);
            }
            comp.setArraySubtotalesIVA(vatArray);
        }

        if (request.otherTaxes() != null && !request.otherTaxes().isEmpty()) {
            ArrayOtrosTributosType taxArray = new ArrayOtrosTributosType();
            for (OtherTaxDetail taxModel : request.otherTaxes()) {
                OtroTributoType tax = new OtroTributoType();
                tax.setCodigo(taxModel.taxId());
                tax.setDescripcion(taxModel.description());
                tax.setBaseImponible(taxModel.baseAmount());
                tax.setImporte(taxModel.taxAmount());
                taxArray.getOtroTributo().add(tax);
            }
            comp.setArrayOtrosTributos(taxArray);
        }

        if (request.associatedVouchers() != null && !request.associatedVouchers().isEmpty()) {
            ArrayComprobantesAsociadosType assocArray = new ArrayComprobantesAsociadosType();
            for (AssociatedVoucher assocModel : request.associatedVouchers()) {
                ComprobanteAsociadoType assoc = new ComprobanteAsociadoType();
                assoc.setCodigoTipoComprobante(assocModel.voucherType());
                assoc.setNumeroPuntoVenta(assocModel.salesPoint());
                assoc.setNumeroComprobante((int) assocModel.voucherNumber());
                assoc.setCuit(assocModel.cuit());
                assocArray.getComprobanteAsociado().add(assoc);
            }
            comp.setArrayComprobantesAsociados(assocArray);
        }

        return comp;
    }

    private static WsmtxcaVoucherRequest toVoucherRequestModel(ComprobanteType comp) {
        List<ItemDetail> items = new ArrayList<>();
        if (comp.getArrayItems() != null && comp.getArrayItems().getItem() != null) {
            for (ItemType item : comp.getArrayItems().getItem()) {
                items.add(new ItemDetail(
                        item.getUnidadesMtx(),
                        item.getCodigoMtx(),
                        item.getCodigo(),
                        item.getDescripcion(),
                        item.getCantidad(),
                        item.getCodigoUnidadMedida(),
                        item.getPrecioUnitario(),
                        item.getImporteBonificacion(),
                        item.getCodigoCondicionIVA(),
                        item.getImporteIVA(),
                        item.getImporteItem()
                ));
            }
        }

        List<TaxDetail> vatSubtotals = new ArrayList<>();
        if (comp.getArraySubtotalesIVA() != null && comp.getArraySubtotalesIVA().getSubtotalIVA() != null) {
            for (SubtotalIVAType sub : comp.getArraySubtotalesIVA().getSubtotalIVA()) {
                vatSubtotals.add(new TaxDetail(
                        sub.getCodigo(),
                        BigDecimal.ZERO,
                        sub.getImporte()
                ));
            }
        }

        List<OtherTaxDetail> otherTaxes = new ArrayList<>();
        if (comp.getArrayOtrosTributos() != null && comp.getArrayOtrosTributos().getOtroTributo() != null) {
            for (OtroTributoType tax : comp.getArrayOtrosTributos().getOtroTributo()) {
                otherTaxes.add(new OtherTaxDetail(
                        tax.getCodigo(),
                        tax.getDescripcion(),
                        tax.getBaseImponible(),
                        BigDecimal.ZERO,
                        tax.getImporte()
                ));
            }
        }

        List<AssociatedVoucher> assocs = new ArrayList<>();
        if (comp.getArrayComprobantesAsociados() != null && comp.getArrayComprobantesAsociados().getComprobanteAsociado() != null) {
            for (ComprobanteAsociadoType assoc : comp.getArrayComprobantesAsociados().getComprobanteAsociado()) {
                assocs.add(new AssociatedVoucher(
                        assoc.getCodigoTipoComprobante(),
                        assoc.getNumeroPuntoVenta(),
                        assoc.getNumeroComprobante(),
                        assoc.getCuit()
                ));
            }
        }

        return new WsmtxcaVoucherRequest(
                comp.getCodigoTipoComprobante(),
                comp.getNumeroPuntoVenta(),
                comp.getNumeroComprobante(),
                toLocalDate(comp.getFechaEmision()),
                comp.getCodigoTipoDocumento(),
                comp.getNumeroDocumento(),
                comp.getCondicionIVAReceptor(),
                comp.getImporteGravado(),
                comp.getImporteNoGravado(),
                comp.getImporteExento(),
                comp.getImporteSubtotal(),
                comp.getImporteOtrosTributos(),
                comp.getImporteTotal(),
                comp.getCodigoMoneda(),
                comp.getCotizacionMoneda(),
                comp.getCancelaEnMismaMonedaExtranjera() != null ? comp.getCancelaEnMismaMonedaExtranjera().value() : null,
                comp.getObservaciones(),
                comp.getCodigoConcepto(),
                toLocalDate(comp.getFechaServicioDesde()),
                toLocalDate(comp.getFechaServicioHasta()),
                toLocalDate(comp.getFechaVencimientoPago()),
                Collections.unmodifiableList(items),
                Collections.unmodifiableList(vatSubtotals),
                Collections.unmodifiableList(otherTaxes),
                Collections.unmodifiableList(assocs)
        );
    }

    private static List<WsmtxcaError> mapErrors(@Nullable ArrayCodigosDescripcionesType array) {
        if (array == null || array.getCodigoDescripcion() == null) {
            return Collections.emptyList();
        }
        List<WsmtxcaError> list = new ArrayList<>();
        for (CodigoDescripcionType cd : array.getCodigoDescripcion()) {
            list.add(new WsmtxcaError(cd.getCodigo(), cd.getDescripcion()));
        }
        return Collections.unmodifiableList(list);
    }

    private static @Nullable LocalDate toLocalDate(@Nullable XMLGregorianCalendar calendar) {
        if (calendar == null) {
            return null;
        }
        return LocalDate.of(calendar.getYear(), calendar.getMonth(), calendar.getDay());
    }

    private static @Nullable XMLGregorianCalendar toXmlCalendar(@Nullable LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(
                    localDate.getYear(),
                    localDate.getMonthValue(),
                    localDate.getDayOfMonth(),
                    DatatypeConstants.FIELD_UNDEFINED,
                    DatatypeConstants.FIELD_UNDEFINED,
                    DatatypeConstants.FIELD_UNDEFINED,
                    DatatypeConstants.FIELD_UNDEFINED,
                    DatatypeConstants.FIELD_UNDEFINED
            );
        } catch (Exception e) {
            throw new ArcaValidationException("Failed to convert LocalDate to XMLGregorianCalendar", e);
        }
    }
}
