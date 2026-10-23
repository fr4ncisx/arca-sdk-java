import json
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path

from check_security_cache import check_cache
import check_security_cache


class SecurityCacheTest(unittest.TestCase):
    def test_rejects_processing_errors_even_when_maven_reports_success(self):
        with tempfile.TemporaryDirectory() as temporary:
            log = Path(temporary) / "update.log"
            log.write_text("[INFO] Check for updates complete\n[INFO] BUILD SUCCESS\n", encoding="utf-8")
            check_security_cache.check_update_log(log)
            for content in (
                "[ERROR] Failed to process CVE-2026-6785\n[INFO] BUILD SUCCESS\n",
                "[INFO] Checking for updates\n",
                "",
            ):
                log.write_text(content, encoding="utf-8")
                with self.subTest(content=content), self.assertRaises(ValueError):
                    check_security_cache.check_update_log(log)

    def test_requires_fresh_marker_and_database_metadata(self):
        now = datetime(2026, 10, 22, tzinfo=timezone.utc)
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            marker = directory / "refreshed-at.txt"
            report = directory / "report.json"
            with self.assertRaisesRegex(FileNotFoundError, "main"):
                check_cache(directory, now=now)
            marker.write_text("2026-10-22T00:00:00Z", encoding="utf-8")
            check_cache(directory, now=now)
            for age in (0, 48, 49, -1):
                timestamp = (now - timedelta(hours=age)).isoformat()
                report.write_text(json.dumps({"scanInfo": {"dataSource": [
                    {"name": "NVD API Last Checked", "timestamp": timestamp}
                ]}}), encoding="utf-8")
                with self.subTest(age=age):
                    if age in (0, 48):
                        check_cache(directory, report, now)
                    else:
                        with self.assertRaises(ValueError):
                            check_cache(directory, report, now)
            report.write_text("{}", encoding="utf-8")
            with self.assertRaises(ValueError):
                check_cache(directory, report, now)

    def test_rejects_stale_future_and_naive_markers(self):
        now = datetime(2026, 10, 22, tzinfo=timezone.utc)
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            for timestamp in ("2026-10-19T00:00:00+00:00", "2026-10-23T00:00:00+00:00",
                              "2026-10-22T00:00:00", "invalid"):
                (directory / "refreshed-at.txt").write_text(timestamp, encoding="utf-8")
                with self.subTest(timestamp=timestamp), self.assertRaises(ValueError):
                    check_cache(directory, now=now)


if __name__ == "__main__":
    unittest.main()
