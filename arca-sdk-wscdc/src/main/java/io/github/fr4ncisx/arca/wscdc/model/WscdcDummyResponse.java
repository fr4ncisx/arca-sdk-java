package io.github.fr4ncisx.arca.wscdc.model;

import java.util.Objects;

/**
 * Represents the health status response from the WSCDC dummy operation.
 *
 * @param appServer status of the application server
 * @param dbServer status of the database server
 * @param authServer status of the authentication server
 * @author fr4ncisx
 * @since 0.9.0
 */
public record WscdcDummyResponse(
    String appServer,
    String dbServer,
    String authServer
) {
    /**
     * Compact constructor to validate required parameters.
     */
    public WscdcDummyResponse {
        Objects.requireNonNull(appServer, "appServer must not be null");
        Objects.requireNonNull(dbServer, "dbServer must not be null");
        Objects.requireNonNull(authServer, "authServer must not be null");
    }

    /**
     * Checks if all servers are online ("OK").
     *
     * @return true if all servers are online, false otherwise
     */
    public boolean isOk() {
        return "OK".equalsIgnoreCase(appServer)
            && "OK".equalsIgnoreCase(dbServer)
            && "OK".equalsIgnoreCase(authServer);
    }
}
