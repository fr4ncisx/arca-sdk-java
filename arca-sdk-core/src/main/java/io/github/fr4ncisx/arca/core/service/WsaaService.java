package io.github.fr4ncisx.arca.core.service;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;

/**
 * Standard ARCA business services authenticated through the WSAA gateway.
 * <p>
 * Each enum value encapsulates the literal service identifier registered
 * in the ARCA authentication authority.
 *
 * @author fr4ncisx
 * @since 1.3.0
 */
public enum WsaaService {

    /**
     * Electronic invoicing service (WSFEv1).
     */
    WSFE("wsfe"),

    /**
     * Taxpayer registry service (ws_sr_padron_a4).
     */
    REGISTRY_A4("ws_sr_padron_a4"),

    /**
     * Export invoicing service (WSFEXv1).
     */
    WSFEX("wsfex"),

    /**
     * Itemized invoicing service (WSMTXCA).
     */
    WSMTXCA("wsmtxca"),

    /**
     * Voucher constatation service (WSCDC).
     */
    WSCDC("wscdc");

    private final String serviceId;

    WsaaService(String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * Returns the literal service name accepted by WSAA.
     *
     * @return WSAA service identifier string
     */
    public String serviceId() {
        return serviceId;
    }

    /**
     * Resolves an enum instance from its literal service identifier.
     *
     * @param serviceId the literal identifier
     * @return the matching WsaaService
     * @throws ArcaValidationException if serviceId is blank or unrecognized
     */
    public static WsaaService fromServiceId(String serviceId) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new ArcaValidationException("serviceId must not be blank");
        }
        String normalized = serviceId.trim();
        for (WsaaService service : values()) {
            if (service.serviceId.equalsIgnoreCase(normalized)) {
                return service;
            }
        }
        throw new ArcaValidationException("Unknown WSAA service: " + serviceId);
    }
}
