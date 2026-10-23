import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SECURITY_WORKFLOW = ROOT / ".github" / "workflows" / "security.yml"
NVD_REFRESH_WORKFLOW = ROOT / ".github" / "workflows" / "nvd-refresh.yml"


class SecurityWorkflowTest(unittest.TestCase):
    def test_pull_request_scan_has_no_nvd_secret_reference(self):
        workflow = SECURITY_WORKFLOW.read_text(encoding="utf-8")

        self.assertIn("pull_request:", workflow)
        self.assertNotIn("secrets.NVD_API_KEY", workflow)
        self.assertNotIn("-DnvdApiKey", workflow)
        self.assertIn("dependency-check-db5.6-main-${{ runner.os }}-", workflow)

    def test_nvd_refresh_is_separate_and_restricted_to_main(self):
        workflow = NVD_REFRESH_WORKFLOW.read_text(encoding="utf-8")

        self.assertIn("push:", workflow)
        self.assertIn("schedule:", workflow)
        self.assertNotIn("workflow_dispatch:", workflow)
        self.assertNotIn("pull_request:", workflow)
        self.assertIn("if: github.ref == 'refs/heads/main'", workflow)
        self.assertIn("cancel-in-progress: false", workflow)
        self.assertIn("secrets.NVD_API_KEY", workflow)
        self.assertNotIn("-DnvdApiKey=", workflow)
        self.assertIn("dependency-check-db5.6-main-${{ runner.os }}-", workflow)


if __name__ == "__main__":
    unittest.main()
