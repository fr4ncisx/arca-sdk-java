import copy
import json
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path
from unittest.mock import Mock

from check_grype_report import check_database, check_report, check_sbom, dependency_inventory, read_json


NOW = datetime(2026, 10, 23, tzinfo=timezone.utc)
PURL = "pkg:maven/example/library@1.0?type=jar"
COORDINATES = {("example", "library", "1.0")}
SBOM_PATH = Path("target/bom.json")


def database():
    return {"valid": True, "built": NOW.isoformat(), "schemaVersion": "6.0.5", "error": None}


def sbom():
    return {"bomFormat": "CycloneDX", "components": [
        {"type": "library", "group": "example", "name": "library", "version": "1.0",
         "purl": PURL, "bom-ref": PURL}
    ]}


def report(score=6.9, severity="Medium"):
    return {
        "descriptor": {"name": "grype", "version": "0.121.0", "db": {"status": database()}},
        "source": {"type": "sbom-file", "target": str(SBOM_PATH)},
        "matches": [{"artifact": {"purl": PURL}, "vulnerability": {
            "id": "CVE-TEST", "severity": severity,
            "cvss": [{"version": "3.1", "metrics": {"baseScore": score}}]
        }, "relatedVulnerabilities": []}],
        "ignoredMatches": [],
    }


class GrypeReportTest(unittest.TestCase):
    def test_accepts_cvss_below_threshold_and_empty_matches(self):
        check_report(report(), SBOM_PATH, NOW)
        data = report()
        data["matches"] = []
        check_report(data, SBOM_PATH, NOW)

    def test_blocks_cvss_at_threshold_and_above_even_without_fix(self):
        for score in (7.0, 10):
            with self.subTest(score=score), self.assertRaisesRegex(ValueError, "CVE-TEST"):
                check_report(report(score), SBOM_PATH, NOW)

    def test_uses_related_advisory_scores(self):
        data = report()
        data["matches"][0]["relatedVulnerabilities"] = [
            {"id": "GHSA-test", "severity": "High", "cvss": [
                {"version": "4.0", "metrics": {"baseScore": 9.3}}
            ]}
        ]
        with self.assertRaises(ValueError):
            check_report(data, SBOM_PATH, NOW)

    def test_considers_related_severity_when_all_cvss_scores_are_absent(self):
        data = report()
        data["matches"][0]["vulnerability"]["cvss"] = []
        data["matches"][0]["relatedVulnerabilities"] = [{"id": "GHSA-test", "severity": "Critical"}]
        with self.assertRaises(ValueError):
            check_report(data, SBOM_PATH, NOW)
        data["matches"][0]["relatedVulnerabilities"][0]["severity"] = "Unknown"
        with self.assertRaises(ValueError):
            check_report(data, SBOM_PATH, NOW)

    def test_falls_back_to_severity_only_when_no_numeric_score_exists(self):
        for severity in ("High", "Critical", "Unknown", "", None):
            data = report(severity=severity)
            data["matches"][0]["vulnerability"]["cvss"] = []
            with self.subTest(severity=severity), self.assertRaises(ValueError):
                check_report(data, SBOM_PATH, NOW)
        data = report()
        data["matches"][0]["vulnerability"]["cvss"] = []
        check_report(data, SBOM_PATH, NOW)

    def test_rejects_invalid_scores_instead_of_using_severity(self):
        for score in (float("nan"), float("inf"), -1, 10.1, 10 ** 500, "7.0", True, None):
            with self.subTest(score=score), self.assertRaises(ValueError):
                check_report(report(score), SBOM_PATH, NOW)

    def test_rejects_malformed_reports_and_unexpected_ignored_matches(self):
        for field in ("descriptor", "source", "matches"):
            data = report()
            del data[field]
            with self.subTest(field=field), self.assertRaises(ValueError):
                check_report(data, SBOM_PATH, NOW)
        data = report()
        data["ignoredMatches"] = copy.deepcopy(data["matches"])
        with self.assertRaises(ValueError):
            check_report(data, SBOM_PATH, NOW)

    def test_rejects_scan_of_another_source(self):
        data = report()
        data["source"]["target"] = "other.json"
        with self.assertRaisesRegex(ValueError, "source"):
            check_report(data, SBOM_PATH, NOW)

    def test_database_age_is_based_on_build_time(self):
        for age in (0, 48):
            data = database()
            data["built"] = (NOW - timedelta(hours=age)).isoformat()
            check_database(data, NOW)
        for value in (None, "invalid", "2026-10-23T00:00:00",
                      (NOW - timedelta(hours=49)).isoformat(),
                      (NOW + timedelta(seconds=1)).isoformat()):
            data = database()
            data["built"] = value
            with self.subTest(value=value), self.assertRaises(ValueError):
                check_database(data, NOW)

    def test_rejects_invalid_database_status(self):
        for change in ({"valid": False}, {"error": "corrupt"}, {"schemaVersion": ""},
                       {"schemaVersion": "v5.0.0"}, {"schemaVersion": True}):
            data = database() | change
            with self.subTest(change=change), self.assertRaises(ValueError):
                check_database(data, NOW)
        with self.assertRaises(ValueError):
            check_database({}, NOW)

    def test_sbom_requires_external_maven_coordinates_and_matching_purls(self):
        self.assertEqual(COORDINATES, check_sbom(sbom(), COORDINATES))
        for data in ({}, {"bomFormat": "CycloneDX", "components": []}):
            with self.subTest(data=data), self.assertRaises(ValueError):
                check_sbom(data, COORDINATES)
        for field in ("group", "name", "version", "purl"):
            data = sbom()
            del data["components"][0][field]
            with self.subTest(field=field), self.assertRaises(ValueError):
                check_sbom(data, COORDINATES)
        data = sbom()
        data["components"][0]["purl"] = "pkg:maven/other/library@1.0"
        with self.assertRaises(ValueError):
            check_sbom(data, COORDINATES)

    def test_detects_omitted_transitive_dependency(self):
        with self.assertRaisesRegex(ValueError, "coverage"):
            check_sbom(sbom(), COORDINATES | {("example", "transitive", "2.0")})

    def test_inventory_requires_every_module_and_excludes_test_subtrees(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / "pom.xml").write_text(
                '<project xmlns="http://maven.apache.org/POM/4.0.0"><modules>'
                '<module>module</module></modules></project>', encoding="utf-8")
            (root / "target").mkdir()
            (root / "module/target").mkdir(parents=True)
            module = {"groupId": "sdk", "artifactId": "module", "version": "1", "children": [
                {"groupId": "example", "artifactId": "library", "version": "1.0", "scope": "compile"},
                {"groupId": "test", "artifactId": "only", "version": "1", "scope": "test", "children": [
                    {"groupId": "test", "artifactId": "child", "version": "1", "scope": "compile"}
                ]}
            ]}
            tree = {"groupId": "sdk", "artifactId": "parent", "version": "1", "children": [module]}
            (root / "target/security-dependencies.json").write_text(json.dumps(tree), encoding="utf-8")
            with self.assertRaises(FileNotFoundError):
                dependency_inventory(root)
            (root / "module/target/security-dependencies.json").write_text(json.dumps(module), encoding="utf-8")
            self.assertEqual(COORDINATES, dependency_inventory(root))

    def test_rejects_invalid_json_and_nonfinite_json_constants(self):
        for value in ("{", "[]", '{"score": NaN}', '{"score": Infinity}'):
            path = Mock()
            path.read_text.return_value = value
            with self.subTest(value=value), self.assertRaises(ValueError):
                read_json(path)


if __name__ == "__main__":
    unittest.main()
