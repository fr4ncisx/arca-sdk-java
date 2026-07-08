package io.github.fr4ncisx.arca.wsaa.internal.certificate;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.wsaa.spi.CertificateSource;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the appropriate {@link CertificateSource} for a given taxpayer CUIT.
 * <p>
 * This internal component scans registered certificate sources, inspects their loaded
 * X.509 certificates, and extracts their associated CUIT from the Subject DN using
 * standard attributes (serialNumber or OID 2.5.4.5).
 * <p>
 * It enforces deterministic resolution: if no candidate matches the target CUIT or
 * if multiple candidates match (causing ambiguity), it rejects the selection.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public final class CertificateSourceSelector {

    private static final Pattern CUIT_PATTERN = Pattern.compile(
            "([0-9]{2}-?[0-9]{8}-?[0-9]|[0-9]{11})"
    );

    private final List<CertificateSource> candidates;

    /**
     * Creates a new selector with the registered certificate source candidates.
     *
     * @param candidates the collection of certificate sources to select from
     * @throws ArcaValidationException if candidates collection is null
     */
    public CertificateSourceSelector(Collection<CertificateSource> candidates) {
        if (candidates == null) {
            throw new ArcaValidationException("candidates list must not be null");
        }
        this.candidates = List.copyOf(candidates);
    }

    /**
     * Resolves a single matching certificate source for the supplied configuration.
     *
     * @param config the SDK configuration containing the target taxpayer CUIT
     * @return the resolved certificate source
     * @throws ArcaValidationException if config is null, no certificate matches the CUIT,
     *                                 or multiple matching certificates are found
     */
    public CertificateSource select(ArcaConfig config) {
        if (config == null) {
            throw new ArcaValidationException("config must not be null");
        }
        Cuit targetCuit = config.cuit();
        List<CertificateSource> matches = new ArrayList<>();

        for (CertificateSource source : candidates) {
            if (source == null) {
                continue;
            }
            try {
                KeyStore keyStore = source.load();
                Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    if (keyStore.isKeyEntry(alias)) {
                        X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                        if (cert != null) {
                            Cuit certCuit = extractCuit(cert);
                            if (certCuit != null && certCuit.equals(targetCuit)) {
                                matches.add(source);
                                break; // Found a matching key entry in this keystore
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // If a source cannot be loaded or is invalid, it is skipped as a valid candidate.
            }
        }

        if (matches.isEmpty()) {
            throw new ArcaValidationException("No registered CertificateSource matches target CUIT: " + targetCuit);
        }
        if (matches.size() > 1) {
            throw new ArcaValidationException("Ambiguity detected: multiple compatible CertificateSources found for CUIT: " + targetCuit);
        }

        return matches.get(0);
    }

    private Cuit extractCuit(X509Certificate cert) {
        try {
            X500Name x500Name = X500Name.getInstance(cert.getSubjectX500Principal().getEncoded());
            RDN[] rdns = x500Name.getRDNs(BCStyle.SERIALNUMBER);
            if (rdns.length > 0) {
                String serialValue = IETFUtils.valueToString(rdns[0].getFirst().getValue());
                if (serialValue != null) {
                    Matcher matcher = CUIT_PATTERN.matcher(serialValue);
                    if (matcher.find()) {
                        return Cuit.parse(matcher.group(1));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors for this certificate candidate
        }
        return null;
    }
}
