package io.github.fr4ncisx.arca.wsaa.internal.cms;

import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsaa.spi.CertificateSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;

/**
 * Signs XML messages using PKCS#7 (CMS) SignedData.
 * <p>
 * This class provides the cryptographic engine for signing Ticket de Requerimiento
 * de Acceso (TRA) payloads required by the ARCA WSAA service. It relies on the
 * BouncyCastle Provider for PKCS#7 / CMS SignedData generation.
 *
 * @author fr4ncisx
 * @since 0.1.0-M4
 */
public final class CmsSigner {

    static {
        if (Security.getProvider("BC") == null)
            Security.addProvider(new BouncyCastleProvider());
    }

    private final CertificateSource certificateSource;
    private final char[] keyPassword;

    /**
     * Creates a new CMS signer with the given certificate source and key password.
     *
     * @param certificateSource the source of the cryptographic certificate and private key.
     * @param keyPassword the password for the private key entry in the keystore.
     */
    public CmsSigner(CertificateSource certificateSource, char[] keyPassword) {
        if (certificateSource == null)
            throw new ArcaValidationException("certificateSource must not be null");
        if (keyPassword == null)
            throw new ArcaValidationException("keyPassword must not be null");
        this.certificateSource = certificateSource;
        this.keyPassword = keyPassword;
    }

    /**
     * Signs the given XML string using PKCS#7 (CMS) SignedData.
     * <p>
     * The generated signature embeds the original XML payload (attached data) and is
     * Base64 encoded, matching the exact structure expected by the ARCA WSAA service.
     *
     * @param xml the XML string payload to sign.
     * @return the Base64-encoded CMS SignedData signature.
     * @throws ArcaAuthException if the signing process fails due to missing keys, invalid
     *                           certificates, or cryptographic errors.
     */
    public String sign(String xml) {
        if (xml == null)
            throw new ArcaAuthException("The XML payload to sign cannot be null.");
        try {
            KeyStore keyStore = certificateSource.load();
            String alias = null;
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String a = aliases.nextElement();
                if (keyStore.isKeyEntry(a)) {
                    alias = a;
                    break;
                }
            }
            if (alias == null)
                throw new ArcaAuthException("No private key entry found in the provided KeyStore.");

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, keyPassword);
            if (privateKey == null)
                throw new ArcaAuthException("Failed to retrieve the private key using alias '" + alias + "'.");

            Certificate[] chain = keyStore.getCertificateChain(alias);
            if (chain == null || chain.length == 0)
                throw new ArcaAuthException("No certificate chain associated with alias '" + alias + "'.");

            X509Certificate signerCert = (X509Certificate) chain[0];

            List<X509Certificate> certList = new ArrayList<>();
            for (Certificate cert : chain)
                certList.add((X509Certificate) cert);
            Store<?> certStore = new JcaCertStore(certList);

            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();

            ContentSigner sha256Signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider("BC")
                    .build(privateKey);

            generator.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                            new JcaDigestCalculatorProviderBuilder().setProvider("BC").build()
                    ).build(sha256Signer, signerCert)
            );

            generator.addCertificates(certStore);

            CMSTypedData inputData = new CMSProcessableByteArray(xml.getBytes(StandardCharsets.UTF_8));
            CMSSignedData signedData = generator.generate(inputData, true);

            return Base64.getEncoder().encodeToString(signedData.getEncoded());
        } catch (ArcaAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new ArcaAuthException("Failed to generate PKCS#7 (CMS) signature: " + e.getMessage(), e);
        }
    }
}
