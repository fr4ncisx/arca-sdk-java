package io.github.fr4ncisx.arca.registry.internal.assembler;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.internal.adapter.ArcaSoapClient;
import io.github.fr4ncisx.arca.soap.internal.config.SoapConfig;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.registry.internal.client.DefaultRegistryClient;
import io.github.fr4ncisx.arca.registry.internal.generated.*;
import io.github.fr4ncisx.arca.registry.internal.usecase.GetTaxpayerUseCase;
import io.github.fr4ncisx.arca.registry.spi.RegistryClient;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;

/**
 * Assembler to construct and wire internal dependencies of the Registry client.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public final class RegistryClientAssembler {

    private RegistryClientAssembler() {
    }

    /**
     * Assembles a Registry client using official environment endpoints.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @return the fully wired RegistryClient instance
     */
    public static RegistryClient assemble(ArcaConfig config, AuthProvider authProvider) {
        if (config == null) {
            throw new ArcaValidationException("config must not be null");
        }
        return assemble(config, authProvider, config.environment().getRegistryUrl().toString());
    }

    /**
     * Assembles a Registry client using a custom endpoint URL.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param endpointUrl  the target Registry SOAP endpoint URL
     * @return the fully wired RegistryClient instance
     */
    public static RegistryClient assemble(ArcaConfig config, AuthProvider authProvider, String endpointUrl) {
        if (config == null) {
            throw new ArcaValidationException("config must not be null");
        }
        if (authProvider == null) {
            throw new ArcaValidationException("authProvider must not be null");
        }
        if (endpointUrl == null || endpointUrl.trim().isEmpty()) {
            throw new ArcaValidationException("endpointUrl must not be null or blank");
        }

        SoapConfig soapConfig = SoapConfig.from(config);
        PersonaServiceA4 portObj = new PersonaServiceA4_Service().getPersonaServiceA4Port();

        BindingProvider bp = (BindingProvider) portObj;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

        ArcaSoapPort<GetPersona, PersonaReturn> getTaxpayerSoapPort = new ArcaSoapClient<>(
            bp,
            req -> {
                try {
                    return portObj.getPersona(
                        req.getToken(),
                        req.getSign(),
                        req.getCuitRepresentada(),
                        req.getIdPersona()
                    );
                } catch (SRValidationException_Exception e) {
                    throw new WebServiceException(e);
                }
            },
            soapConfig
        );

        GetTaxpayerUseCase getTaxpayerUseCase = new GetTaxpayerUseCase(config, authProvider, getTaxpayerSoapPort);

        return new DefaultRegistryClient(config, getTaxpayerUseCase, portObj);
    }
}
