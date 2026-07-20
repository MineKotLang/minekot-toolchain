# MineKot style-guide enforcement

This document records current enforcement against the MineKot
[Kotlin](https://github.com/MineKotLang/dev.minekot.org/blob/master/content/codestyle/kotlin/index.md),
[Gradle Kotlin DSL](https://github.com/MineKotLang/dev.minekot.org/blob/master/content/codestyle/gradle/index.md),
and [Markdown](https://github.com/MineKotLang/dev.minekot.org/blob/master/content/codestyle/markdown/index.md)
guides as reviewed on 2026-07-20.

It does not claim complete guide coverage.
An item is complete only when its implementation and matching tests pass.

## Current evidence

The following fresh targeted suites passed on 2026-07-20:

- `:plugin:minekot-toolchain-lint-rules:test`: 186 tests, zero failures.
- `:plugin:minekot-toolchain-gradle-plugin:test`: 39 tests, zero failures.

The plugin suite includes TestKit coverage for preview no-write behavior, confirmed plan fingerprints,
staged compilation, Markdown verification, semantic-review fingerprints, and publication rollback.

The repository-wide `./gradlew check mineKotSmokeTest` also passed on 2026-07-20.
The standalone smoke project passed semantic review, raw verification, Detekt, compilation, and smoke checks.

## Status meanings

- **Automated** means the deterministic requirement is actively checked.
- **Partial** means useful cases are checked, but known guide cases remain.
- **Review** means the requirement depends on developer intent and is confirmed by `minekot-review.json`.
- **Missing** means required enforcement is not implemented.
- **Not enforced** means the check was intentionally removed because it harmed readability.

## Kotlin coverage

| Requirement                    | Status              | Current enforcement                                                                        |
|:-------------------------------|:--------------------|:-------------------------------------------------------------------------------------------|
| UTF-8, LF, final newline       | Automated           | `verifyMineKotCodestyle` checks raw bytes and final newline.                               |
| American English               | Review              | `verifyMineKotSemanticReview` requires a source-fingerprinted confirmation.                |
| Import policy                  | Automated           | `ImportPolicy` checks grouping, wildcards, and nested imports.                             |
| 120 columns and wrapping       | Partial             | `MaxLineLength` and `LineWrapping` cover common PSI shapes without rewriting strings.      |
| Formatter controls             | Automated           | Balanced controls are checked and automatic edits avoid excluded regions.                  |
| Indentation and spacing        | Partial             | Selected ktlint rules are enabled; the complete guide matrix is not proven.                |
| Trailing commas                | Automated           | Kotlin list, type, destructuring, index, context, lambda, and `when` contexts are covered. |
| Parameter wrapping             | Not enforced        | Dedicated wrapping enforcement was removed to avoid bloated declarations and calls.        |
| String-template braces         | Automated           | `StringTemplateBraces` reports and corrects simple unbraced templates.                     |
| Kotlin and kotlinx APIs        | Partial             | Import rules, conservative corrections, and resolved JVM call checks cover selected APIs.  |
| Error handling                 | Partial             | Raw catches and selected discarded or unsafe `Result` use are checked.                     |
| Explicit nested scope          | Partial             | Conservative receiver/shadow checks exist; full receiver-stack proof is unfinished.        |
| Coroutine preference           | Partial             | Known thread, timer, executor, future, and sleep APIs are covered.                         |
| `forEach` preference           | Partial             | Safe loop shapes are corrected; control-flow and mutation hazards remain report-only.      |
| Data-driven and modular design | Review              | Architecture cannot be derived safely from syntax alone.                                   |
| MiniMessage and localization   | Partial plus review | Known sinks/helpers are checked; semantic tag and localization intent require review.      |
| KDoc                           | Partial             | Methods, parameters, returns, and inferred collection/state properties are checked.        |
| Comment necessity              | Review              | Formatting is automated; necessity and content remain semantic.                            |

## Gradle Kotlin DSL coverage

Gradle Kotlin DSL receives applicable Kotlin checks plus the checks below.

| Requirement                          | Status              | Current enforcement                                                                           |
|:-------------------------------------|:--------------------|:----------------------------------------------------------------------------------------------|
| Kotlin DSL only                      | Automated           | Non-generated `.gradle` and `.groovy` files fail repository verification.                     |
| Plugin declarations and block order  | Partial             | Imperative plugin application is rejected; complete top-level ordering is unfinished.         |
| Version catalogs and fixed versions  | Partial             | Dynamic versions are rejected; complete catalog structure and use are unfinished.             |
| Dependency scopes and exclusions     | Review              | Correct scope and transitive intent require semantic review.                                  |
| Task names and lazy APIs             | Partial             | Camel-case action names and selected eager APIs are checked.                                  |
| Explicit task dependencies           | Partial plus review | Obvious declarations can be checked; task-graph intent still needs review.                    |
| Repository syntax and order          | Partial             | Simple Maven syntax is corrected; full classification and ordering are unfinished.            |
| Typed accessors and extension blocks | Partial             | Selected class-token and untyped accessor forms are rejected.                                 |
| Project dependencies                 | Partial             | Complete file-JAR and multi-project checks are unfinished.                                    |
| Convention plugins and custom tasks  | Review              | Boundaries and extraction decisions require architectural review.                             |
| Reproducible configuration           | Partial             | Cache, parallel, configure-on-demand, baseline, and selected version checks exist.            |
| Test framework and forks             | Partial             | The plugin configures JUnit Platform and parallel forks; repository validation is incomplete. |
| Additional test source sets          | Missing             | Classpath and configuration wiring are not yet validated.                                     |

## Markdown coverage

Markdown is parsed with CommonMark `0.28.0` and GFM table support.

| Requirement                                              | Status    | Current enforcement                                                                          |
|:---------------------------------------------------------|:----------|:---------------------------------------------------------------------------------------------|
| UTF-8, LF, final newline, tabs, 999 columns              | Automated | Raw-file and Markdown checks cover these requirements.                                       |
| ATX headings, hierarchy, spacing, and uppercase headings | Automated | Source spans are used for line diagnostics.                                                  |
| Lists and indentation                                    | Partial   | Marker and basic indentation checks exist; all nested boundary cases need fixtures.          |
| Fences and language identifiers                          | Automated | Indented code and unlabeled fenced code are rejected.                                        |
| Emphasis markers                                         | Partial   | Underscore emphasis is rejected; necessity remains review-only.                              |
| Blockquotes and horizontal rules                         | Partial   | Deterministic spacing is checked; full nested cases need fixtures.                           |
| Tables                                                   | Partial   | GFM tables and delimiter alignment are checked; complete cell-layout coverage is unfinished. |
| Images and placeholders                                  | Partial   | Local paths and descriptive labels are checked; optimization remains review-only.            |
| Internal links and anchors                               | Partial   | Local targets and document anchors are checked; all GitHub anchor edge cases are unfinished. |
| Content quality and American English                     | Review    | Confirmed by the semantic-review artifact.                                                   |
| External links                                           | Review    | Confirmation expires after the configured maximum age, defaulting to 30 days.                |

## Implemented interfaces

- `mineKotFormat` stages deterministic corrections under `build/`, validates them, compiles staged Kotlin,
  and publishes with locking, sibling temporary files, per-file atomic moves, and complete rollback.
- `mineKotAssistPreview` writes JSON and a focused unified diff without modifying sources.
- `mineKotAssistApply` requires the exact confirmed plan, stages all Kotlin sources, compiles staged sources,
  rechecks fingerprints, and publishes through the rollback-capable publisher.
- Assisted schema version 1 uses sealed typed option models while retaining the JSON `options` object.
- `mineKotReviewPreview` writes the semantic-review template.
- `verifyMineKotSemanticReview` requires the exact aggregate fingerprint, reviewer, ISO-8601 timestamp,
  every fixed confirmation ID, and a non-expired external-link review.
- `verifyMineKotCodestyle` rejects case-insensitive `*baseline*.xml` files outside ignored/generated paths.
- Every `DetektCreateBaselineTask` remains disabled; no baseline is generated or wired.

Publication is transactional in the all-or-rollback sense.
It is not globally atomic across multiple files.

## Open implementation roadmap

### Shared semantic and edit engine

- [ ] Replace remaining regex and name-based findings with Detekt 2 Analysis API symbol, receiver,
  expected-type, call, and source-set resolution.
- [ ] Add structured import insertion/removal to the shared edit model instead of transformation-local text edits.
- [ ] Add resolved alias, annotation, comment, formatter-region, partial-syntax, overlap, stale-text,
  and multiple-finding matrices for every automatic correction.
- [ ] Derive affected production and test compilations from every Kotlin source set,
  preserving its classpath and compiler options instead of using one aggregate test classpath.
- [ ] Add multi-project staged compilation for all affected consumers.

### Kotlin rules

- [x] Treat raw MiniMessage tags as raw strings and cover Kotlin DSL sinks.
- [x] Include private, internal, and override methods in `MissingKDoc` checks.
- [x] Detect common inferred collection and state-property initializers.
- [ ] Resolve all MiniMessage UI, console, and logging sinks and helper symbols through analysis.
- [ ] Resolve every nested receiver and shadowed declaration before reporting `ExplicitScopeInNestedScope`.
- [ ] Replace remaining `KotlinxPreference`, `CoroutinePreference`, `ResultHandling`, `MagicNumber`,
  and `ForEachPreference` heuristics with symbol, type, and control/data-flow proof.
- [ ] Prove line wrapping plus configured ktlint converges after one correction pass and a byte-identical second pass.

### Gradle enforcement

- [ ] Validate complete top-level block order.
- [ ] Validate repository classification, placement, and order.
- [ ] Require complete version-catalog structure and use, fixed plugin/dependency versions,
  and project dependency notation.
- [ ] Cover all eager task APIs, typed accessors, extension/configuration/task names,
  and explicit dependency declarations.
- [ ] Validate typed custom-task boundaries where syntax can prove a violation.
- [ ] Validate build-cache configuration, test framework/forks, and additional test-source-set wiring.

### Markdown enforcement

- [x] Parse Markdown with CommonMark `0.28.0` and GFM tables.
- [x] Implement raw-byte, heading, fence, emphasis, basic list, table, image, and local-link checks.
- [ ] Add a fixture matrix for every deterministic guide subsection, formatter exclusion,
  nested block, anchor, table, image, and fenced-code boundary.
- [ ] Complete GitHub-compatible anchor normalization and nested-list/table validation.

### Assisted transformations

- [ ] Resolve `File` declarations and selected call sites project-wide without line-based symbol selection.
- [ ] Derive every numeric constant type from compiler expected types, including inferred returns and arguments.
- [x] Preserve typed result-handling actions for discard, fallback, and failure handlers.
- [ ] Complete lifecycle-aware coroutine migration for owned executors, threads, timers, loops,
  dispatchers, cancellation, and `mineKotManagedScope` cleanup.
- [ ] Route MiniMessage migration only through resolved MineKot helpers with explicit localization,
  semantic-tag, and placeholder choices.
- [ ] Prove safe catch filtering, returns, rethrows, and cancellation semantics before try/catch conversion.

### Transaction and task verification

- [x] Preview tasks do not modify sources.
- [x] Assisted apply requires the exact confirmed plan and current fingerprints.
- [x] Publication failure injection proves complete rollback of already moved files.
- [x] Semantic review requires exact fingerprints and complete confirmations.
- [ ] Add assisted staged raw-file verification before publication.
- [ ] Add clean and dirty multi-project format/apply fixtures.
- [ ] Add interrupted-publication recovery from the rollback journal.
- [ ] Prove `mineKotFormat` second-pass byte identity without additional convergence passes.

### Final repository completion

- [ ] Migrate repository Kotlin and Gradle Kotlin DSL sources to the remaining newly active rules.
- [ ] Check in a current root `minekot-review.json` if the root project begins applying the toolchain plugin.
- [x] Check in and verify the smoke-project `minekot-review.json` artifact after semantic review.
- [x] Run `./gradlew check mineKotSmokeTest` successfully.
- [x] Run smoke formatting to convergence, then verify a clean byte-identical pass.
- [ ] Refresh test totals from that final execution.

## Historical audits

Earlier “pre-completion” tables and completion claims described proposed or partial work as finished.
They are superseded by the current coverage tables and unchecked roadmap above.
