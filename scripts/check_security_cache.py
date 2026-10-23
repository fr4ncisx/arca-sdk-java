import argparse
import json
from datetime import datetime, timedelta, timezone
from pathlib import Path


def check_update_log(path):
    content = path.read_text(encoding="utf-8", errors="replace")
    if "BUILD SUCCESS" not in content or "[ERROR]" in content:
        raise ValueError("NVD update is incomplete or reported processing errors")


def check_timestamp(value, now):
    if not isinstance(value, str):
        raise ValueError("Security cache timestamp must be an ISO-8601 string")
    value = value.strip()
    timestamp = datetime.fromisoformat(value[:-1] + "+00:00" if value.endswith("Z") else value)
    if timestamp.tzinfo is None:
        raise ValueError("Security cache timestamp must include its timezone")
    age = now - timestamp.astimezone(timezone.utc)
    if age < timedelta(0) or age > timedelta(hours=48):
        raise ValueError("Security cache timestamp is future-dated or older than 48 hours")


def check_cache(directory, report=None, now=None):
    now = now or datetime.now(timezone.utc)
    check_timestamp((directory / "refreshed-at.txt").read_text(encoding="utf-8"), now)
    if report is not None:
        data = json.loads(report.read_text(encoding="utf-8"))
        sources = data.get("scanInfo", {}).get("dataSource", [])
        checks = [source for source in sources if source.get("name") == "NVD API Last Checked"]
        if len(checks) != 1:
            raise ValueError("Report must contain the NVD database last-check timestamp")
        check_timestamp(checks[0]["timestamp"], now)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("directory", type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    try:
        check_cache(args.directory, args.report)
    except (OSError, ValueError, KeyError, TypeError) as error:
        raise SystemExit(f"Security cache validation failed: {error}") from error
    print("Security cache freshness verified")


if __name__ == "__main__":
    main()
