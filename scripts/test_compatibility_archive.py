import io
import tempfile
import unittest
import zipfile
from pathlib import Path

from check_compatibility import unpack_baseline


class CompatibilityArchiveTest(unittest.TestCase):
    def test_rejects_unsafe_and_credential_paths_before_extraction(self):
        for name in ("../outside", ".env", "cert.p12", "private.key"):
            with self.subTest(name=name), tempfile.TemporaryDirectory() as temporary:
                content = io.BytesIO()
                with zipfile.ZipFile(content, "w") as archive:
                    archive.writestr("pom.xml", "safe")
                    archive.writestr(name, "placeholder")
                directory = Path(temporary)
                with self.assertRaises(ValueError):
                    unpack_baseline(content.getvalue(), directory)
                self.assertFalse((directory / "pom.xml").exists())

    def test_extracts_safe_sources(self):
        with tempfile.TemporaryDirectory() as temporary:
            content = io.BytesIO()
            with zipfile.ZipFile(content, "w") as archive:
                archive.writestr("pom.xml", "safe")
            directory = Path(temporary)
            unpack_baseline(content.getvalue(), directory)
            self.assertEqual((directory / "pom.xml").read_text(encoding="utf-8"), "safe")


if __name__ == "__main__":
    unittest.main()
