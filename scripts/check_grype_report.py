import argparse
import json
import math
import xml.etree.ElementTree as ET
from datetime import datetime, timedelta, timezone
from pathlib import Path
from urllib.parse import unquote


def reject_constant(value):
    raise ValueError(f"Non-finite JSON number: {value}")


def read_json(path):
    data = json.loads(path.read_text(encoding="utf-8"), parse_constant=reject_constant)
    if not isinstance(data, dict):
        raise ValueError(f"Expected a JSON object in {path}")
    return data


def check_database(data, now=None):
    schema = data.get("schemaVersion")
    if (data.get("valid") is not True or data.get("error") or not isinstance(schema, str)
            or schema.removeprefix("v").split(".")[0] != "6"):
        raise ValueError("Grype database is missing, invalid, or incompatible")
    value = data.get("built")
    if not isinstance(value, str):
        raise ValueError("Grype database build timestamp is missing")
    built = datetime.fromisoformat(value.replace("Z", "+00:00"))
    if built.tzinfo is None:
        raise ValueError("Grype database build timestamp must include a timezone")
    age = (now or datetime.now(timezone.utc)) - built.astimezone(timezone.utc)
    if age < timedelta(0) or age > timedelta(hours=48):
        raise ValueError("Grype database is future-dated or older than 48 hours")


def maven_coordinates(purl):
    if not isinstance(purl, str) or not purl.startswith("pkg:maven/"):
        raise ValueError("Expected a Maven package URL")
    package = purl.split("?", 1)[0].split("#", 1)[0][len("pkg:maven/"):]
    try:
        name, version = package.rsplit("@", 1)
        group, artifact = name.rsplit("/", 1)
    except ValueError as error:
        raise ValueError("Incomplete Maven package URL") from error
    coordinates = tuple(unquote(value) for value in (group, artifact, version))
    if not all(coordinates):
        raise ValueError("Incomplete Maven package URL")
    return coordinates


def dependency_inventory(root):
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    project = ET.parse(root / "pom.xml").getroot()
    modules = [root] + [root / module.text for module in project.findall("m:modules/m:module", namespace)]
    trees = [read_json(module / "target/security-dependencies.json") for module in modules]
    reactor = {(tree["groupId"], tree["artifactId"]) for tree in trees}
    dependencies = set()
    pending = list(trees)
    while pending:
        node = pending.pop()
        if node.get("scope") == "test":
            continue
        coordinates = (node["groupId"], node["artifactId"], node["version"])
        if coordinates[:2] not in reactor:
            dependencies.add(coordinates)
        pending.extend(node.get("children", []))
    if not dependencies:
        raise ValueError("Maven dependency inventory is empty")
    return dependencies


def check_sbom(data, expected=None):
    components = data.get("components")
    if data.get("bomFormat") != "CycloneDX" or not isinstance(components, list) or not components:
        raise ValueError("CycloneDX SBOM has no components")
    coordinates = set()
    for component in components:
        actual = maven_coordinates(component.get("purl"))
        declared = tuple(component.get(field) for field in ("group", "name", "version"))
        if actual != declared:
            raise ValueError("SBOM coordinates do not match their Maven package URL")
        coordinates.add(actual)
    if expected is not None and not expected.issubset(coordinates):
        missing = sorted(expected - coordinates)
        raise ValueError(f"SBOM dependency coverage is incomplete: {missing}")
    return coordinates


def check_report(data, sbom_path, now=None):
    descriptor = data.get("descriptor", {})
    if descriptor.get("name") != "grype" or descriptor.get("version") != "0.121.0":
        raise ValueError("Unexpected Grype report version")
    check_database(descriptor.get("db", {}).get("status", {}), now)
    source = data.get("source", {})
    target = source.get("target")
    if source.get("type") != "sbom-file" or not isinstance(target, str) or Path(target).resolve() != sbom_path.resolve():
        raise ValueError("Grype report source does not match the validated SBOM")
    if not isinstance(data.get("matches"), list) or data.get("ignoredMatches"):
        raise ValueError("Invalid matches or unexpected ignored vulnerabilities in Grype report")
    failures = []
    for match in data["matches"]:
        vulnerability = match["vulnerability"]
        identifier = vulnerability["id"]
        package = match["artifact"]["purl"]
        maven_coordinates(package)
        advisories = [vulnerability] + match.get("relatedVulnerabilities", [])
        scores = []
        for advisory in advisories:
            for cvss in advisory.get("cvss", []):
                score = cvss["metrics"]["baseScore"]
                if type(score) not in (float, int) or not 0 <= score <= 10 or not math.isfinite(score):
                    raise ValueError(f"Invalid CVSS score for {identifier}")
                scores.append(score)
        if scores:
            highest = max(scores)
            blocked = highest >= 7
            reason = f"CVSS {highest:g}"
        else:
            severities = {advisory.get("severity") for advisory in advisories}
            if not severities.issubset({"Negligible", "Low", "Medium", "High", "Critical"}):
                raise ValueError(f"Unknown severity without CVSS for {identifier}")
            blocked = bool(severities & {"High", "Critical"})
            reason = "/".join(sorted(severities & {"High", "Critical"}))
        if blocked:
            failures.append(f"{identifier}: {package} ({reason})")
    if failures:
        raise ValueError("Security threshold exceeded:\n" + "\n".join(failures))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--database", type=Path)
    parser.add_argument("--sbom", type=Path)
    parser.add_argument("--dependency-root", type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    if not any((args.database, args.sbom, args.report)) or (args.report and not args.sbom):
        parser.error("Provide --database or --sbom; --report requires --sbom")
    try:
        if args.database:
            check_database(read_json(args.database))
        if args.sbom:
            expected = dependency_inventory(args.dependency_root) if args.dependency_root else None
            coordinates = check_sbom(read_json(args.sbom), expected)
            print(f"Validated {len(coordinates)} Maven SBOM components")
        if args.report:
            check_report(read_json(args.report), args.sbom)
    except (OSError, ValueError, KeyError, TypeError, AttributeError, ET.ParseError) as error:
        raise SystemExit(f"Security validation failed: {error}") from error
    print("Security validation passed")


if __name__ == "__main__":
    main()
