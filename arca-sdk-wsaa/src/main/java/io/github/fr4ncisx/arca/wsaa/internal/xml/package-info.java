/**
 * Hardened XML parsing infrastructure for the WSAA authentication flow.
 * <p>
 * All DOM parsers created within this package neutralize XXE attack vectors
 * by disallowing DOCTYPE declarations, external entities, parameter entities,
 * and XInclude processing.
 *
 * @author fr4ncisx
 * @since 1.3.0
 */
@NullMarked
package io.github.fr4ncisx.arca.wsaa.internal.xml;

import org.jspecify.annotations.NullMarked;
