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
 * POM validation test checking the structural integrity of the arca-sdk-bom artifact.
 * <p>
 * Ensures all internal modules are declared in dependencyManagement and are correctly
 * versioned via revision property placeholder.
 *
 * @author fr4ncisx
 * @since 0.6.0
 */
class BOMPomValidationTest {

    @Test
    void validateBOMPomStructure() throws Exception {
        File pomFile = new File("../arca-sdk-bom/pom.xml");
        assertThat(pomFile).exists();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(pomFile);

        XPath xPath = XPathFactory.newInstance().newXPath();

        // Query all dependency artifacts within dependencyManagement
        String xpathExpr = "/project/dependencyManagement/dependencies/dependency";
        NodeList nodes = (NodeList) xPath.compile(xpathExpr).evaluate(doc, XPathConstants.NODESET);
        assertThat(nodes.getLength()).isGreaterThan(0);

        List<String> internalModules = List.of(
                "arca-sdk-bundle",
                "arca-sdk-core",
                "arca-sdk-soap",
                "arca-sdk-wsaa",
                "arca-sdk-wsfev1",
                "arca-sdk-test-support",
                "arca-sdk-registry",
                "arca-sdk-client"
        );

        List<String> validatedModules = new ArrayList<>();

        for (int i = 0; i < nodes.getLength(); i++) {
            var node = nodes.item(i);
            String groupId = xPath.compile("groupId").evaluate(node).trim();
            String artifactId = xPath.compile("artifactId").evaluate(node).trim();
            String version = xPath.compile("version").evaluate(node).trim();

            if (internalModules.contains(artifactId)) {
                assertThat(groupId).isIn("${project.groupId}", "io.github.fr4ncisx");
                assertThat(version).isEqualTo("${revision}");
                validatedModules.add(artifactId);
            }
        }

        assertThat(validatedModules).containsExactlyInAnyOrderElementsOf(internalModules);
    }
}
