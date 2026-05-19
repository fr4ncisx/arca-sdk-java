package io.github.fr4ncisx.arca.soap.spi;

import io.github.fr4ncisx.arca.core.exception.ArcaSoapException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ArcaSoapPort}.
 * <p>
 * These tests verify that the SOAP port is a small, generic, and
 * framework-independent contract. They cover lambda usage, manual test-double
 * usage, compilation against the public contract, contractual exception
 * declaration, and public API isolation from Metro, JAX-WS, JAXB, and SOAP
 * runtime types.
 *
 * @author fr4ncisx
 * @since 0.1.0-M2
 */
class ArcaSoapPortTest {

    private static final TestRequest REQUEST = new TestRequest("LoginCms");
    private static final TestResponse RESPONSE = new TestResponse("ok");
    private static final ArcaSoapException SOAP_EXCEPTION =
            new ArcaSoapException("SOAP invocation failed");

    private static final Set<String> FORBIDDEN_FRAMEWORK_PACKAGES = Set.of(
            "javax.xml.ws",
            "jakarta.xml.ws",
            "javax.xml.soap",
            "jakarta.xml.soap",
            "javax.xml.bind",
            "jakarta.xml.bind",
            "com.sun.xml.ws",
            "org.glassfish.metro"
    );

    /**
     * Verifies that {@link ArcaSoapPort} is declared as a Java interface.
     */
    @Test
    void shouldBeAnInterface() {
        assertThat(ArcaSoapPort.class).isInterface();
    }

    /**
     * Verifies that {@link ArcaSoapPort} is explicitly marked as a functional
     * interface.
     */
    @Test
    void shouldBeAnnotatedAsFunctionalInterface() {
        assertThat(ArcaSoapPort.class)
                .hasAnnotation(FunctionalInterface.class);
    }

    /**
     * Verifies that {@link ArcaSoapPort} can be implemented as a lambda because
     * it is a functional interface with a single request/response operation.
     *
     * @throws ArcaSoapException if the test port invocation fails.
     */
    @Test
    void shouldInvokeSoapPortImplementedAsLambda() throws ArcaSoapException {
        ArcaSoapPort<TestRequest, TestResponse> port = request ->
                new TestResponse("processed-" + request.operation());

        TestResponse response = port.invoke(REQUEST);

        assertThat(response.result()).isEqualTo("processed-LoginCms");
    }

    /**
     * Verifies that {@link ArcaSoapPort} can be replaced by a manual stub without
     * creating SOAP clients, generated classes, runtime infrastructure, or
     * framework-specific dependencies.
     *
     * @throws ArcaSoapException if the stub invocation fails.
     */
    @Test
    void shouldAllowManualTestDoubleWithoutSoapRuntimeInfrastructure()
            throws ArcaSoapException {
        RecordingSoapPortStub port = new RecordingSoapPortStub(RESPONSE);

        TestResponse response = port.invoke(REQUEST);

        assertThat(response).isEqualTo(RESPONSE);
        assertThat(port.invocations()).containsExactly(REQUEST);
    }

    /**
     * Verifies that client code can compile against {@link ArcaSoapPort} using
     * only SDK contract types and plain Java request/response payloads, without
     * depending on JAX-WS, Metro, JAXB, or SOAP runtime types in the public
     * signature.
     *
     * @throws ArcaSoapException if the test port invocation fails.
     */
    @Test
    void shouldCompileConsumerCodeAgainstFrameworkIndependentContract()
            throws ArcaSoapException {
        ContractConsumer consumer = new ContractConsumer(request ->
                new TestResponse("compiled-" + request.operation()));

        TestResponse response = consumer.execute(REQUEST);

        assertThat(response.result()).isEqualTo("compiled-LoginCms");
    }

    /**
     * Verifies that invocation failures are expressed through the SDK SOAP
     * contract exception instead of leaking framework-specific exceptions.
     */
    @Test
    void shouldPropagateArcaSoapExceptionAsContractError() {
        ArcaSoapPort<TestRequest, TestResponse> port = request -> throwSoapException();

        assertThatThrownBy(() -> port.invoke(REQUEST))
                .isSameAs(SOAP_EXCEPTION);
    }

    /**
     * Verifies that the public invocation method has the expected erased
     * request/response signature and declares {@link ArcaSoapException}.
     *
     * @throws NoSuchMethodException if the invoke method is not available with
     *                               the expected erased signature.
     */
    @Test
    void shouldExposeExpectedInvokeMethodSignature() throws NoSuchMethodException {
        Method invoke = ArcaSoapPort.class.getMethod("invoke", Object.class);

        assertThat(invoke.getName()).isEqualTo("invoke");
        assertThat(invoke.getParameterTypes()).containsExactly(Object.class);
        assertThat(invoke.getReturnType()).isEqualTo(Object.class);
        assertThat(invoke.getExceptionTypes()).containsExactly(ArcaSoapException.class);
    }

    /**
     * Verifies that the public API of {@link ArcaSoapPort} does not expose
     * Metro, JAX-WS, JAXB, or SOAP runtime types in its method parameters,
     * return type, or declared exceptions.
     *
     * @throws NoSuchMethodException if the invoke method is not available with
     *                               the expected erased signature.
     */
    @Test
    void shouldNotExposeConcreteSoapFrameworkTypesInPublicApi()
            throws NoSuchMethodException {
        Method invoke = ArcaSoapPort.class.getMethod("invoke", Object.class);

        List<String> publicApiTypeNames = List.of(
                typeNameOf(invoke.getGenericReturnType()),
                typeNameOf(invoke.getGenericParameterTypes()[0]),
                typeNameOf(invoke.getGenericExceptionTypes()[0])
        );

        assertThat(publicApiTypeNames)
                .noneMatch(ArcaSoapPortTest::startsWithForbiddenFrameworkPackage);
    }

    /**
     * Throws the configured SOAP exception from a single method invocation.
     * <p>
     * This keeps exception assertions focused on one invocation inside the
     * assertion lambda.
     *
     * @return never returns normally.
     * @throws ArcaSoapException always thrown as the contract error.
     */
    private static TestResponse throwSoapException() throws ArcaSoapException {
        throw SOAP_EXCEPTION;
    }

    /**
     * Returns the type name used to inspect the public API for forbidden
     * framework packages.
     *
     * @param type reflected type to inspect.
     * @return the fully qualified type name.
     */
    private static String typeNameOf(Type type) {
        return type.getTypeName();
    }

    /**
     * Checks whether a type name belongs to a forbidden SOAP framework package.
     *
     * @param typeName reflected type name.
     * @return true if the type belongs to a forbidden framework package.
     */
    private static boolean startsWithForbiddenFrameworkPackage(String typeName) {
        return FORBIDDEN_FRAMEWORK_PACKAGES.stream()
                .anyMatch(typeName::startsWith);
    }

    /**
     * Minimal consumer used to prove that application code can depend on the
     * SOAP port contract without referencing any concrete SOAP framework type.
     *
     * @param port framework-independent SOAP port.
     */
    private record ContractConsumer(ArcaSoapPort<TestRequest, TestResponse> port) {

        /**
         * Creates a consumer that depends only on the generic SOAP port
         * contract.
         *
         * @param port framework-independent SOAP port.
         */
        private ContractConsumer {
        }

        /**
         * Executes a request through the generic SOAP port.
         *
         * @param request request payload.
         * @return response payload.
         * @throws ArcaSoapException if the SOAP port invocation fails.
         */
        private TestResponse execute(TestRequest request) throws ArcaSoapException {
            return port.invoke(request);
        }
    }

    /**
     * Recording stub for {@link ArcaSoapPort}.
     * <p>
     * Records received requests and returns a predefined response. This allows
     * tests to verify invocation behavior without Mockito or SOAP runtime
     * infrastructure.
     */
    private static final class RecordingSoapPortStub
            implements ArcaSoapPort<TestRequest, TestResponse> {

        private final TestResponse response;
        private final List<TestRequest> invocations = new java.util.ArrayList<>();

        /**
         * Creates a recording port with a predefined response.
         *
         * @param response response returned by {@link #invoke(TestRequest)}.
         */
        private RecordingSoapPortStub(TestResponse response) {
            this.response = response;
        }

        /**
         * Records the received request and returns the predefined response.
         *
         * @param request operation request payload.
         * @return predefined response payload.
         */
        @Override
        public TestResponse invoke(TestRequest request) {
            invocations.add(request);
            return response;
        }

        /**
         * Returns the requests received by this test double.
         *
         * @return recorded invocation requests.
         */
        private List<TestRequest> invocations() {
            return invocations;
        }
    }

    /**
     * Test request payload used to verify the generic request side of the port.
     *
     * @param operation operation name carried by the test request.
     */
    private record TestRequest(String operation) {
    }

    /**
     * Test response payload used to verify the generic response side of the
     * port.
     *
     * @param result result value carried by the test response.
     */
    private record TestResponse(String result) {
    }
}