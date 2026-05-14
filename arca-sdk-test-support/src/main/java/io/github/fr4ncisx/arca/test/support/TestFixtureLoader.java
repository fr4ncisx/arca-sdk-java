package io.github.fr4ncisx.arca.test.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Helper para cargar fixtures XML desde classpath durante tests de integración.
 */
public final class TestFixtureLoader {

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
    public static String load(String fixturePath) {
        String normalizedPath = fixturePath.startsWith("/") ? fixturePath.substring(1) : fixturePath;
        try (InputStream stream = TestFixtureLoader.class.getClassLoader().getResourceAsStream(normalizedPath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Fixture no encontrado en classpath: " + fixturePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el fixture: " + fixturePath, exception);
        }
    }
}
