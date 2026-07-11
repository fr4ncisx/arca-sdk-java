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

### Null safety and JSpecify

- **JSpecify Integration**: Use JSpecify annotations (`@NullMarked`, `@Nullable`) to enforce compile-time nullness analysis.
- **Package-Level Defaults**: Every production package must declare a `package-info.java` containing `@NullMarked` to make all parameters and return types non-null by default.
- **Nullable Types**: Explicitly annotate fields, arguments, or return values that can be null using `@Nullable`.

## Javadoc guidelines

All Javadoc comments must be written **strictly in professional English**. No
other language is permitted.

### Class-level Javadoc

Every public class, interface, enum, and record must have a class-level Javadoc
that describes its purpose, responsibility, and role within the architecture.
Include `@author` and `@since` tags. The `@since` value must identify the
milestone or version where the symbol was introduced for the first time.
Do not rewrite an existing `@since` value during later refactors, package
moves, or follow-up milestones unless the symbol itself is newly introduced.

### Method-level Javadoc

Every public and protected method must have a Javadoc that describes:

- What the method does (not how).
- Parameters with `@param` tags.
- Return value with `@return` tag.
- Exceptions with `@throws` tag for all checked and significant unchecked
  exceptions.

### Formatting rules

- **No HTML tags** are allowed in Javadoc content. The only exception is the
  `<p>` tag for paragraph breaks.
- `<a href="...">` is permitted inside `{@see}` tags when referencing external
  WSDL files or official documentation that cannot be expressed with `{@link}`.
- Use `<p>` to separate logical sections within a Javadoc (e.g., between the
  main description and `@param` blocks, or to separate usage notes from
  behavioral descriptions).
- Use plain text formatting only. Rely on `{@code}` for inline code references
  and `{@link}` for cross-references.
- Use `{@see}` references only when strictly necessary for cross-module
  navigation or when the referenced type is not already obvious from the code
  context. Avoid redundant `@see` for closely related types in the same package.
- Keep descriptions concise, precise, and professional.

### Test documentation

Test classes and test methods must also have Javadoc written in English.
Describe the scenario under test and the expected behavior. Use `<p>` for
paragraph separation. No HTML tags.

Example:

```java
/**
 * Validates that a CUIT with an invalid check digit is rejected.
 * <p>
 * The validator computes the expected check digit using modulo 10
 * and compares it against the supplied digit.
 *
 * @author fr4ncisx
 * @since 1.0.0
 */
@Test
void rejectsInvalidCuitCheckDigit() {
    // ...
}
```

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

## Versioning and release guidelines

- **Version Synchronization**: Every new milestone or release version must explicitly update the version identifier across all project `pom.xml` files (parent and modules) and references in the `README.md` to guarantee documentation alignment.

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
