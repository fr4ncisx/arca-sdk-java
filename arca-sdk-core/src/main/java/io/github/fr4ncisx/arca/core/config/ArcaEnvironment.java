package io.github.fr4ncisx.arca.core.config;

import java.net.URI;

/**
 * Official ARCA environments with WSAA and WSFEv1 endpoints.
 * <p>
 * Each constant exposes the full URLs for the authentication
 * (WSAA) and electronic invoicing (WSFEv1) services.
 *
 * @see <a href="https://wsaahomo.afip.gov.ar/ws/services/LoginCms?wsdl">WSDL WSAA Homologacion</a>
 * @see <a href="https://wsaa.afip.gov.ar/ws/services/LoginCms?wsdl">WSDL WSAA Produccion</a>
 * @see <a href="https://wswhomo.afip.gov.ar/wsfev1/service.asmx?wsdl">WSDL WSFEv1 Homologacion</a>
 * @see <a href="https://servicios1.afip.gov.ar/wsfev1/service.asmx?wsdl">WSDL WSFEv1 Produccion</a>
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
     */
    HOMOLOGACION(
            URI.create("https://wsaahomo.afip.gov.ar/ws/services/LoginCms"),
            URI.create("https://wswhomo.afip.gov.ar/wsfev1/service.asmx"),
            URI.create("https://awshomo.afip.gov.ar/sr-padron/webservices/personaServiceA4")),

    /**
     * ARCA production environment.
     * <p>
     * WSAA: {@code https://wsaa.afip.gov.ar/ws/services/LoginCms}
     * <p>
     * WSFEv1: {@code https://servicios1.afip.gov.ar/wsfev1/service.asmx}
     */
    PRODUCCION(
            URI.create("https://wsaa.afip.gov.ar/ws/services/LoginCms"),
            URI.create("https://servicios1.afip.gov.ar/wsfev1/service.asmx"),
            URI.create("https://aws.afip.gov.ar/sr-padron/webservices/personaServiceA4"));

    private final URI wsaaUrl;
    private final URI wsfev1Url;
    private final URI registryUrl;

    ArcaEnvironment(URI wsaaUrl, URI wsfev1Url, URI registryUrl) {
        this.wsaaUrl = wsaaUrl;
        this.wsfev1Url = wsfev1Url;
        this.registryUrl = registryUrl;
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
}
