package io.github.fr4ncisx.arca.soap.internal.security;

/**
 * Utility class for hardening JAX-WS and XML parsing against XXE vulnerabilities.
 * <p>
 * Invocation is strictly opt-in: the SDK no longer applies these JVM-wide
 * properties automatically, because mutating global system properties affects
 * every XML consumer in the host application. Applications that want this
 * additional layer must call {@link #apply()} explicitly during bootstrap.
 * <p>
 * Note that system properties only affect parser factories created after
 * invocation; the SDK relies on per-factory hardening for its own parsing.
 *
 * @author fr4ncisx
 * @since 1.1.0
 */
public final class SoapHardeningUtil {

    private SoapHardeningUtil() {
    }

    /**
     * Applies JVM-wide safety restrictions for XML parsing to mitigate XXE vulnerabilities.
     * <p>
     * Use with caution: these system properties are global to the JVM and may
     * affect unrelated components that legitimately resolve external schemas.
     */
    public static void apply() {
        System.setProperty("javax.xml.accessExternalDTD", "");
        System.setProperty("javax.xml.accessExternalSchema", "");
        System.setProperty("javax.xml.accessExternalStylesheet", "");
    }
}
