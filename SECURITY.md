# Security Policy

## Reporting Vulnerabilities

Report security vulnerabilities privately to **fr4ncisx@gmail.com**, or use [GitHub private vulnerability reporting](https://github.com/fr4ncisx/arca-sdk-java/security/advisories/new) when enabled. Do not disclose credentials, private keys, fiscal payloads, or unpatched vulnerabilities in public issues.

Include the SDK version, affected modules, Java vendor and runtime version, operating system, expected behavior, impact, and a reproduction using synthetic data. Fixes and backports are evaluated per report; no response deadline, backport, or CVE assignment is guaranteed.

---

## Security Alerts for Integrators

### 1. Oracle Java JSSE Security Alert (CVE-2026-91403 — CVSS 9.1)

Oracle identifies JSSE as the affected component and rates this vulnerability CVSS 3.1 9.1. Its risk matrix states that it is remotely exploitable without authentication. Oracle lists these supported affected versions:

| Product | Versions listed by Oracle |
| :------ | :------------------------ |
| Oracle Java SE | 8u511, 8u511-perf, 11.0.33, 17.0.21, 21.0.13, 25.0.5, 27.0.1 |
| Oracle GraalVM for JDK | 17.0.21, 21.0.13 |
| Oracle GraalVM Enterprise Edition | 21.3.20 |

See the [Oracle Security Alert](https://www.oracle.com/security-alerts/alert-cve-2026-91403.html) and its [risk matrix](https://www.oracle.com/security-alerts/cve-2026-91403verbose.html) for details and patch availability. Oracle notes that earlier unsupported releases may also be affected. This alert identifies Oracle products; check the advisory from the actual Java runtime vendor for other distributions.

#### Impact for SDK Consumers

The SDK is compiled for Java 21, but that does not identify the Java vendor or runtime build used by a consuming application. Actual exposure depends on that runtime and its JSSE implementation. The SDK cannot determine whether a consumer's runtime is affected.

#### Remediation

Update the deployed Java runtime to a build that the vendor confirms contains the applicable security fix. Do not infer that another vendor's build is affected or fixed from Oracle's version numbers.

If a WSFEv1 voucher authorization times out, the SDK reports that the result is unknown and does not replay the request. Query the voucher with `FECompConsultar` before resubmitting; the [official WSFEv1 manual](https://www.afip.gob.ar/ws/WSFEV1/documentos/manual_desarrollador_COMPG_v3_3.pdf) describes this procedure for communication timeouts.

On thread interruption, a batch retains one entry per input position, including duplicate requests. Completed results remain at their original positions. Unresolved submitted requests carry an error stating that the outcome is unknown; query ARCA before resubmitting them. Requests that were not submitted carry a separate error stating that fact. Cancellation does not undo a request already sent to ARCA. Fail-fast termination continues to return the processed prefix.

`SoapHardeningUtil.enableTlsRevocationChecking()` is optional, JVM-wide certificate-revocation hardening. It enables CRL distribution point checking and client-driven OCSP using `com.sun.net.ssl.checkRevocation`, `com.sun.security.enableCRLDP`, and the Java Security property `ocsp.enable`. Call it before creating TLS clients. This setting affects the whole JVM and does not fix CVE-2026-91403 or replace the runtime vendor's security patch. See the [Java 21 JSSE security guide](https://docs.oracle.com/en/java/javase/21/security/security-developer-guide.pdf).

```java
SoapHardeningUtil.enableTlsRevocationChecking();
```

---

## Hardening Guidance for Integrators

### PKCS#12 Keys and Certificates (CWE-312 / CWE-316)

Never commit certificates, private keys, passwords, or WSAA tickets. Prefer `Pkcs12CertificateSource.fromBytes(byte[], char[])` when certificate material is already available in memory. A custom `TraSigner` can integrate an external signing implementation; the SDK does not provide an HSM or KMS adapter.

### Ticket Cache Directory Security (`ticketCacheDir`) (CWE-276 / CWE-22)

- The SDK stores persistent tickets in per-environment and per-CUIT subdirectories below `ticketCacheDir`.
- Legacy cache files directly under the configured root are ignored and are not migrated. Remove them manually after stopping processes that use older SDK versions.
- Plan the upgrade around the expiration of existing WSAA tickets. A fresh login may be rejected while ARCA still considers an old ticket valid. Do not delete a valid ticket as a way to revoke it, and do not copy legacy files into an identity-scoped directory: they do not establish their CUIT or environment. Roll out after ticket expiration or validate a controlled transition in homologation first.
- Cache reads, writes, eviction, and clearing are serialized with a JVM lock and a stable lock file for cooperating processes. Ticket updates use a restricted temporary file and atomic replacement.
- The directory must be writable even for reads, because reads acquire the lock file and may evict expired tickets. Use the same absolute directory path across cooperating instances; validate file locking and atomic replacement on the actual deployment volume. Do not delete the lock file while processes are running. This file lock protects individual cache operations, not the complete login sequence across independent clients or processes.
- On POSIX filesystems, cache files and lock files are restricted to `rw-------`, and cache directories to `rwx------`. On Windows and filesystems without POSIX permissions, configure equivalent access controls for the application account.
- Cache filenames are sanitized by the SDK to prevent path traversal through cache keys.

### SOAP Diagnostics and Fiscal Data

The SOAP handler logs message direction and message kind only. It does not serialize or log SOAP bodies, taxpayer identifiers, invoice amounts, authorization codes, WSAA tokens, or signatures. Applications should still avoid logging their own SOAP payloads or credentials.

### Consumer Compatibility

DTO collection inputs are immutable snapshots. Use a separate mutable list when editing data and construct a new DTO afterward. Null collections or elements are rejected where the model requires a collection. CAE and CAEA monetary values must be finite, and exchange rates must be finite and strictly positive. Public monetary types and constructor signatures are unchanged.

---

## Supply Chain and Software Bill of Materials (SBOM)

The SDK generates an aggregate CycloneDX 1.5 SBOM. The `Security Scan` workflow runs Grype 0.121.0 for pull requests targeting `main`, pushes to `main`, and daily. The Grype binary is pinned and verified against its SHA-256 checksum. No API credentials are provided to the workflow.

The Maven `security-scan` profile produces the SBOM. Vulnerability policy enforcement requires the Grype scan and Python validation commands below.

Grype downloads a prebuilt vulnerability database published by Anchore. A cache miss downloads this database during the same job, including on a pull request; no default-branch bootstrap is required. Each job explicitly checks for updates, verifies database validity and a build age of at most 48 hours, and saves the validated database before scanning. The manifest request has a 30-second timeout, the database download has a five-minute timeout, and the job has a 20-minute limit. Failed updates or invalid databases fail the check. Scan commands disable automatic updates so the report uses the database already validated by that job.

```bash
mvn package dependency:tree -Psecurity-scan -DskipTests -DoutputType=json -DoutputFile=target/security-dependencies.json
python -B scripts/check_grype_report.py --sbom target/bom.json --dependency-root .
grype --config .grype.yaml db update
grype --config .grype.yaml db status -o json > target/grype-db-status.json
python -B scripts/check_grype_report.py --database target/grype-db-status.json
grype --config .grype.yaml sbom:target/bom.json -o json --file target/grype-report.json
python -B scripts/check_grype_report.py --sbom target/bom.json --report target/grype-report.json
```

Install and verify Grype 0.121.0 before running these commands. JSON files must be UTF-8; Windows PowerShell redirection before PowerShell 7.4 may require explicit UTF-8 output handling. The commands write `target/grype-report.json`, database status, and `target/bom.json` and `target/bom.xml`. Maven includes compile, runtime, provided, and system dependencies, excluding dependencies used only in test scope. Published test-support dependencies remain included when their declared scope is compile. The SBOM must cover the external coordinates in each reactor module's resolved dependency tree, including transitive dependencies.

The policy rejects any CVSS base score of 7 or higher from a matched advisory or its related advisories. Without numeric CVSS data, High and Critical severities block; unknown severity fails as inconclusive. Malformed scores, non-finite values, invalid reports, unexpected ignored matches, and missing or stale database metadata fail validation. Findings are not filtered based on fix availability, and no ignore rules are configured. JSON reports and SBOMs remain available as workflow artifacts when the policy rejects findings.

### Validation Evidence and Limits

Run `python -B scripts/check_compatibility.py` to compare all runtime public APIs against the pinned v1.2.3 commit and exercise Java and Spring reference consumers. The normal unit-test command alone does not perform the baseline comparison. Reference consumers do not establish compatibility for unknown external applications, especially applications that mutate DTO lists.

The policy validates the database build timestamp in both `grype db status` and the scan report, rejecting future dates and ages over 48 hours. It also verifies that the report identifies the validated SBOM as its input. The age describes the published database build, not the arrival time of every upstream advisory.

Grype primarily uses GitHub Security Advisories for Maven packages and other feeds for other ecosystems, including NVD. Matching and CVSS results can differ from Dependency-Check. A passing scan does not establish complete advisory coverage or verify the Java runtime installed by a consumer. Anchore availability is still required for updates; caching avoids repeated database downloads but does not eliminate that external dependency. Old `.dependency-check-data` directories are not reused or deleted by this migration.

The migration also upgrades the managed Bouncy Castle provider and PKIX artifacts from 1.78.1 to 1.85, addressing the advisories detected by the new scan, including CVE-2026-8763 and CVE-2026-13506. Applications overriding the managed Bouncy Castle version must independently verify their resolved classpath and security exposure.

Live authorization requires a separately reviewed homologation configuration and explicit write opt-in. A successful catalog query verifies authenticated access, not voucher authorization. An existing voucher reconciled on a rerun must not be reported as a new successful authorization. Mock-server recovery tests establish SDK retry behavior, not remote service availability.

Live homologation tests are optional and remain unverified when no suitable environment is available. The SDK does not bundle or select a consumer's Java runtime; runtime patching remains the responsibility of each deployment.
