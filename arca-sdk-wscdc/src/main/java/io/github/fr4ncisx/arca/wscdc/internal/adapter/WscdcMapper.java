package io.github.fr4ncisx.arca.wscdc.internal.adapter;

import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wscdc.internal.generated.ArrayOfOpcional;
import io.github.fr4ncisx.arca.wscdc.internal.generated.CmpAuthRequest;
import io.github.fr4ncisx.arca.wscdc.internal.generated.CmpDatos;
import io.github.fr4ncisx.arca.wscdc.internal.generated.CmpResponse;
import io.github.fr4ncisx.arca.wscdc.internal.generated.DummyResponse;
import io.github.fr4ncisx.arca.wscdc.internal.generated.Err;
import io.github.fr4ncisx.arca.wscdc.internal.generated.Obs;
import io.github.fr4ncisx.arca.wscdc.internal.generated.Opcional;
import io.github.fr4ncisx.arca.wscdc.model.WscdcConstatRequest;
import io.github.fr4ncisx.arca.wscdc.model.WscdcConstatResponse;
import io.github.fr4ncisx.arca.wscdc.model.WscdcDummyResponse;
import io.github.fr4ncisx.arca.wscdc.model.WscdcError;
import io.github.fr4ncisx.arca.wscdc.model.WscdcObservation;
import io.github.fr4ncisx.arca.wscdc.model.WscdcOptionalField;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Utility mapper to translate between WSCDC JAX-WS generated stubs and public domain records.
 *
 * @author fr4ncisx
 * @since 0.9.0
 */
public final class WscdcMapper {

    private WscdcMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Map auth headers.
     *
     * @param ticket the authentication ticket
     * @param cuit the client cuit
     * @return the mapped auth request
     */
    public static CmpAuthRequest mapAuth(ArcaAccessTicket ticket, Cuit cuit) {
        CmpAuthRequest auth = new CmpAuthRequest();
        auth.setToken(ticket.token());
        auth.setSign(ticket.sign());
        auth.setCuit(cuit.number());
        return auth;
    }

    /**
     * Map request to stub data.
     *
     * @param request the public domain request
     * @return the mapped stub request data
     */
    public static CmpDatos mapRequest(WscdcConstatRequest request) {
        CmpDatos target = new CmpDatos();
        target.setCbteModo(request.voucherMode());
        target.setCuitEmisor(request.issuerCuit().number());
        target.setPtoVta(request.salesPoint());
        target.setCbteTipo(request.voucherType());
        target.setCbteNro(request.voucherNumber());
        target.setCbteFch(request.voucherDate().format(DateTimeFormatter.BASIC_ISO_DATE));
        target.setImpTotal(request.totalAmount().doubleValue());
        target.setCodAutorizacion(request.authorizationCode());
        target.setDocTipoReceptor(request.receiverDocType());
        target.setDocNroReceptor(request.receiverDocNumber());

        if (request.optionalFields() != null && !request.optionalFields().isEmpty()) {
            ArrayOfOpcional targetOpt = new ArrayOfOpcional();
            for (WscdcOptionalField f : request.optionalFields()) {
                if (f != null) {
                    Opcional o = new Opcional();
                    o.setId(f.id());
                    o.setValor(f.value());
                    targetOpt.getOpcional().add(o);
                }
            }
            target.setOpcionales(targetOpt);
        }
        return target;
    }

    /**
     * Map stub response to public domain response.
     *
     * @param source the JAXB stub response
     * @return the mapped public response
     */
    public static WscdcConstatResponse mapResponse(CmpResponse source) {
        String result = source.getResultado() != null ? source.getResultado() : "R";
        LocalDate processDate = parseDate(source.getFchProceso());

        List<WscdcObservation> observations = new ArrayList<>();
        if (source.getObservaciones() != null && source.getObservaciones().getObs() != null) {
            for (Obs o : source.getObservaciones().getObs()) {
                if (o != null) {
                    observations.add(new WscdcObservation(o.getCode(), o.getMsg()));
                }
            }
        }

        List<WscdcError> errors = new ArrayList<>();
        if (source.getErrors() != null && source.getErrors().getErr() != null) {
            for (Err e : source.getErrors().getErr()) {
                if (e != null) {
                    errors.add(new WscdcError(e.getCode(), e.getMsg()));
                }
            }
        }

        return new WscdcConstatResponse(
            result,
            processDate,
            Collections.unmodifiableList(observations),
            Collections.unmodifiableList(errors)
        );
    }

    /**
     * Map dummy response.
     *
     * @param source the JAXB dummy response
     * @return the mapped public dummy response
     */
    public static WscdcDummyResponse mapDummy(DummyResponse source) {
        return new WscdcDummyResponse(
            source.getAppServer() != null ? source.getAppServer() : "ERROR",
            source.getDbServer() != null ? source.getDbServer() : "ERROR",
            source.getAuthServer() != null ? source.getAuthServer() : "ERROR"
        );
    }

    private static @Nullable LocalDate parseDate(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String clean = value.trim();
            if (clean.length() >= 8) {
                return LocalDate.parse(clean.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
