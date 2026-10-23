import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class SecurityWorkflowTest(unittest.TestCase):
    def test_security_gate_remains_required_without_secret_or_bootstrap_dependency(self):
        workflow = (ROOT / ".github/workflows/security.yml").read_text(encoding="utf-8")
        for trigger in ("pull_request:", "push:", "schedule:"):
            self.assertIn(trigger, workflow)
        self.assertIn("name: Security Scan", workflow)
        self.assertIn("security-check:", workflow)
        self.assertNotIn("secrets.", workflow)
        self.assertNotIn("continue-on-error", workflow)
        self.assertNotIn("cache-hit", workflow)
        self.assertIn("sha256sum --check", workflow)
        update = workflow.index("grype --config .grype.yaml db update")
        save = workflow.index("name: Save validated Grype database")
        scan = workflow.index("name: Scan SBOM and enforce CVSS policy")
        self.assertLess(update, save)
        self.assertLess(save, scan)
        self.assertFalse((ROOT / ".github/workflows/nvd-refresh.yml").exists())

    def test_sbom_scopes_are_explicit_and_dependency_check_is_removed(self):
        namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
        project = ET.parse(ROOT / "pom.xml").getroot()
        profile = project.find("m:profiles/m:profile[m:id='security-scan']", namespace)
        plugins = profile.findall("m:build/m:plugins/m:plugin", namespace)
        self.assertEqual(["cyclonedx-maven-plugin"],
                         [plugin.findtext("m:artifactId", namespaces=namespace) for plugin in plugins])
        config = plugins[0].find("m:configuration", namespace)
        for scope in ("Compile", "Runtime", "Provided", "System"):
            self.assertEqual("true", config.findtext(f"m:include{scope}Scope", namespaces=namespace))
        self.assertEqual("false", config.findtext("m:includeTestScope", namespaces=namespace))


if __name__ == "__main__":
    unittest.main()
