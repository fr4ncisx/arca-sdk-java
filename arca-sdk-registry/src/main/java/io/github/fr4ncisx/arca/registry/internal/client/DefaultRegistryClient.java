package io.github.fr4ncisx.arca.registry.internal.client;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.registry.internal.generated.DummyReturn;
import io.github.fr4ncisx.arca.registry.internal.generated.PersonaServiceA4;
import io.github.fr4ncisx.arca.registry.internal.usecase.GetTaxpayerUseCase;
import io.github.fr4ncisx.arca.registry.model.TaxpayerData;
import io.github.fr4ncisx.arca.registry.spi.RegistryClient;
import jakarta.xml.ws.BindingProvider;
import com.sun.xml.ws.developer.JAXWSProperties;

/**
 * Default implementation of the {@link RegistryClient} interface.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public final class DefaultRegistryClient implements RegistryClient {

    private final ArcaConfig config;
    private final GetTaxpayerUseCase getTaxpayerUseCase;
    private final PersonaServiceA4 port;

    /**
     * Creates a new client instance.
     *
     * @param config             the SDK configuration
     * @param getTaxpayerUseCase the usecase for taxpayer retrieval
     * @param port               the JAX-WS Web Service port
     */
    public DefaultRegistryClient(
            ArcaConfig config,
            GetTaxpayerUseCase getTaxpayerUseCase,
            PersonaServiceA4 port) {
        if (config == null) {
            throw new ArcaValidationException("config must not be null");
        }
        if (getTaxpayerUseCase == null) {
            throw new ArcaValidationException("getTaxpayerUseCase must not be null");
        }
        if (port == null) {
            throw new ArcaValidationException("port must not be null");
        }
        this.config = config;
        this.getTaxpayerUseCase = getTaxpayerUseCase;
        this.port = port;
    }

    @Override
    public TaxpayerData getTaxpayer(Cuit cuit) throws ArcaAuthException, ArcaValidationException, ArcaSoapException {
        return getTaxpayerUseCase.execute(cuit);
    }

    @Override
    @SuppressWarnings("null")
    public boolean ping() {
        try {
            BindingProvider bp = (BindingProvider) port;
            var context = bp.getRequestContext();
            context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, config.environment().getRegistryUrl().toString());

            long timeoutMillis = config.readTimeout().toMillis();
            int intTimeout = (int) Math.min(timeoutMillis, Integer.MAX_VALUE);
            context.put(JAXWSProperties.CONNECT_TIMEOUT, intTimeout);
            context.put(JAXWSProperties.REQUEST_TIMEOUT, intTimeout);

            DummyReturn response = port.dummy();
            return response != null
                    && response.getAppserver() != null
                    && response.getDbserver() != null
                    && response.getAuthserver() != null;
        } catch (Throwable t) {
            return false;
        }
    }
}
