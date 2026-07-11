package io.github.fr4ncisx.arca.registry.spi;

import io.github.fr4ncisx.arca.core.exception.ArcaAuthException;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.registry.model.TaxpayerData;

/**
 * Public client contract for interacting with the ARCA Taxpayer Registry Web Service (ws_sr_padron_a4).
 * <p>
 * This client provides methods to query taxpayer profiles and to verify service availability.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public interface RegistryClient {

    /**
     * Retrieves the registry profile of a taxpayer by their CUIT.
     *
     * @param cuit the target taxpayer CUIT to query
     * @return the taxpayer's profile data
     * @throws ArcaAuthException if authentication with WSAA fails
     * @throws ArcaValidationException if the CUIT format is invalid or the taxpayer is not found
     * @throws ArcaSoapException if a SOAP fault or network communication issue occurs
     */
    TaxpayerData getTaxpayer(Cuit cuit) throws ArcaAuthException, ArcaValidationException, ArcaSoapException;

    /**
     * Performs a fast connectivity check to evaluate the availability of the padron service.
     *
     * @return {@code true} if the service is online and responding, {@code false} otherwise
     */
    boolean ping();
}
