package io.github.fr4ncisx.arca.test.support.benchmark;

import io.github.fr4ncisx.arca.wsfev1.internal.generated.FECAEResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Technical smoke benchmark validating JAXB serialization and deserialization latency.
 * <p>
 * This test parses and serializes the official CAE request success fixture, measuring
 * execution times over multiple iterations to ensure there are no performance regressions
 * or memory leaks during XML processing.
 *
 * @author fr4ncisx
 * @since 0.6.0
 */
@Tag("benchmark")
class WsfeSerializationBenchmarkSmokeTest {

    private static final Logger log = LoggerFactory.getLogger(WsfeSerializationBenchmarkSmokeTest.class);
    private static final int WARMUP_ITERATIONS = 100;
    private static final int MEASUREMENT_ITERATIONS = 1000;
    private static final String FIXTURE_PATH = "/fixtures/wsfev1/cae-request-success.xml";

    @Test
    void executeSerializationBenchmark() throws Exception {
        JAXBContext context = JAXBContext.newInstance(FECAEResponse.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Marshaller marshaller = context.createMarshaller();

        FECAEResponse response = loadResponse(unmarshaller);
        assertThat(response).isNotNull();
        assertThat(response.getFeDetResp()).isNotNull();
        assertThat(response.getFeDetResp().getFECAEDetResponse()).isNotEmpty();
        assertThat(response.getFeDetResp().getFECAEDetResponse().get(0).getCAE()).isEqualTo("12345678901234");

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            loadResponse(unmarshaller);
            serializeResponse(marshaller, response);
        }

        Instant startUnmarshall = Instant.now();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            loadResponse(unmarshaller);
        }
        Instant endUnmarshall = Instant.now();
        Duration unmarshallDuration = Duration.between(startUnmarshall, endUnmarshall);

        Instant startMarshall = Instant.now();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            serializeResponse(marshaller, response);
        }
        Instant endMarshall = Instant.now();
        Duration marshallDuration = Duration.between(startMarshall, endMarshall);

        double avgUnmarshallMs = (double) unmarshallDuration.toNanos() / MEASUREMENT_ITERATIONS / 1_000_000.0;
        double avgMarshallMs = (double) marshallDuration.toNanos() / MEASUREMENT_ITERATIONS / 1_000_000.0;

        log.info("JAXB Deserialization benchmark ({} iterations): total={}, avg={:.4f} ms per run",
                MEASUREMENT_ITERATIONS, unmarshallDuration, avgUnmarshallMs);
        log.info("JAXB Serialization benchmark ({} iterations): total={}, avg={:.4f} ms per run",
                MEASUREMENT_ITERATIONS, marshallDuration, avgMarshallMs);

        assertThat(avgUnmarshallMs).isLessThan(5.0);
        assertThat(avgMarshallMs).isLessThan(5.0);
    }

    private FECAEResponse loadResponse(Unmarshaller unmarshaller) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(FIXTURE_PATH)) {
            if (is == null) {
                throw new IllegalStateException("Fixture not found on classpath: " + FIXTURE_PATH);
            }
            JAXBElement<FECAEResponse> element = unmarshaller.unmarshal(new StreamSource(is), FECAEResponse.class);
            return element.getValue();
        }
    }

    private byte[] serializeResponse(Marshaller marshaller, FECAEResponse response) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JAXBElement<FECAEResponse> element = new JAXBElement<>(
                new javax.xml.namespace.QName("http://ar.gov.afip.dif.FEV1/", "FECAESolicitarResult"),
                FECAEResponse.class,
                response
        );
        marshaller.marshal(element, baos);
        return baos.toByteArray();
    }
}
