package io.github.fr4ncisx.arca.wsaa.internal.client;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

/**
 * JAX-WS Web Service interface representing the WSAA LoginCMS endpoint.
 * <p>
 * This interface maps the SOAP 1.1 RPC/Literal (or RPC/Encoded tolerant) operation
 * required by the ARCA WSAA service to authenticate clients using a CMS signature.
 *
 * @author fr4ncisx
 * @since 0.1.0-M4
 */
@WebService(
    name = "LoginCMS",
    targetNamespace = "http://wsaa.view.sua.dvadac.desein.afip.gov",
    portName = "LoginCms",
    serviceName = "LoginCMSService"
)
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface LoginCmsWebService {

    /**
     * Authenticates a client against WSAA using a PKCS#7 (CMS) SignedData payload.
     *
     * @param in0 the Base64-encoded CMS signature.
     * @return a XML string containing the login ticket response (token and sign).
     */
    @WebMethod(operationName = "loginCms")
    @WebResult(name = "loginCmsReturn")
    String loginCms(
        @WebParam(name = "in0") String in0
    );
}
