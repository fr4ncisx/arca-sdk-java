package consumer.reference;

import io.github.fr4ncisx.arca.client.spi.ArcaClient;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.spring.ArcaProperties;
import io.github.fr4ncisx.arca.wsfev1.spi.WsfeClient;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ByteArrayResource;

import java.time.Duration;
import java.util.Map;

/**
 * Exercises Spring wiring from a separately compiled application.
 *
 * @author fr4ncisx
 * @since 1.2.4
 */
public final class SpringConsumer {
    private SpringConsumer() {
    }

    /**
     * Builds the public facade through autoconfiguration without remote calls.
     *
     * @return whether Spring exposes a consistent facade and WSFE client.
     */
    public static boolean verify() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("reference",
                    Map.of("arca.cuit", "20-33333333-4", "arca.certificate-location", "dummy")));
            context.registerBean("arca-io.github.fr4ncisx.arca.spring.ArcaProperties", ArcaProperties.class,
                    () -> new ArcaProperties("20-33333333-4", ArcaEnvironment.HOMOLOGACION,
                            Duration.ofSeconds(10), Duration.ofSeconds(30), true,
                            new ByteArrayResource(new byte[]{1, 2, 3}), "dummy"));
            context.register(ConsumerConfiguration.class);
            context.refresh();
            return context.getBean(ArcaClient.class).wsfev1() == context.getBean(WsfeClient.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class ConsumerConfiguration {
    }
}
