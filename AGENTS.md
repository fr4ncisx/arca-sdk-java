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
- `arca-sdk-registry`: `ws_sr_padron_a4` integration.
- `arca-sdk-client`: unified root facade and client assembly.
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

- **Unused Imports**: Do not add imports that are not being used in the code or tests. Keep imports clean and organized, and remove any redundant or obsolete imports before finalizing a change.
- **No Fully Qualified Inline Names**: Do not write fully qualified class names (e.g. `java.util.List` or `java.time.Duration`) inline within method bodies, test methods, or class declarations when they can be imported. Always declare proper imports at the top of the file to maintain clean, readable, and idiomatic Java code.
- **Immutability by Default**: Use `record` classes for all domain models, configuration holders, DTOs, and value objects to enforce strict immutability.
- **Modern Control Flow & Pattern Matching**: Leverage pattern matching for `switch` and `instanceof`, and sealed hierarchies (`sealed interface/class`) for complex return states or errors instead of throwing checkable exceptions or casting objects.
- **Fail-Fast Validation**: Constructors and boundary methods must validate input parameters aggressively on entry (using `Objects.requireNonNull`, CUIT validators, or state checks) and throw `ArcaValidationException` immediately.
- **Prohibited Inline Comments**: Do not write inline comments (`// ...` or `/* ... */`) within method or constructor bodies to explain what the code does or to keep commented-out code. The code itself must be self-documenting. Clean up any commented-out code blocks or unnecessary inline comments in production and test code.
- **Strict Javadocs**: Only document classes, interfaces, records, enums, public/protected methods, and fields using official Javadoc format. Write Javadocs only when strictly necessary to describe the "what" and "why" of public APIs, never the implementation "how".

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

- **Strict Hexagonal Boundaries**: The domain layer (input/output records, exceptions, use cases) must remain pure and free from framework, transport, or serialization dependencies (such as `jakarta.xml.ws.*` or JAXB generated stubs under `internal.generated.*`). All SOAP mapping must reside exclusively within the infrastructure/mapper layer (`*Mapper.java`).
- **Single Responsibility Use Cases (SRP)**: Each remote call or operation must be encapsulated in its own separate Use Case class (e.g., `GetLastVoucherUseCase`, `RequestCaeUseCase`). Do not group unrelated operations into monolithic service classes.
- **Interface Segregation & Encapsulation**: Concrete implementations (e.g., `DefaultWsfeClient`) and assemblers (`WsfeClientAssembler`) must be package-private or hidden in `internal.*` packages. The public facade (`WsfeClient`, `ArcaClient`) must expose only minimal, abstract interfaces and immutable records.
- **Dependency Inversion (DIP)**: Use Case classes must depend on ports/abstractions (e.g., `AuthProvider`, `ArcaSoapPort`), never on concrete SOAP port implementations or client classes.

### Robust Exception Handling & Collections Null Safety
- **No Generic Catching**: Do not catch generic `Exception` or `Throwable` unless rethrowing or context wrapping at thread/async boundaries. Catch precise subclasses instead.
- **Soap & Serialization Wrapping**: Wrap lower-level SOAP transport faults (`WebServiceException`, `XMLStreamException`, JAXB errors) into clean, high-level SDK domain exceptions (`ArcaSoapException`, `ArcaException`) at the infrastructure layer, and ensure sensitive parameters are sanitized.
- **Zero Null Collections**: Methods returning collections must never return `null` under any circumstances. Always return immutable empty collections (e.g., `List.of()`, `Collections.unmodifiableList(...)`).

## Testing guidelines

Tests use JUnit, AssertJ, XMLUnit, and WireMock. Name test classes with
`*Test.java`, for example `ArcaEnvironmentTest.java`. Add unit tests for
validation rules, and use XML fixtures for SOAP payload comparisons. Normal
tests must not require network access.

- **Mocking Boundaries**: Mock only interfaces (`ArcaSoapPort`, `AuthProvider`). Never mock domain models, records, utilities, or static mapping methods.
- **Network Decoupling**: Unit tests must not perform real network traffic or bind to physical local ports. Real integration testing must utilize mock servers (WireMock/ArcaMockServer) with semantic XML comparison tools (`XMLUnit`) rather than literal string assertions.

## Commit, release, and git workflow guidelines

Recent commits use Conventional Commits: `feat(modules): ...`,
`fix(core): ...`, `refactor(core): ...`, and `chore(deps): ...`. Keep commits
focused. Pull requests must include a summary, tests run, linked issues when
available, and notes for security-sensitive changes.

- **Modular Commits**: Group changes into atomic, logical commits. Avoid creating large, single commits containing unrelated changes.
- **Conventional Commits**: Apply Conventional Commits conventions (e.g., `feat(wsfev1): ...`, `fix(core): ...`, `test(client): ...`, `chore(clean): ...`) to make the history clean and readable.
- **Explicit User Approvals**: Never perform git commits, pushes, or tags automatically. Present the exact file diffs and command proposals to the User, and wait for their explicit approval before executing any write or push operations to the repository.
- **Version Synchronization**: Every new milestone or release version must explicitly update the version identifier across all project `pom.xml` files (parent and modules) and references in the `README.md` to guarantee documentation alignment.
- **Annotated Releases**: All releases must use annotated Git tags (e.g., `git tag -a vX.Y.Z -m "Release description"`) summarizing the version changelog, and tags must be pushed to the remote repository explicitly after user confirmation.

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
