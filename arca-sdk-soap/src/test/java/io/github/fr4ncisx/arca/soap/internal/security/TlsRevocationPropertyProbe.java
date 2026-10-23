package io.github.fr4ncisx.arca.soap.internal.security;

import java.security.Security;

/**
 * Isolated process probe for JVM-wide TLS revocation properties.
 *
 * @author fr4ncisx
 * @since 1.2.4
 */
public final class TlsRevocationPropertyProbe {

    private TlsRevocationPropertyProbe() {
    }

    /**
     * Enables TLS revocation checking and prints the resulting property values.
     *
     * @param args command-line arguments, which are ignored.
     */
    public static void main(String[] args) {
        SoapHardeningUtil.enableTlsRevocationChecking();
        System.out.print(System.getProperty("com.sun.net.ssl.checkRevocation"));
        System.out.print('|');
        System.out.print(System.getProperty("com.sun.security.enableCRLDP"));
        System.out.print('|');
        System.out.print(Security.getProperty("ocsp.enable"));
    }
}
