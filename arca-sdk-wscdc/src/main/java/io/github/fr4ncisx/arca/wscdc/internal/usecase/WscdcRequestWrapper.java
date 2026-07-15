package io.github.fr4ncisx.arca.wscdc.internal.usecase;

import io.github.fr4ncisx.arca.wscdc.internal.generated.CmpAuthRequest;
import io.github.fr4ncisx.arca.wscdc.internal.generated.CmpDatos;

/**
 * Technical wrapper to bundle auth and request parameters for WSCDC.
 *
 * @author fr4ncisx
 * @since 0.9.0
 */
public record WscdcRequestWrapper(
    CmpAuthRequest auth,
    CmpDatos cmpReq
) {}
