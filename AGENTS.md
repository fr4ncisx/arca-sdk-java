# Repository Guidelines

This guide summarizes how to work in this repository. Keep changes scoped and
aligned with the module boundaries below.

## Project structure and module organization

This is a Java 21 multi-module Maven SDK for ARCA SOAP services. Code lives in
each module's `src/main/java`, and tests live in `src/test/java`.

- `arca-sdk-core`: shared config, clocks, validation, exceptions, and log
  sanitization.
- `arca-sdk-soap`: SOAP transport, JAX-WS/Metro adapters, and SOAP config.
- `arca-sdk-wsaa`: WSAA authentication, TRA, CMS signing, and ticket cache.
- `arca-sdk-wsfev1`: WSFEv1 models, generated stubs, mappers, and use cases.
- `arca-sdk-registry`: future `ws_sr_padron_a4` integration.
- `arca-sdk-test-support`: WireMock utilities and XML fixtures.
- `arca-sdk-bom` and `arca-sdk-bundle`: dependency management and bundle.
- `dev/`: local dashboard/backlog assets. This directory is currently ignored
  by Git.

## Build, test, and development commands

Use Maven from the repository root. Do not use PowerShell for file operations
or text manipulation; use the edit tool instead. Batch independent tool calls
(glob, grep, read, edit) in parallel rather than running them sequentially.

- `mvn clean verify`: build all modules and run tests.
- `mvn test -pl arca-sdk-core`: run tests for one module.
- `mvn clean verify -Pgenerate-stubs`: regenerate stubs from WSDL files.
- `mvn verify -Darca.integration=true`: run integration tests that require real
  ARCA credentials and certificates.

## Coding style and naming conventions

Use Java 21 features deliberately, including records and sealed hierarchies. Use
four-space indentation. Keep public API types outside `internal.*`; generated
JAXB classes must stay isolated under
`internal.generated`. Prefer package names under `io.github.fr4ncisx.arca`.

## Architecture and quality principles

Use hexagonal architecture consistently. Keep public APIs and use cases
separate from infrastructure adapters, generated SOAP stubs, and transport
code. Apply SOLID and clean code: small classes, clear names, explicit
dependencies, and no hidden global state. Prefer immutable records for value
objects and interfaces for ports.

## Testing guidelines

Tests use JUnit, AssertJ, XMLUnit, and WireMock. Name test classes with
`*Test.java`, for example `ArcaEnvironmentTest.java`. Add unit tests for
validation rules, and use XML fixtures for SOAP payload comparisons. Normal
tests must not require network access.

## Commit and pull request guidelines

Recent commits use Conventional Commits: `feat(modules): ...`,
`fix(core): ...`, `refactor(core): ...`, and `chore(deps): ...`. Keep commits
focused. Pull requests must include a summary, tests run, linked issues when
available, and notes for security-sensitive changes.

## Security and configuration tips

Never commit certificates, passwords, private keys, real tokens, or SOAP
payloads with taxpayer data. Keep WSAA secrets out of logs, sanitize token/sign
values, and use fixtures or mocks. Treat OWASP Top Ten risks as baseline review
criteria, especially injection, sensitive data exposure, insecure configuration,
and insufficient logging or monitoring.

## Encoding and character safety

Always use **UTF-8** for reading and writing files. Windows defaults to
Windows-1252 (CP1252) for Spanish locale, which causes **mojibake** —
double-encoded characters like `Ã­` (should be `í`), `â†'` (should be `→`),
or `â€"` (should be `—`).

- **PowerShell**: `Get-Content` and `Set-Content` default to system encoding.
  Always specify `-Encoding UTF8`. Better yet, avoid PowerShell for file
  manipulation entirely — use the `edit` tool instead.
- **Python scripts**: Always specify `encoding='utf-8'` in `open()`.
- **Node.js scripts**: Use `fs.readFileSync(path, 'utf8')` or
  `fs.writeFileSync(path, data, 'utf8')`.
- **Git**: Configure `git config --global core.quotepath false` and ensure
  `.gitattributes` declares `* text=auto eol=lf`.
- **Java**: The project uses UTF-8 source encoding. Verify with
  `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>` in POM.
- **BOM**: Do not introduce UTF-8 BOM into any files.
