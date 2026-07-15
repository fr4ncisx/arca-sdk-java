package io.github.fr4ncisx.arca.client.internal.client;

import io.github.fr4ncisx.arca.client.spi.ArcaClient;
import io.github.fr4ncisx.arca.core.clock.SystemClock;
import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.internal.auth.DefaultAuthProvider;
import io.github.fr4ncisx.arca.wsaa.internal.cache.InMemoryTicketCache;
import io.github.fr4ncisx.arca.wsaa.internal.client.LoginCmsClient;
import io.github.fr4ncisx.arca.wsaa.internal.cms.CmsSigner;
import io.github.fr4ncisx.arca.wsaa.internal.tra.TraGenerator;
import io.github.fr4ncisx.arca.wsaa.spi.CertificateSource;
import io.github.fr4ncisx.arca.wsaa.spi.Pkcs12CertificateSource;
import io.github.fr4ncisx.arca.wsfev1.internal.assembler.WsfeClientAssembler;
import io.github.fr4ncisx.arca.wsfev1.spi.WsfeClient;
import io.github.fr4ncisx.arca.wsfexv1.internal.assembler.WsfexClientAssembler;
import io.github.fr4ncisx.arca.wsfexv1.spi.WsfexClient;
import io.github.fr4ncisx.arca.wsmtxca.internal.assembler.WsmtxcaClientAssembler;
import io.github.fr4ncisx.arca.wsmtxca.spi.WsmtxcaClient;
import io.github.fr4ncisx.arca.registry.internal.assembler.RegistryClientAssembler;
import io.github.fr4ncisx.arca.registry.spi.RegistryClient;

/**
 * Factory class responsible for wiring and instantiating the unified {@link ArcaClient}.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public final class DefaultArcaClient {

    private DefaultArcaClient() {
    }

    /**
     * Wires all internal components and constructs the unified ArcaClient.
     *
     * @param config      the SDK configuration
     * @param certificate the certificate source
     * @return the wired ArcaClient
     */
    public static ArcaClient create(ArcaConfig config, CertificateSource certificate) {
        char[] password = new char[0];
        if (certificate instanceof Pkcs12CertificateSource p12) {
            password = p12.getPassword();
        }

        TraGenerator traGenerator = new TraGenerator(SystemClock.INSTANCE);
        CmsSigner cmsSigner = new CmsSigner(certificate, password);
        LoginCmsClient loginCmsClient = new LoginCmsClient(config, config.environment().getWsaaUrl().toString());
        InMemoryTicketCache ticketCache = new InMemoryTicketCache(SystemClock.INSTANCE);

        AuthProvider authProvider = new DefaultAuthProvider(ticketCache, traGenerator, cmsSigner, loginCmsClient);

        WsfeClient wsfeClient = WsfeClientAssembler.assemble(config, authProvider);
        RegistryClient registryClient = RegistryClientAssembler.assemble(config, authProvider);
        WsfexClient wsfexClient = WsfexClientAssembler.assemble(config, authProvider);
        WsmtxcaClient wsmtxcaClient = WsmtxcaClientAssembler.assemble(config, authProvider);

        return new ArcaClient(wsfeClient, registryClient, wsfexClient, wsmtxcaClient);
    }
}
