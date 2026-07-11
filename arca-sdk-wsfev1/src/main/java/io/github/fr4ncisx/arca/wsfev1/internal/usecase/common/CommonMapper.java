package io.github.fr4ncisx.arca.wsfev1.internal.usecase.common;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.FEAuthRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Shared utility mappings and formats for WSFEv1 request/response translation.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public final class CommonMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CommonMapper() {
    }

    /**
     * Translates an access ticket and CUIT to a SOAP FEAuthRequest structure.
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
     * Formats a local date using the standard yyyyMMdd pattern required by ARCA.
     *
     * @param date the date to format
     * @return the formatted date string, or null if input is null
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMATTER);
    }

    /**
     * Parses a date string in the standard yyyyMMdd pattern.
     *
     * @param raw the raw date string
     * @return the parsed LocalDate, or null if input is null or blank
     */
    public static LocalDate parseDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(raw.trim(), DATE_FORMATTER);
    }
}
