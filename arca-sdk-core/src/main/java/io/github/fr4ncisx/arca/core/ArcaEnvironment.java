package io.github.fr4ncisx.arca.core;

import java.net.URI;

/**
 * Ambientes oficiales de ARCA con los endpoints de WSAA y WSFEv1.
 * <p>
 * Cada constante expone las URLs completas de los servicios de autenticacion
 * (WSAA) y facturacion electronica (WSFEv1) mediante {@link #getWsaaUrl()}
 * y {@link #getWsfev1Url()}.
 *
 * @see <a href="https://wsaahomo.afip.gov.ar/ws/services/LoginCms?wsdl">WSDL WSAA Homologacion</a>
 * @see <a href="https://wsaa.afip.gov.ar/ws/services/LoginCms?wsdl">WSDL WSAA Produccion</a>
 * @see <a href="https://wswhomo.afip.gov.ar/wsfev1/service.asmx?wsdl">WSDL WSFEv1 Homologacion</a>
 * @see <a href="https://servicios1.afip.gov.ar/wsfev1/service.asmx?wsdl">WSDL WSFEv1 Produccion</a>
 */
public enum ArcaEnvironment {
    /**
     * Ambiente de homologacion (testing) de ARCA.
     * <p>
     * WSAA: {@code https://wsaahomo.afip.gov.ar/ws/services/LoginCms}
     * <p>
     * WSFEv1: {@code https://wswhomo.afip.gov.ar/wsfev1/service.asmx}
     */
    HOMOLOGACION(
            URI.create("https://wsaahomo.afip.gov.ar/ws/services/LoginCms"),
            URI.create("https://wswhomo.afip.gov.ar/wsfev1/service.asmx")),

    /**
     * Ambiente de produccion de ARCA.
     * <p>
     * WSAA: {@code https://wsaa.afip.gov.ar/ws/services/LoginCms}
     * <p>
     * WSFEv1: {@code https://servicios1.afip.gov.ar/wsfev1/service.asmx}
     */
    PRODUCCION(
            URI.create("https://wsaa.afip.gov.ar/ws/services/LoginCms"),
            URI.create("https://servicios1.afip.gov.ar/wsfev1/service.asmx"));

    private final URI wsaaUrl;
    private final URI wsfev1Url;

    ArcaEnvironment(URI wsaaUrl, URI wsfev1Url) {
        this.wsaaUrl = wsaaUrl;
        this.wsfev1Url = wsfev1Url;
    }

    /**
     * Retorna la URL del servicio WSAA (autenticacion).
     *
     * @return URI del endpoint LoginCms correspondiente al ambiente.
     */
    public URI getWsaaUrl() {
        return wsaaUrl;
    }

    /**
     * Retorna la URL del servicio WSFEv1 (facturacion electronica).
     *
     * @return URI del endpoint WSFEv1 correspondiente al ambiente.
     */
    public URI getWsfev1Url() {
        return wsfev1Url;
    }
}
