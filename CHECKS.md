# MineKot style-guide enforcement

This document records current deterministic enforcement against the MineKot [Kotlin](https://github.com/MineKotLang/dev.minekot.org/blob/master/content/codestyle/kotlin/index.md), [Gradle Kotlin DSL](https://github.com/MineKotLang/dev.minekot.org/blob/master/content/codestyle/gradle/index.md), and [Markdown](https://github.com/MineKotLang/dev.minekot.org/blob/master/content/codestyle/markdown/index.md) guides. It does not claim complete guide coverage. Subjective human judgment is not a build prerequisite.

## Status meanings

- **Automated** means a deterministic requirement is actively checked.
- **Partial** means useful cases are checked, but known guide cases remain.
- **Not automated** means the requirement depends on human intent or lacks safe deterministic proof.
- **Missing** means required deterministic enforcement is not implemented.
- **Not enforced** means enforcement was intentionally removed because it harmed readability or correctness.

## Kotlin coverage

| Requirement                    | Status        | Current enforcement                                                                                                                                                                       |
|:-------------------------------|:--------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| UTF-8, LF, final newline       | Automated     | `verifyMineKotCodestyle` checks raw bytes and final newlines.                                                                                                                             |
| American English               | Not automated | Language quality requires human review.                                                                                                                                                   |
| Import policy                  | Automated     | `ImportPolicy` checks grouping, wildcards, and nested imports.                                                                                                                            |
| 120 columns and wrapping       | Partial       | `MaxLineLength` and `LineWrapping` cover selected PSI shapes without rewriting strings.                                                                                                   |
| Formatter controls             | Automated     | Balanced controls are checked and automatic edits avoid excluded regions.                                                                                                                 |
| Indentation and spacing        | Partial       | Selected ktlint rules are enabled; complete guide coverage is not proven.                                                                                                                 |
| Trailing commas                | Automated     | Kotlin list, type, destructuring, index, context, lambda, and `when` contexts are covered.                                                                                                |
| Parameter wrapping             | Not enforced  | Dedicated enforcement was removed to avoid bloated declarations and calls.                                                                                                                |
| String-template braces         | Automated     | `StringTemplateBraces` reports and corrects simple unbraced templates.                                                                                                                    |
| Kotlin and kotlinx APIs        | Partial       | Full-analysis `ResolvedApiPreference` checks selected exact JDK calls and exempts configured helper/adapter package boundaries. Heuristic `KotlinxPreference` is deprecated and inactive. |
| Error handling                 | Partial       | Unsafe broad, swallowed, and cancellation-breaking catches plus selected discarded or unsafe `Result` uses are reported without automatic failure swallowing.                             |
| Explicit nested scope          | Partial       | Full analysis resolves outer members through nested lambdas and reports without correction. Some inline-contract and unlabeled-receiver shapes remain unsupported.                        |
| Coroutine preference           | Partial       | Full analysis resolves selected JDK concurrency and Bukkit scheduler callable IDs with helper/adapter package exemptions.                                                                 |
| Iteration form                 | Partial       | Existing loop-shape findings are report-only. Automatic conversion is disabled pending semantic proof.                                                                                    |
| Data-driven and modular design | Not automated | Architectural intent cannot be derived safely from syntax alone.                                                                                                                          |
| Intent-based value extraction  | Partial       | Numeric syntax checks cover selected suspicious literals; extraction intent and approved exceptions are not yet proven.                                                                   |
| MiniMessage and localization   | Partial       | Full analysis resolves selected Adventure sinks, raw component factories, and MiniMessage helpers. Logs, diagnostics, build output, protocols, and machine text are excluded.             |
| KDoc                           | Partial       | Public API and syntactically non-obvious internal properties are checked. Private declarations, contract-identical overrides, and redundant tag completeness are excluded.                |
| Comment necessity              | Not automated | Formatting is automated; necessity and content require human review.                                                                                                                      |

## Gradle Kotlin DSL coverage

Gradle Kotlin DSL receives applicable Kotlin checks plus the checks below.

| Requirement                          | Status        | Current enforcement                                                                           |
|:-------------------------------------|:--------------|:----------------------------------------------------------------------------------------------|
| Kotlin DSL only                      | Automated     | Non-generated `.gradle` and `.groovy` files fail repository verification.                     |
| Plugin declarations and block order  | Partial       | Imperative plugin application is rejected; complete top-level ordering is unfinished.         |
| Version catalogs and fixed versions  | Partial       | Dynamic versions are rejected; complete catalog structure and use are unfinished.             |
| Dependency scopes and exclusions     | Not automated | Correct scope and transitive intent require human review.                                     |
| Task names and lazy APIs             | Partial       | Camel-case action names and selected eager APIs are checked.                                  |
| Explicit task dependencies           | Partial       | Obvious declarations can be checked; complete task-graph intent cannot.                       |
| Repository syntax and order          | Partial       | Simple Maven syntax is corrected; full classification and ordering are unfinished.            |
| Typed accessors and extension blocks | Partial       | Selected class-token and untyped accessor forms are rejected.                                 |
| Project dependencies                 | Partial       | Complete file-JAR and multi-project checks are unfinished.                                    |
| Convention plugins and custom tasks  | Not automated | Extraction boundaries require architectural judgment.                                         |
| Reproducible configuration           | Partial       | Cache, parallel, configure-on-demand, baseline, and selected version checks exist.            |
| Test framework and forks             | Partial       | The plugin configures JUnit Platform and parallel forks; repository validation is incomplete. |
| Additional test source sets          | Missing       | Classpath and configuration wiring are not yet validated.                                     |

## Markdown coverage

Markdown is parsed with CommonMark `0.28.0` and GFM table support.

| Requirement                                         | Status        | Current enforcement                                                                          |
|:----------------------------------------------------|:--------------|:---------------------------------------------------------------------------------------------|
| UTF-8, LF, final newline, tabs, 999 columns         | Automated     | Raw-file and Markdown checks cover these requirements.                                       |
| ATX headings, hierarchy, spacing, and sentence case | Automated     | Source spans are used for line diagnostics.                                                  |
| Lists and indentation                               | Partial       | Marker and basic indentation checks exist; nested boundary coverage is incomplete.           |
| Fences and language identifiers                     | Automated     | Indented code and unlabeled fenced code are rejected.                                        |
| Emphasis markers                                    | Partial       | Underscore emphasis is rejected; necessity is not automated.                                 |
| Blockquotes and horizontal rules                    | Partial       | Deterministic spacing is checked; complete nested coverage is unfinished.                    |
| Tables                                              | Partial       | GFM tables and delimiter alignment are checked; complete cell-layout coverage is unfinished. |
| Images and placeholders                             | Partial       | Local paths and descriptive labels are checked; optimization is not automated.               |
| Internal links and anchors                          | Partial       | Local targets and document anchors are checked; GitHub anchor edge cases remain.             |
| Content quality and American English                | Not automated | Human review remains necessary.                                                              |
| External-link validity                              | Not automated | No deterministic network link check is part of `check`.                                      |

## Implemented interfaces

- `check` depends only on deterministic verification; no human approval receipt or semantic-review file is required. It runs plain build-script Detekt plus compilation-aware `detektMain` and `detektTest` tasks.
- `mineKotFormat` stages sources, applies one correction pass, snapshots source hashes, applies a second pass, and requires byte-identical output. It then verifies raw staged files, compiles every configured Kotlin/JVM compilation with serialized original compiler arguments and staged associated outputs, and publishes through rollback-capable per-file replacement.
- `mineKotAssistPreview` writes JSON and a focused unified diff without modifying sources.
- `mineKotAssistApply` requires the exact confirmed plan, stages Kotlin sources, runs the same per-compilation Kotlin/JVM validation, rechecks every staged Kotlin source hash plus requested replacements, and publishes through the rollback-capable publisher.
- Assisted schema version 1 uses sealed typed option models while retaining the JSON `options` object.
- `verifyMineKotCodestyle` rejects case-insensitive `*baseline*.xml` files outside ignored or generated paths.
- Every `DetektCreateBaselineTask` remains disabled; no baseline is generated or wired.
- Rule descriptors record lifecycle disposition, default reporting, correction safety, and replacement IDs. Unsafe heuristic corrections remain disabled until semantic proof exists.

Publication is transactional in the all-or-rollback sense. It is not globally atomic across multiple files.

## Open implementation roadmap

### Shared semantic and edit engine

- [ ] Replace remaining regex and name-based findings with Detekt 2 Analysis API symbol, receiver, expected-type, call, and source-set resolution where deterministic proof is possible.
- [ ] Add structured import insertion and removal to the shared edit model.
- [ ] Add resolved alias, annotation, comment, formatter-region, partial-syntax, overlap, stale-text, and multiple-finding matrices for every automatic correction.
- [x] Derive every configured Kotlin/JVM compilation while preserving sources, classpaths, compiler options/plugins, generated-source producers, friend paths, and associated staged outputs.
- [x] Validate custom Kotlin source roots and custom JVM compilations independently.
- [x] Retain unchanged project-dependency producer wiring during staged compilation.
- [ ] Add multi-project staged compilation for affected consumers.
- [x] Rerun registered source-derived Kotlin generators against isolated formatter and assisted source mirrors; fail closed for risky unregistered producers.
- [x] Clone KSP 2.3.10 Kotlin/class generation against staged source roots without running original KSP/compile tasks; disable staged KSP caching and reject path-like processor options.
- [ ] Add explicit isolation mappings for KSP processors that perform external filesystem writes instead of using `CodeGenerator`.
- [ ] Add target-specific Kotlin Multiplatform staged runners; the current plugin rejects KMP explicitly.

### Kotlin rules

- [x] Revise and approve the Kotlin and Gradle codestyle policy source.
- [x] Record keep, rewrite, deprecate, or remove decisions for all 17 rules.
- [x] Resolve a bounded set of Adventure sinks, component factories, and MiniMessage helpers through full analysis.
- [ ] Extend nested-receiver proof to inline-contract, extension-receiver, inherited, smart-cast, and unlabeled receiver shapes.
- [ ] Extend resolved API and coroutine descriptors with configured annotations, direct native escape chains, and overload-sensitive replacement proof.
- [ ] Replace remaining unsafe heuristics with symbol, type, and control-flow proof or downgrade them to non-correcting reports.
- [ ] Add fixtures proving every enabled correction converges after one pass and produces a byte-identical second pass.

### Gradle enforcement

- [ ] Validate complete top-level block order.
- [ ] Validate repository classification, placement, and order.
- [ ] Validate supported version-catalog structure and use, fixed plugin/dependency versions, and project dependency notation.
- [ ] Expand resolved eager-task API, typed-accessor, and explicit-dependency checks within the pinned compatibility baseline.
- [ ] Validate typed custom-task boundaries where syntax proves a violation.
- [ ] Validate build-cache configuration, test framework/forks, and additional test-source-set wiring.

### Markdown enforcement

- [x] Parse Markdown with CommonMark `0.28.0` and GFM tables.
- [x] Implement raw-byte, heading, fence, emphasis, basic-list, table, image, and local-link checks.
- [ ] Add fixtures for every deterministic guide subsection and support nested boundary.
- [ ] Complete GitHub-compatible anchor normalization and nested-list/table validation.

### Assisted transformations

- [ ] Resolve `File` declarations and selected call sites project-wide without line-based symbol selection.
- [ ] Derive numeric constant types from compiler-expected types, including inferred returns and arguments.
- [x] Preserve typed result-handling actions for discard, fallback, and failure handlers.
- [ ] Complete lifecycle-aware coroutine migration for owned executors, threads, timers, loops, dispatchers, cancellation, and managed-scope cleanup.
- [ ] Route MiniMessage migration only through resolved helpers with explicit localization, semantic-tag, and placeholder choices.
- [ ] Prove safe catch filtering, returns, rethrows, and cancellation semantics before try/catch conversion.

### Transaction and task verification

- [x] Preview tasks do not modify sources.
- [x] Assisted apply requires the exact confirmed plan and current fingerprints.
- [x] Publication failure injection proves rollback of already moved files.
- [ ] Add assisted staged raw-file verification before publication.
- [ ] Add clean and dirty multi-project format/apply fixtures.
- [ ] Add interrupted-publication recovery from the rollback journal.
- [ ] Verify one-pass formatter convergence across every enabled correction and interaction fixture.

### Final repository completion

- [ ] Migrate repository Kotlin and Gradle Kotlin DSL sources after the codestyle policy stabilizes.
- [x] Run targeted plugin and lint suites against the pinned release baseline.
- [x] Run `./gradlew check mineKotSmokeTest` successfully.
- [ ] Refresh test totals only from a final clean execution.

## Historical audits

Earlier completion claims described proposed or partial work as finished. Current coverage tables and unchecked roadmap supersede them.
