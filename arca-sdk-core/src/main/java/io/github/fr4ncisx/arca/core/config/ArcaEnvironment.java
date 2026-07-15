package io.github.fr4ncisx.arca.core.config;

import java.net.URI;

/**
 * Official ARCA environments with WSAA, WSFEv1, Registry, WSFEXv1, and WSMTXCA endpoints.
 * <p>
 * Each constant exposes the full URLs for the authentication
 * (WSAA), electronic invoicing (WSFEv1), taxpayer registry (ws_sr_padron_a4),
 * export invoicing (WSFEXv1), and itemized invoicing (WSMTXCA) services.
 *
 * @see <a href="https://wsaahomo.afip.gov.ar/ws/services/LoginCms?wsdl">WSDL WSAA Homologacion</a>
 * @see <a href="https://wsaa.afip.gov.ar/ws/services/LoginCms?wsdl">WSDL WSAA Produccion</a>
 * @see <a href="https://wswhomo.afip.gov.ar/wsfev1/service.asmx?wsdl">WSDL WSFEv1 Homologacion</a>
 * @see <a href="https://servicios1.afip.gov.ar/wsfev1/service.asmx?wsdl">WSDL WSFEv1 Produccion</a>
 * @see <a href="https://fwshomo.afip.gov.ar/wsmtxca/services/MTXCAService?wsdl">WSDL WSMTXCA Homologacion</a>
 * @see <a href="https://serviciosjava.afip.gob.ar/wsmtxca/services/MTXCAService?wsdl">WSDL WSMTXCA Produccion</a>
 * @author fr4ncisx
 * @since 0.1.0-M1
 */
public enum ArcaEnvironment {

    /**
     * ARCA testing (homologacion) environment.
     * <p>
     * WSAA: {@code https://wsaahomo.afip.gov.ar/ws/services/LoginCms}
     * <p>
     * WSFEv1: {@code https://wswhomo.afip.gov.ar/wsfev1/service.asmx}
     * <p>
     * Registry: {@code https://awshomo.afip.gov.ar/sr-padron/webservices/personaServiceA4}
     * <p>
     * WSFEXv1: {@code https://wswhomo.afip.gov.ar/wsfexv1/service.asmx}
     * <p>
     * WSMTXCA: {@code https://fwshomo.afip.gov.ar/wsmtxca/services/MTXCAService}
     */
    HOMOLOGACION(
            URI.create("https://wsaahomo.afip.gov.ar/ws/services/LoginCms"),
            URI.create("https://wswhomo.afip.gov.ar/wsfev1/service.asmx"),
            URI.create("https://awshomo.afip.gov.ar/sr-padron/webservices/personaServiceA4"),
            URI.create("https://wswhomo.afip.gov.ar/wsfexv1/service.asmx"),
            URI.create("https://fwshomo.afip.gov.ar/wsmtxca/services/MTXCAService"),
            URI.create("https://wswhomo.afip.gov.ar/WSCDC/service.asmx")),

    /**
     * ARCA production environment.
     * <p>
     * WSAA: {@code https://wsaa.afip.gov.ar/ws/services/LoginCms}
     * <p>
     * WSFEv1: {@code https://servicios1.afip.gov.ar/wsfev1/service.asmx}
     * <p>
     * Registry: {@code https://aws.afip.gov.ar/sr-padron/webservices/personaServiceA4}
     * <p>
     * WSFEXv1: {@code https://servicios1.afip.gov.ar/wsfexv1/service.asmx}
     * <p>
     * WSMTXCA: {@code https://serviciosjava.afip.gob.ar/wsmtxca/services/MTXCAService}
     */
    PRODUCCION(
            URI.create("https://wsaa.afip.gov.ar/ws/services/LoginCms"),
            URI.create("https://servicios1.afip.gov.ar/wsfev1/service.asmx"),
            URI.create("https://aws.afip.gov.ar/sr-padron/webservices/personaServiceA4"),
            URI.create("https://servicios1.afip.gov.ar/wsfexv1/service.asmx"),
            URI.create("https://serviciosjava.afip.gob.ar/wsmtxca/services/MTXCAService"),
            URI.create("https://servicios1.afip.gov.ar/WSCDC/service.asmx"));

    private final URI wsaaUrl;
    private final URI wsfev1Url;
    private final URI registryUrl;
    private final URI wsfexv1Url;
    private final URI wsmtxcaUrl;
    private final URI wscdcUrl;

    ArcaEnvironment(URI wsaaUrl, URI wsfev1Url, URI registryUrl, URI wsfexv1Url, URI wsmtxcaUrl, URI wscdcUrl) {
        this.wsaaUrl = wsaaUrl;
        this.wsfev1Url = wsfev1Url;
        this.registryUrl = registryUrl;
        this.wsfexv1Url = wsfexv1Url;
        this.wsmtxcaUrl = wsmtxcaUrl;
        this.wscdcUrl = wscdcUrl;
    }

    /**
     * Returns the WSAA authentication service URL.
     *
     * @return URI of the LoginCms endpoint for this environment.
     */
    public URI getWsaaUrl() {
        return wsaaUrl;
    }

    /**
     * Returns the WSFEv1 electronic invoicing service URL.
     *
     * @return URI of the WSFEv1 endpoint for this environment.
     */
    public URI getWsfev1Url() {
        return wsfev1Url;
    }

    /**
     * Returns the wssrpadron_a4 taxpayer registry service URL.
     *
     * @return URI of the wssrpadron_a4 endpoint for this environment.
     */
    public URI getRegistryUrl() {
        return registryUrl;
    }

    /**
     * Returns the WSFEXv1 electronic export invoicing service URL.
     *
     * @return URI of the WSFEXv1 endpoint for this environment.
     */
    public URI getWsfexv1Url() {
        return wsfexv1Url;
    }

    /**
     * Returns the WSMTXCA itemized invoicing service URL.
     *
     * @return URI of the WSMTXCA endpoint for this environment.
     * @since 0.7.0
     */
    public URI getWsmtxcaUrl() {
        return wsmtxcaUrl;
    }

    /**
     * Returns the WSCDC voucher constatation service URL.
     *
     * @return URI of the WSCDC endpoint for this environment.
     * @since 0.9.0
     */
    public URI getWscdcUrl() {
        return wscdcUrl;
    }
}
