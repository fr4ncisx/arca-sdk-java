# Changelog

## 1.2.5

### Security

- Replace Dependency-Check and its direct NVD API refresh workflow with Grype 0.121.0 scanning the aggregate CycloneDX SBOM. The security workflow validates a prebuilt vulnerability database no older than 48 hours and supports cache misses in the same run.
- Upgrade Bouncy Castle to 1.85 to address vulnerabilities reported in the cryptography and PKIX dependencies.
- Keep the public SDK signatures unchanged and retain the CVSS 7 blocking threshold.

## 1.2.4

### Resilience and API compatibility

- Make DTO list values immutable snapshots. Create a separate mutable list and a new DTO when changing data.
- Disable automatic retries for voucher authorizations and reports. After a timeout, query the outcome before resubmitting. Safe queries retain up to three total attempts.
- Preserve one batch result per input position, including duplicate requests. Distinguish requests not sent from operations with an uncertain outcome. Fail-fast batches retain the processed prefix.
- Isolate persistent ticket caches by environment and CUIT. Do not migrate legacy flat files. Coordinate cache replacement with ticket expiration to avoid WSAA rejection of a still-active ticket.
- Require cache directories to support writes, file locks, and atomic replacement. Restrict cache permissions to the application account on Windows.
- Reject non-finite CAE and CAEA amounts and require finite positive exchange rates.
- Treat a zero timeout as unlimited and cap WSAA response bodies at 1 MiB.

### Security

- Limit SOAP transport logs to endpoint and message type. Keep TLS revocation hardening opt-in and JVM-wide; it does not replace updates to the Java provider.
