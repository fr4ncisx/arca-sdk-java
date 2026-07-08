package io.github.fr4ncisx.arca.wsfev1.spi;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests to enforce that the public SPI contract {@link WsfeClient} does not
 * leak internal generated stubs, JAXB classes, or SOAP infrastructure to consumers.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
class WsfeClientPublicContractTest {

    @Test
    void publicInterfaceSignaturesAreDecoupledFromInternalTypes() {
        Class<?> clientClass = WsfeClient.class;

        for (Method method : clientClass.getDeclaredMethods()) {
            Class<?> returnType = method.getReturnType();
            assertIsPublicDomainType(returnType, "Return type of method " + method.getName());

            for (Parameter param : method.getParameters()) {
                assertIsPublicDomainType(param.getType(), "Parameter " + param.getName() + " of method " + method.getName());
            }
        }
    }

    private void assertIsPublicDomainType(Class<?> clazz, String context) {
        if (clazz.isPrimitive()) {
            return;
        }

        String packageName = clazz.getPackageName();

        assertThat(packageName)
                .withFailMessage(context + " (" + clazz.getName() + ") must not leak JAX-WS/JAXB bindings")
                .doesNotStartWith("jakarta.xml")
                .doesNotStartWith("javax.xml");

        assertThat(packageName)
                .withFailMessage(context + " (" + clazz.getName() + ") must not leak internal packages")
                .doesNotContain(".internal");
    }
}
