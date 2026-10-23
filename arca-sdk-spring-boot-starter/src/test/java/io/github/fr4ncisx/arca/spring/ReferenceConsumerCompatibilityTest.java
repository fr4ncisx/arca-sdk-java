package io.github.fr4ncisx.arca.spring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compiles independent consumers and links their bytecode to the candidate SDK.
 *
 * @author fr4ncisx
 * @since 1.2.4
 */
class ReferenceConsumerCompatibilityTest {
    @TempDir
    Path temporary;

    /**
     * Verifies consumer source compatibility and current runtime behavior.
     *
     * @throws Exception if compilation or consumer execution fails.
     */
    @Test
    void compilesAndRunsConsumersAgainstCandidate() throws Exception {
        compileAndRun(System.getProperty("java.class.path"));
    }

    /**
     * Links consumers compiled against v1.2.3 to the current runtime.
     *
     * @throws Exception if baseline artifacts, compilation, or runtime linkage fail.
     */
    @Test
    @EnabledIfSystemProperty(named = "arca.compatibility.baselineDir", matches = ".+")
    void runsBaselineCompiledConsumersAgainstCandidate() throws Exception {
        Path baseline = Path.of(System.getProperty("arca.compatibility.baselineDir"));
        List<String> modules = List.of("core", "soap", "wsaa", "wsfev1", "wsfexv1", "wsmtxca",
                "wscdc", "registry", "client", "spring-boot-starter");
        String baselineClasspath = modules.stream().map(module -> {
            String artifact = "arca-sdk-" + module;
            Path jar = baseline.resolve(artifact).resolve("target").resolve(artifact + "-1.2.3.jar");
            assertThat(jar).isRegularFile();
            return jar.toString();
        }).collect(Collectors.joining(File.pathSeparator));
        String externalClasspath = Arrays.stream(System.getProperty("java.class.path").split(
                        Pattern.quote(File.pathSeparator)))
                .filter(entry -> {
                    for (Path part : Path.of(entry)) {
                        if (part.toString().startsWith("arca-sdk-")) {
                            return false;
                        }
                    }
                    return true;
                }).collect(Collectors.joining(File.pathSeparator));
        compileAndRun(baselineClasspath + File.pathSeparator + externalClasspath);
    }

    private void compileAndRun(String classpath) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).isNotNull();
        for (String name : List.of("JavaConsumer", "SpringConsumer")) {
            Path source = temporary.resolve(name + ".java");
            try (var input = getClass().getResourceAsStream("/compatibility/" + name + ".java")) {
                assertThat(input).isNotNull();
                Files.writeString(source, new String(input.readAllBytes(), StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            }
            int status = compiler.run(null, null, null, "--release", "21", "-encoding", "UTF-8",
                    "-classpath", classpath, "-d", temporary.toString(), source.toString());
            assertThat(status).as("consumer compilation").isZero();
            try (var loader = new URLClassLoader(new URL[]{temporary.toUri().toURL()}, getClass().getClassLoader())) {
                Class<?> consumer = loader.loadClass("consumer.reference." + name);
                assertThat(consumer.getMethod("verify").invoke(null)).isEqualTo(true);
            }
        }
    }
}
