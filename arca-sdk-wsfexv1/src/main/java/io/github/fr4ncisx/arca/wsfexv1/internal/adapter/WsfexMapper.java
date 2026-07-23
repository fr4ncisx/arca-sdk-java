package io.github.fr4ncisx.arca.wsfexv1.internal.adapter;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.Actividad;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ArrayOfActividad;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ArrayOfCmpAsoc;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ArrayOfItem;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ArrayOfOpcional;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ArrayOfPermiso;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXAuthRequest;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXErr;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXEvents;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXGetCMP;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXGetCMPR;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXLastCMP;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXLastCMPResponse;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXOutAuthorize;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXRequest;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.CmpAsoc;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXGetCMPResponseDataType;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXResponseAuthorize;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXResponseLastCMP;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.Item;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.Opcional;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.Permiso;
import io.github.fr4ncisx.arca.wsfexv1.model.AfipError;
import io.github.fr4ncisx.arca.wsfexv1.model.AssociatedVoucher;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportActivity;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportItem;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportOptionalField;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportPermit;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherConsultRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherConsultResponse;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherDetail;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherResponse;
import io.github.fr4ncisx.arca.wsfexv1.model.LastExportVoucherRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.LastExportVoucherResponse;

import java.util.List;
import java.util.stream.Collectors;

public final class WsfexMapper {

    private WsfexMapper() {
    }

    public static ClsFEXAuthRequest toAuthRequest(ArcaAccessTicket ticket, Cuit companyCuit) {
        if (ticket == null) {
            throw new ArcaValidationException("ticket must not be null");
        }
        if (companyCuit == null) {
            throw new ArcaValidationException("companyCuit must not be null");
        }
        ClsFEXAuthRequest auth = new ClsFEXAuthRequest();
        auth.setToken(ticket.token());
        auth.setSign(ticket.sign());
        auth.setCuit(companyCuit.number());
        return auth;
    }

    public static ClsFEXLastCMP toLastCmpRequest(ArcaAccessTicket ticket, Cuit companyCuit, LastExportVoucherRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        ClsFEXLastCMP req = new ClsFEXLastCMP();
        req.setToken(ticket.token());
        req.setSign(ticket.sign());
        req.setCuit(companyCuit.number());
        req.setPtoVenta(request.salesPoint());
        req.setCbteTipo(request.voucherType());
        return req;
    }

    public static ClsFEXGetCMP toGetCmpRequest(ExportVoucherConsultRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        ClsFEXGetCMP req = new ClsFEXGetCMP();
        req.setCbteTipo(request.voucherType());
        req.setPuntoVta(request.salesPoint());
        req.setCbteNro(request.voucherNumber());
        return req;
    }

    public static ClsFEXRequest toFexRequest(ExportVoucherRequest request) {
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        ClsFEXRequest req = new ClsFEXRequest();
        req.setId(request.id());
        req.setFechaCbte(request.voucherDate());
        req.setCbteTipo(request.voucherType());
        req.setPuntoVta(request.salesPoint());
        req.setCbteNro(request.voucherNumber());
        req.setTipoExpo(request.exportType());
        req.setPermisoExistente(request.permitExists());
        req.setDstCmp(request.destinationCountry());
        req.setCliente(request.clientName());
        req.setCuitPaisCliente(request.clientCountryCuit());
        req.setDomicilioCliente(request.clientAddress());
        req.setIdImpositivo(request.clientTaxId());
        req.setMonedaId(request.currencyId());
        req.setMonedaCtz(request.currencyExchangeRate());
        req.setCanMisMonExt(request.otherCurrencies());
        req.setObsComerciales(request.commercialObservations());
        req.setImpTotal(request.totalAmount());
        req.setObs(request.observations());
        req.setFormaPago(request.paymentTerms());
        req.setIncoterms(request.incoterms());
        req.setIncotermsDs(request.incotermsDescription());
        req.setIdiomaCbte(request.language());
        req.setFechaPago(request.paymentDate());

        if (request.permits() != null && !request.permits().isEmpty()) {
            ArrayOfPermiso permitsArray = new ArrayOfPermiso();
            for (ExportPermit p : request.permits()) {
                Permiso permiso = new Permiso();
                permiso.setIdPermiso(p.id());
                permiso.setDstMerc(p.destinationCountry());
                permitsArray.getPermiso().add(permiso);
            }
            req.setPermisos(permitsArray);
        }

        if (request.associatedVouchers() != null && !request.associatedVouchers().isEmpty()) {
            ArrayOfCmpAsoc assocArray = new ArrayOfCmpAsoc();
            for (AssociatedVoucher v : request.associatedVouchers()) {
                CmpAsoc asoc = new CmpAsoc();
                asoc.setCbteTipo(v.type());
                asoc.setCbtePuntoVta(v.salesPoint());
                asoc.setCbteNro(v.number());
                asoc.setCbteCuit(v.cuit());
                assocArray.getCmpAsoc().add(asoc);
            }
            req.setCmpsAsoc(assocArray);
        }

        if (request.items() != null && !request.items().isEmpty()) {
            ArrayOfItem itemsArray = new ArrayOfItem();
            for (ExportItem it : request.items()) {
                Item item = new Item();
                item.setProCodigo(it.code());
                item.setProDs(it.description());
                item.setProQty(it.quantity());
                item.setProUmed(it.unitOfMeasure());
                item.setProPrecioUni(it.unitPrice());
                item.setProBonificacion(it.discount());
                item.setProTotalItem(it.totalAmount());
                itemsArray.getItem().add(item);
            }
            req.setItems(itemsArray);
        }

        if (request.optionals() != null && !request.optionals().isEmpty()) {
            ArrayOfOpcional optsArray = new ArrayOfOpcional();
            for (ExportOptionalField opt : request.optionals()) {
                Opcional opcional = new Opcional();
                opcional.setId(opt.id());
                opcional.setValor(opt.value());
                optsArray.getOpcional().add(opcional);
            }
            req.setOpcionales(optsArray);
        }

        if (request.activities() != null && !request.activities().isEmpty()) {
            ArrayOfActividad actArray = new ArrayOfActividad();
            for (ExportActivity act : request.activities()) {
                Actividad actividad = new Actividad();
                actividad.setId(act.id());
                actArray.getActividad().add(actividad);
            }
            req.setActividades(actArray);
        }

        return req;
    }

    public static ExportVoucherResponse toExportVoucherResponse(FEXResponseAuthorize response) {
        if (response == null) {
            throw new ArcaValidationException("response must not be null");
        }
        long id = 0;
        long cuit = 0;
        short voucherType = 0;
        int salesPoint = 0;
        long voucherNumber = 0;
        String cae = null;
        String caeExpirationDate = null;

        ClsFEXOutAuthorize out = response.getFEXResultAuth();
        if (out != null) {
            id = out.getId();
            cuit = out.getCuit();
            voucherType = out.getCbteTipo();
            salesPoint = out.getPuntoVta();
            voucherNumber = out.getCbteNro();
            cae = out.getCae();
            caeExpirationDate = out.getFchVencCae();
        }

        return new ExportVoucherResponse(
                id,
                cuit,
                voucherType,
                salesPoint,
                voucherNumber,
                cae,
                caeExpirationDate,
                mapError(response.getFEXErr()),
                mapEvent(response.getFEXEvents())
        );
    }

    public static LastExportVoucherResponse toLastExportVoucherResponse(FEXResponseLastCMP response) {
        if (response == null) {
            throw new ArcaValidationException("response must not be null");
        }
        long lastNumber = 0;
        String lastDate = null;

        ClsFEXLastCMPResponse out = response.getFEXResultLastCMP();
        if (out != null) {
            lastNumber = out.getCbteNro();
            lastDate = out.getCbteFecha();
        }

        return new LastExportVoucherResponse(
                lastNumber,
                lastDate,
                mapError(response.getFEXErr()),
                mapEvent(response.getFEXEvents())
        );
    }

    public static ExportVoucherConsultResponse toExportVoucherConsultResponse(FEXGetCMPResponseDataType response) {
        if (response == null) {
            throw new ArcaValidationException("response must not be null");
        }
        ExportVoucherDetail detail = null;
        ClsFEXGetCMPR get = response.getFEXResultGet();
        if (get != null) {
            detail = mapDetail(get);
        }

        return new ExportVoucherConsultResponse(
                detail,
                mapError(response.getFEXErr()),
                mapEvent(response.getFEXEvents())
        );
    }

    private static ExportVoucherDetail mapDetail(ClsFEXGetCMPR get) {
        List<ExportPermit> permits = List.of();
        if (get.getPermisos() != null && get.getPermisos().getPermiso() != null) {
            permits = get.getPermisos().getPermiso().stream()
                    .map(p -> new ExportPermit(p.getIdPermiso(), p.getDstMerc()))
                    .collect(Collectors.toList());
        }

        List<AssociatedVoucher> assocs = List.of();
        if (get.getCmpsAsoc() != null && get.getCmpsAsoc().getCmpAsoc() != null) {
            assocs = get.getCmpsAsoc().getCmpAsoc().stream()
                    .map(v -> new AssociatedVoucher(v.getCbteTipo(), v.getCbtePuntoVta(), v.getCbteNro(), v.getCbteCuit()))
                    .collect(Collectors.toList());
        }

        List<ExportItem> items = List.of();
        if (get.getItems() != null && get.getItems().getItem() != null) {
            items = get.getItems().getItem().stream()
                    .map(it -> new ExportItem(it.getProCodigo(), it.getProDs(), it.getProQty(), it.getProUmed(), it.getProPrecioUni(), it.getProBonificacion(), it.getProTotalItem()))
                    .collect(Collectors.toList());
        }

        List<ExportOptionalField> optionals = List.of();
        if (get.getOpcionales() != null && get.getOpcionales().getOpcional() != null) {
            optionals = get.getOpcionales().getOpcional().stream()
                    .map(opt -> new ExportOptionalField(opt.getId(), opt.getValor()))
                    .collect(Collectors.toList());
        }

        List<ExportActivity> activities = List.of();
        if (get.getActividades() != null && get.getActividades().getActividad() != null) {
            activities = get.getActividades().getActividad().stream()
                    .map(act -> new ExportActivity(act.getId()))
                    .collect(Collectors.toList());
        }

        return new ExportVoucherDetail(
                get.getId(),
                get.getFechaCbte(),
                get.getCbteTipo(),
                get.getPuntoVta(),
                get.getCbteNro(),
                get.getTipoExpo(),
                get.getPermisoExistente(),
                permits,
                get.getDstCmp(),
                get.getCliente(),
                get.getCuitPaisCliente(),
                get.getDomicilioCliente(),
                get.getIdImpositivo(),
                get.getMonedaId(),
                get.getMonedaCtz(),
                get.getCanMisMonExt(),
                get.getObsComerciales(),
                get.getImpTotal(),
                get.getObs(),
                assocs,
                get.getFormaPago(),
                get.getIncoterms(),
                get.getIncotermsDs(),
                get.getIdiomaCbte(),
                items,
                get.getCae(),
                get.getFchVencCae(),
                get.getFechaCbteCae(),
                get.getResultado(),
                get.getMotivosObs(),
                optionals,
                get.getFechaPago(),
                activities
        );
    }

    private static List<AfipError> mapError(ClsFEXErr err) {
        if (err == null || (err.getErrCode() == 0 && (err.getErrMsg() == null || err.getErrMsg().isBlank()))) {
            return List.of();
        }
        return List.of(new AfipError(err.getErrCode(), err.getErrMsg()));
    }

    private static List<AfipError> mapEvent(ClsFEXEvents event) {
        if (event == null || (event.getEventCode() == 0 && (event.getEventMsg() == null || event.getEventMsg().isBlank()))) {
            return List.of();
        }
        return List.of(new AfipError(event.getEventCode(), event.getEventMsg()));
    }
}
