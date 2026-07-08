package io.github.fr4ncisx.arca.wsfev1.internal.mapper;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.model.CaeRequest;
import io.github.fr4ncisx.arca.wsfev1.model.CaeVatLine;
import io.github.fr4ncisx.arca.wsfev1.model.LastVoucherRequest;

import java.time.format.DateTimeFormatter;

/**
 * Translator for maps input models into JAXB XML structures.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public final class WsfeRequestMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private WsfeRequestMapper() {
    }

    /**
     * Translates access ticket and CUIT to SOAP Auth request.
     *
     * @param ticket      the WSAA access ticket
     * @param companyCuit the taxpayer CUIT of the company
     * @return the generated FEAuthRequest
     */
    public static FEAuthRequest toAuthRequest(ArcaAccessTicket ticket, Cuit companyCuit) {
        if (ticket == null) {
            throw new ArcaValidationException("ticket must not be null");
        }
        if (companyCuit == null) {
            throw new ArcaValidationException("companyCuit must not be null");
        }
        FEAuthRequest auth = new FEAuthRequest();
        auth.setToken(ticket.token());
        auth.setSign(ticket.sign());
        auth.setCuit(companyCuit.number());
        return auth;
    }

    /**
     * Translates last voucher query request.
     *
     * @param auth    the SOAP Auth request structure
     * @param request the public query request parameters
     * @return the generated FECompUltimoAutorizado
     */
    public static FECompUltimoAutorizado toSoapRequest(FEAuthRequest auth, LastVoucherRequest request) {
        if (auth == null) {
            throw new ArcaValidationException("auth must not be null");
        }
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }
        FECompUltimoAutorizado soapRequest = new FECompUltimoAutorizado();
        soapRequest.setAuth(auth);
        soapRequest.setPtoVta(request.salesPoint());
        soapRequest.setCbteTipo(request.voucherType().code());
        return soapRequest;
    }

    /**
     * Translates CAE authorization request.
     *
     * @param auth    the SOAP Auth request structure
     * @param request the public CAE request parameters
     * @return the generated FECAESolicitar
     */
    public static FECAESolicitar toSoapRequest(FEAuthRequest auth, CaeRequest request) {
        if (auth == null) {
            throw new ArcaValidationException("auth must not be null");
        }
        if (request == null) {
            throw new ArcaValidationException("request must not be null");
        }

        FECAECabRequest cab = new FECAECabRequest();
        cab.setCantReg(1);
        cab.setPtoVta(request.salesPoint());
        cab.setCbteTipo(request.voucherType().code());

        FECAEDetRequest det = new FECAEDetRequest();
        det.setConcepto(request.concept().code());
        det.setDocTipo(80);
        det.setDocNro(request.customerCuit().number());
        det.setCbteDesde(request.number());
        det.setCbteHasta(request.number());
        det.setCbteFch(request.date().format(DATE_FORMATTER));
        det.setImpTotal(request.total());
        det.setImpTotConc(0.0);
        det.setImpNeto(request.netTaxed());
        det.setImpOpEx(request.exempted());
        det.setImpTrib(0.0);
        det.setImpIVA(request.vatTotal());
        det.setMonId("PES");
        det.setMonCotiz(1.0);

        if (!request.vatLines().isEmpty()) {
            ArrayOfAlicIva arrayIva = new ArrayOfAlicIva();
            for (CaeVatLine line : request.vatLines()) {
                AlicIva alic = new AlicIva();
                alic.setId(line.vatType().code());
                alic.setBaseImp(line.taxBase());
                alic.setImporte(line.vatAmount());
                arrayIva.getAlicIva().add(alic);
            }
            det.setIva(arrayIva);
        }

        ArrayOfFECAEDetRequest arrayDet = new ArrayOfFECAEDetRequest();
        arrayDet.getFECAEDetRequest().add(det);

        FECAERequest caeReq = new FECAERequest();
        caeReq.setFeCabReq(cab);
        caeReq.setFeDetReq(arrayDet);

        FECAESolicitar solicitar = new FECAESolicitar();
        solicitar.setAuth(auth);
        solicitar.setFeCAEReq(caeReq);
        return solicitar;
    }
}
