package io.github.fr4ncisx.arca.client.spi;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.wsaa.spi.CertificateSource;
import io.github.fr4ncisx.arca.wsfev1.spi.WsfeClient;
import io.github.fr4ncisx.arca.wsfexv1.spi.WsfexClient;
import io.github.fr4ncisx.arca.wsmtxca.spi.WsmtxcaClient;
import io.github.fr4ncisx.arca.wscdc.spi.WscdcClient;
import io.github.fr4ncisx.arca.registry.spi.RegistryClient;
import io.github.fr4ncisx.arca.client.internal.client.DefaultArcaClient;

/**
 * Unified public entry point for the ARCA SDK.
 * <p>
 * This class orchestrates access to the different functional clients (WSFEv1, Registry, WSFEXv1, WSMTXCA)
 * and centralizes basic configuration and authentication.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public final class ArcaClient {

    private final WsfeClient wsfeClient;
    private final RegistryClient registryClient;
    private final WsfexClient wsfexClient;
    private final WsmtxcaClient wsmtxcaClient;
    private final WscdcClient wscdcClient;

    /**
     * Package-private constructor to build a client instance.
     *
     * @param wsfeClient     the wired electronic billing client
     * @param registryClient the wired registry lookup client
     * @param wsfexClient    the wired export billing client
     * @param wsmtxcaClient   the wired itemized billing client
     * @param wscdcClient     the wired constatation client
     */
    public ArcaClient(WsfeClient wsfeClient, RegistryClient registryClient, WsfexClient wsfexClient, WsmtxcaClient wsmtxcaClient, WscdcClient wscdcClient) {
        if (wsfeClient == null) {
            throw new ArcaValidationException("wsfeClient must not be null");
        }
        if (registryClient == null) {
            throw new ArcaValidationException("registryClient must not be null");
        }
        if (wsfexClient == null) {
            throw new ArcaValidationException("wsfexClient must not be null");
        }
        if (wsmtxcaClient == null) {
            throw new ArcaValidationException("wsmtxcaClient must not be null");
        }
        if (wscdcClient == null) {
            throw new ArcaValidationException("wscdcClient must not be null");
        }
        this.wsfeClient = wsfeClient;
        this.registryClient = registryClient;
        this.wsfexClient = wsfexClient;
        this.wsmtxcaClient = wsmtxcaClient;
        this.wscdcClient = wscdcClient;
    }

    /**
     * Accesses the WSFEv1 electronic invoicing service client.
     *
     * @return the WsfeClient instance
     */
    public WsfeClient wsfev1() {
        return wsfeClient;
    }

    /**
     * Accesses the Registry taxpayer lookup service client.
     *
     * @return the RegistryClient instance
     */
    public RegistryClient registry() {
        return registryClient;
    }

    /**
     * Accesses the WSFEXv1 export electronic invoicing service client.
     *
     * @return the WsfexClient instance
     */
    public WsfexClient wsfexv1() {
        return wsfexClient;
    }

    /**
     * Accesses the WSMTXCA itemized electronic invoicing service client.
     *
     * @return the WsmtxcaClient instance
     * @since 0.7.0
     */
    public WsmtxcaClient wsmtxca() {
        return wsmtxcaClient;
    }

    /**
     * Accesses the WSCDC voucher constatation service client.
     *
     * @return the WscdcClient instance
     * @since 0.9.0
     */
    public WscdcClient wscdc() {
        return wscdcClient;
    }

    /**
     * Performs a unified availability ping checking all child services.
     *
     * @return {@code true} if all sub-services are online, {@code false} otherwise
     */
    public boolean ping() {
        return wsfeClient.ping() && registryClient.ping() && wsfexClient.ping() && wsmtxcaClient.ping() && wscdcClient.ping();
    }

    /**
     * Creates a new builder instance.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing a unified ArcaClient.
     */
    public static final class Builder {

        private ArcaConfig config;
        private CertificateSource certificate;

        private Builder() {
        }

        /**
         * Configures the client with the specified SDK properties.
         *
         * @param config the configuration properties
         * @return this builder
         */
        public Builder config(ArcaConfig config) {
            this.config = config;
            return this;
        }

        /**
         * Configures the client with the specified certificate source.
         *
         * @param certificate the cryptographic certificate source
         * @return this builder
         */
        public Builder certificate(CertificateSource certificate) {
            this.certificate = certificate;
            return this;
        }

        /**
         * Builds a fully assembled ArcaClient.
         *
         * @return the assembled ArcaClient
         * @throws ArcaValidationException if configuration or certificate source is missing
         */
        public ArcaClient build() {
            if (config == null) {
                throw new ArcaValidationException("config must not be null");
            }
            if (certificate == null) {
                throw new ArcaValidationException("certificate must not be null");
            }
            return DefaultArcaClient.create(config, certificate);
        }
    }
}
