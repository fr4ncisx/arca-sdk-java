package io.github.fr4ncisx.arca.wsaa.internal.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates the raw WSAA HTTP transport behavior.
 *
 * @author fr4ncisx
 * @since 1.2.4
 */
class RawSoapHttpClientTest {

    private static final String ENDPOINT_PATH = "/ws/services/LoginCms";
    private WireMockServer mockServer;

    /**
     * Starts an isolated local SOAP endpoint for transport tests.
     */
    @BeforeEach
    void setUp() {
        mockServer = new WireMockServer(options().dynamicPort());
        mockServer.start();
    }

    /**
     * Stops the local SOAP endpoint after each test.
     */
    @AfterEach
    void tearDown() {
        mockServer.stop();
    }

    /**
     * Verifies that zero timeouts disable their respective limits.
     */
    @Test
    void acceptsZeroTimeouts() throws Exception {
        mockServer.stubFor(post(urlPathEqualTo(ENDPOINT_PATH))
                .willReturn(aResponse().withStatus(200).withFixedDelay(200).withBody("<response/>")));
        RawSoapHttpClient client = new RawSoapHttpClient(
                URI.create(mockServer.baseUrl() + ENDPOINT_PATH), Duration.ZERO, Duration.ZERO);

        assertThat(client.invoke("QUJD")).isEqualTo("<response/>");
    }

    /**
     * Verifies that WSAA responses above the configured memory limit are rejected.
     */
    @Test
    void rejectsResponseBodiesAboveOneMegabyte() {
        mockServer.stubFor(post(urlPathEqualTo(ENDPOINT_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("x".repeat(1_048_577))
                        .withChunkedDribbleDelay(10, 10)));
        RawSoapHttpClient client = new RawSoapHttpClient(
                URI.create(mockServer.baseUrl() + ENDPOINT_PATH),
                Duration.ofSeconds(1), Duration.ofSeconds(1));

        assertThatThrownBy(() -> client.invoke("QUJD"))
                .isInstanceOf(ArcaSoapException.class)
                .hasMessageContaining("maximum response size");
    }

    /**
     * Accepts a fragmented response exactly at the configured byte limit.
     *
     * @throws Exception if the local transport fails.
     */
    @Test
    void acceptsResponseAtOneMegabyteLimit() throws Exception {
        String body = "x".repeat(1_048_576);
        mockServer.stubFor(post(urlPathEqualTo(ENDPOINT_PATH))
                .willReturn(aResponse().withStatus(200).withBody(body).withChunkedDribbleDelay(10, 10)));
        RawSoapHttpClient client = new RawSoapHttpClient(
                URI.create(mockServer.baseUrl() + ENDPOINT_PATH), Duration.ofSeconds(2), Duration.ofSeconds(2));

        assertThat(client.invoke("QUJD")).isEqualTo(body);
    }
}
