package io.github.fr4ncisx.arca.client;

import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.cmp.JApiCmpArchive;
import japicmp.model.JApiClass;
import japicmp.model.JApiChangeStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Release compatibility gate test verifying that japicmp programmatically detects binary incompatibility.
 *
 * @author fr4ncisx
 * @since 0.9.0
 */
class ReleaseCompatibilityGateTest {

    @TempDir
    Path tempDir;

    @Test
    void detectsBinaryIncompatibilityWhenMethodIsDeleted() throws Exception {
        String v1Source = """
            package io.github.fr4ncisx.mock;
            public class MockService {
                public void existingMethod() {}
                public void deletedMethod() {}
            }
            """;

        String v2Source = """
            package io.github.fr4ncisx.mock;
            public class MockService {
                public void existingMethod() {}
            }
            """;

        File jar1 = compileAndPackage("japicmp-mock-1.jar", "MockService", v1Source);
        File jar2 = compileAndPackage("japicmp-mock-2.jar", "MockService", v2Source);

        JarArchiveComparatorOptions options = new JarArchiveComparatorOptions();
        JarArchiveComparator comparator = new JarArchiveComparator(options);
        List<JApiClass> changes = comparator.compare(
                new JApiCmpArchive(jar1, "1.0.0"),
                new JApiCmpArchive(jar2, "2.0.0")
        );

        assertThat(changes).isNotEmpty();
        JApiClass jApiClass = changes.get(0);
        assertThat(jApiClass.getFullyQualifiedName()).isEqualTo("io.github.fr4ncisx.mock.MockService");
        
        boolean hasRemovedMethod = jApiClass.getMethods().stream()
                .anyMatch(m -> m.getName().equals("deletedMethod") && m.getChangeStatus() == JApiChangeStatus.REMOVED);
        assertThat(hasRemovedMethod).isTrue();
    }

    private File compileAndPackage(String jarName, String className, String sourceCode) throws Exception {
        Path srcDir = tempDir.resolve("src_" + jarName);
        Path binDir = tempDir.resolve("bin_" + jarName);
        Files.createDirectories(srcDir);
        Files.createDirectories(binDir);

        Path packageDir = srcDir.resolve("io/github/fr4ncisx/mock");
        Files.createDirectories(packageDir);
        Path sourceFile = packageDir.resolve(className + ".java");
        Files.writeString(sourceFile, sourceCode);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).describedAs("System Java Compiler must be available. Make sure you are running on a JDK, not a JRE.").isNotNull();

        int result = compiler.run(null, null, null,
                "-d", binDir.toString(),
                sourceFile.toString());
        assertThat(result).describedAs("Compilation of mock source failed").isEqualTo(0);

        File jarFile = tempDir.resolve(jarName).toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            Path classFile = binDir.resolve("io/github/fr4ncisx/mock/" + className + ".class");
            JarEntry entry = new JarEntry("io/github/fr4ncisx/mock/" + className + ".class");
            jos.putNextEntry(entry);
            jos.write(Files.readAllBytes(classFile));
            jos.closeEntry();
        }
        return jarFile;
    }
}
