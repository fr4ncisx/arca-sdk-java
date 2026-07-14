package io.github.fr4ncisx.arca.client;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * POM validation test checking the structural integrity of the arca-sdk-bundle artifact.
 * <p>
 * Verifies that the bundle transitively includes all core modules and runtimes while
 * strictly excludes the test support module to keep production packaging clean.
 *
 * @author fr4ncisx
 * @since 0.6.0
 */
class BundlePomValidationTest {

    @Test
    void validateBundlePomStructure() throws Exception {
        File pomFile = new File("../arca-sdk-bundle/pom.xml");
        assertThat(pomFile).exists();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(pomFile);

        XPath xPath = XPathFactory.newInstance().newXPath();

        String xpathExpr = "/project/dependencies/dependency";
        NodeList nodes = (NodeList) xPath.compile(xpathExpr).evaluate(doc, XPathConstants.NODESET);
        assertThat(nodes.getLength()).isGreaterThan(0);

        List<String> expectedSDKModules = List.of(
                "arca-sdk-core",
                "arca-sdk-soap",
                "arca-sdk-wsaa",
                "arca-sdk-wsfev1",
                "arca-sdk-wsfexv1",
                "arca-sdk-registry",
                "arca-sdk-client"
        );

        List<String> expectedRuntimes = List.of(
                "jaxws-rt",
                "jaxb-runtime",
                "slf4j-api"
        );

        List<String> foundSDKModules = new ArrayList<>();
        List<String> foundRuntimes = new ArrayList<>();

        for (int i = 0; i < nodes.getLength(); i++) {
            var node = nodes.item(i);
            String groupId = xPath.compile("groupId").evaluate(node).trim();
            String artifactId = xPath.compile("artifactId").evaluate(node).trim();

            assertThat(artifactId).isNotEqualTo("arca-sdk-test-support");

            if (expectedSDKModules.contains(artifactId)) {
                assertThat(groupId).isEqualTo("${project.groupId}");
                foundSDKModules.add(artifactId);
            } else if (expectedRuntimes.contains(artifactId)) {
                foundRuntimes.add(artifactId);
            }
        }

        assertThat(foundSDKModules).containsExactlyInAnyOrderElementsOf(expectedSDKModules);
        assertThat(foundRuntimes).containsExactlyInAnyOrderElementsOf(expectedRuntimes);
    }
}
