package io.github.fr4ncisx.arca.test.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Helper for loading XML fixtures from classpath during test execution.
 * <p>
 * Provides static methods to read fixture files as UTF-8 strings.
 * Throws IllegalArgumentException if the fixture is not found.
 *
 * @author fr4ncisx
 * @since 0.1.0-M1
 */
final class TestFixtureLoader {

    private TestFixtureLoader() {
    }

    /**
     * Loads an XML fixture from the classpath.
     *
     * @param fixturePath the classpath-relative path to the fixture file.
     * @return the file content as a UTF-8 string.
     * @throws IllegalArgumentException if the fixture is not found on the classpath.
     * @throws IllegalStateException    if the fixture cannot be read.
     */
    static String load(String fixturePath) {
        String normalizedPath = fixturePath.startsWith("/") ? fixturePath.substring(1) : fixturePath;
        try (InputStream stream = TestFixtureLoader.class.getClassLoader().getResourceAsStream(normalizedPath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Fixture not found on classpath: " + fixturePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read fixture: " + fixturePath, exception);
        }
    }
}
