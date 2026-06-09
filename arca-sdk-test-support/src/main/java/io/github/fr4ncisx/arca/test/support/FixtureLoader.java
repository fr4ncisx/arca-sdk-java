package io.github.fr4ncisx.arca.test.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads XML fixtures from the test-support classpath.
 * <p>
 * The loader is package-private because fixture access is an implementation
 * detail of the public test-support API.
 *
 * @author fr4ncisx
 * @since 0.1.0-M3
 */
final class FixtureLoader {

    private final ClassLoader classLoader;

    FixtureLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new IllegalStateException("classLoader must not be null");
        }
        this.classLoader = classLoader;
    }

    String load(String fixturePath) {
        String normalizedPath = normalize(fixturePath);
        try (InputStream stream = classLoader.getResourceAsStream(normalizedPath)) {
            if (stream == null) {
                throw new IllegalStateException("Fixture not found on classpath: " + fixturePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read fixture: " + fixturePath, exception);
        }
    }

    private static String normalize(String fixturePath) {
        if (fixturePath == null || fixturePath.trim().isEmpty()) {
            throw new IllegalStateException("fixturePath must not be blank");
        }
        return fixturePath.startsWith("/") ? fixturePath.substring(1) : fixturePath;
    }
}
