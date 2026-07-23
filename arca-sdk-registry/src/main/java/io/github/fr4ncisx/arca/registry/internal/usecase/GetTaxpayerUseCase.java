package io.github.fr4ncisx.arca.registry.internal.usecase;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.registry.internal.adapter.RegistryMapper;
import io.github.fr4ncisx.arca.registry.internal.generated.GetPersona;
import io.github.fr4ncisx.arca.registry.internal.generated.PersonaReturn;
import io.github.fr4ncisx.arca.registry.internal.generated.SRValidationException_Exception;
import io.github.fr4ncisx.arca.registry.model.TaxpayerData;
import jakarta.xml.ws.WebServiceException;

/**
 * Usecase to retrieve taxpayer data from the ARCA registry.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public final class GetTaxpayerUseCase {

    private final ArcaConfig config;
    private final AuthProvider authProvider;
    private final ArcaSoapPort<GetPersona, PersonaReturn> soapPort;

    /**
     * Creates a new use case instance.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param soapPort     the SOAP port adapter
     */
    public GetTaxpayerUseCase(
            ArcaConfig config,
            AuthProvider authProvider,
            ArcaSoapPort<GetPersona, PersonaReturn> soapPort) {
        if (config == null) {
            throw new ArcaValidationException("config must not be null");
        }
        if (authProvider == null) {
            throw new ArcaValidationException("authProvider must not be null");
        }
        if (soapPort == null) {
            throw new ArcaValidationException("soapPort must not be null");
        }
        this.config = config;
        this.authProvider = authProvider;
        this.soapPort = soapPort;
    }

    /**
     * Executes the use case to query the taxpayer registry.
     *
     * @param cuit the taxpayer CUIT to query
     * @return the taxpayer's profile data
     * @throws ArcaAuthException if authentication with WSAA fails
     * @throws ArcaValidationException if the taxpayer is not found or CUIT is invalid
     * @throws ArcaSoapException if the remote invocation fails
     */
    public TaxpayerData execute(Cuit cuit) throws ArcaAuthException, ArcaValidationException, ArcaSoapException {
        if (cuit == null) {
            throw new ArcaValidationException("cuit must not be null");
        }

        ArcaAccessTicket ticket = authProvider.authenticate("ws_sr_padron_a4");

        GetPersona request = new GetPersona();
        request.setToken(ticket.token());
        request.setSign(ticket.sign());
        request.setCuitRepresentada(config.cuit().number());
        request.setIdPersona(cuit.number());

        try {
            PersonaReturn response = soapPort.invoke(request);
            if (response == null || response.getPersona() == null) {
                throw new ArcaValidationException("Taxpayer with CUIT " + cuit.toString() + " not found");
            }
            return RegistryMapper.toDomain(response);
        } catch (ArcaSoapException e) {
            Throwable cause = e.getCause();
            if (cause instanceof WebServiceException && cause.getCause() instanceof SRValidationException_Exception) {
                throw new ArcaValidationException("Taxpayer validation failed: " + cause.getCause().getMessage(), cause.getCause());
            }
            if (cause instanceof SRValidationException_Exception) {
                throw new ArcaValidationException("Taxpayer validation failed: " + cause.getMessage(), cause);
            }
            throw e;
        }
    }
}
