package io.github.fr4ncisx.arca.soap.internal.security;

/**
 * Utility class for hardening JAX-WS and XML parsing against XXE vulnerabilities.
 *
 * @author fr4ncisx
 * @since 1.1.0
 */
public final class SoapHardeningUtil {

    private SoapHardeningUtil() {
    }

    /**
     * Applies JVM-wide safety restrictions for XML parsing to mitigate XXE vulnerabilities.
     */
    public static void apply() {
        System.setProperty("javax.xml.accessExternalDTD", "");
        System.setProperty("javax.xml.accessExternalSchema", "");
        System.setProperty("javax.xml.accessExternalStylesheet", "");
    }
}
