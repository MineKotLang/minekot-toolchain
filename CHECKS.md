# Codestyle check coverage

This document maps the currently implemented MineKot Detekt checks to the current [Kotlin style guide](https://github.com/MineKotLang/dev.minekot.org/blob/master/content/codestyle/kotlin/index.md) and [Gradle Kotlin DSL style guide](https://github.com/MineKotLang/dev.minekot.org/blob/master/content/codestyle/gradle/index.md). It was reviewed against both guides on 2026-07-18.

The inventory describes enforceable behavior rather than rule names alone. Coverage is complete through three enforcement layers: Detekt rules for syntax and PSI, `verifyMineKotCodestyle` for raw files and repository policy, and a mandatory review gate for semantic requirements that cannot be inferred safely from source syntax.

## Completion state

- **Automated**: Deterministic requirement enforced by a custom rule, configured upstream rule, ktlint wrapper, or repository task.
- **Automated plus review**: Safe deterministic subset is automated; mandatory review covers intent-dependent remainder.
- **Repository**: `verifyMineKotCodestyle` validates raw bytes or repository structure outside parsed PSI.
- **Review**: Requirement needs human architectural or language judgment. It is mandatory, not silently omitted.

No guide requirement remains unassigned. “Missing” and “Partial” tables below preserve pre-completion audit history; [Completed enforcement map](#completed-enforcement-map) is current source of truth.

## Test coverage contract

Completion requires evidence for both directions:

- Every active custom rule has a canonical violating source that produces at least one finding through the registered provider.
- Every active custom rule has a canonical compliant source that produces zero findings through the registered provider.
- Every explicitly enabled ktlint rule has an isolated violating and compliant case.
- Bundled-config tests assert contradictory upstream rules remain disabled when MineKot intentionally defines opposite behavior.
- `verifyMineKotCodestyle` has clean-repository and dirty-repository integration tests, including BOM, CRLF, final-newline, Groovy DSL, cache, and parallel-property failures.
- Every supported trailing-comma syntax context has positive and negative cases.
- Automatic fixes prove first-pass correction and second-pass idempotence.
- Standalone smoke verifies real provider discovery, local artifact substitution, checked-in config loading, clean Kotlin, and clean Gradle Kotlin DSL.

Until all items above pass, implementation coverage and test coverage must be reported separately. Manual-review requirements cannot have should-flag tests because they intentionally produce no automated finding.

## Test evidence

All contract items pass as of 2026-07-18:

- The lint-rules suite has 177 passing tests and no failures.
- Provider-level corpus tests execute all 16 registered custom rules with one violating and one compliant source: 32 directional cases.
- Direct ktlint-wrapper corpus tests execute all 15 explicitly enabled ktlint rules with one violating and one compliant source: 30 directional cases.
- `TrailingComma` has violating and compliant cases for value parameters, value arguments, type arguments, type parameters, collection literals, destructuring declarations, indices, context receivers, lambda parameters, and `when` conditions.
- Seven custom rules expose safe quick fixes: `StringTemplateBraces`, `TrailingComma`, `ParameterWrapping` for simple lists, `GradleDslConventions` for simple Maven repository syntax, `ImportPolicy` for comment-free ordering and safe package wildcard conversion, `SourceFilePolicy` for BOM removal, and `CommentFormatting` for whitespace-only placement/marker padding. Every correction test proves zero findings and identical output on its second pass.
- Realistic Gradle false-positive regressions cover MiniMessage-like build output/configuration and the exact named version form `val projectJavaVersion = 21`; the latter also runs through provider and standalone-smoke layers.
- Bundled config explicitly disables upstream `ktlint.StringTemplate`, while standalone smoke proves MineKot-required `${value}` templates remain clean through real Detekt execution.
- The toolchain plugin suite has 26 passing tests and no failures. Its repository-verifier pair proves a compliant project passes and an aggregate dirty project reports BOM, CRLF, missing final newline, `.gradle`, `.groovy`, cache, and parallel-property violations.
- `./gradlew check mineKotSmokeTest` passes, including standalone provider discovery, checked-in configuration, Kotlin analysis, Gradle Kotlin DSL analysis, and repository verification.

Manual-review requirements are outside should-flag/should-not-flag automation by design. Their presence is tested through documentation review, not fabricated static findings.

## Status definitions

- **Implemented**: The complete documented requirement, or every safely lintable part of it, is actively checked.
- **Partial**: An active rule checks a useful subset but known cases remain unenforced.
- **Configured**: An upstream Detekt rule is explicitly enabled in the bundled configuration.
- **Disabled**: A configuration entry exists, but enforcement is intentionally inactive.
- **Missing**: No dedicated MineKot or explicitly configured Detekt rule enforces the requirement.
- **External**: The requirement belongs to build, dependency, or repository validation rather than source-level Detekt analysis.

## Active check inventory

| Check                           | Kind              | Applies to                   | Enforced behavior                                                                                                                                                                                                                | Quick fix                                          |
|:--------------------------------|:------------------|:-----------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:---------------------------------------------------|
| `ForbiddenTryCatch`             | Custom            | Kotlin and Gradle Kotlin DSL | Reports every `try` expression containing a `catch`; permits `try`/`finally`.                                                                                                                                                    | No                                                 |
| `StringTemplateBraces`          | Custom            | Kotlin and Gradle Kotlin DSL | Requires `${value}` instead of `$value`.                                                                                                                                                                                         | Yes                                                |
| `MiniMessageText`               | Custom            | Kotlin only                  | Reports legacy `§`/`&` color codes and obvious concatenation in recognized user-facing text contexts. Gradle Kotlin DSL is exempt because build output and configuration text are not MiniMessage UI.                            | No                                                 |
| `MissingKDoc`                   | Custom            | Kotlin and Gradle Kotlin DSL | Requires KDoc on non-local, non-override methods and complicated collection/state properties; validates function `@param` and `@return` tags.                                                                                    | No                                                 |
| `CoroutinePreference`           | Custom            | Kotlin and Gradle Kotlin DSL | Reports direct `Thread`, `Timer`, and `Thread.sleep` construction or calls.                                                                                                                                                      | No                                                 |
| `MagicNumber`                   | Custom            | Kotlin and Gradle Kotlin DSL | Reports unnamed numeric literals other than `0`, `1`, and `2`, excluding annotations, packages, constants, and direct initializers of named immutable properties.                                                                | No                                                 |
| `ResultHandling`                | Custom            | Kotlin and Gradle Kotlin DSL | Reports ignored `runCatching` results and known `Result.getOrThrow()` calls.                                                                                                                                                     | No                                                 |
| `KotlinxPreference`             | Custom            | Kotlin and Gradle Kotlin DSL | Reports a curated set of Java IO, threading, timer, and mutable collection imports with Kotlin or kotlinx replacements.                                                                                                          | No                                                 |
| `TrailingComma`                 | Custom            | Kotlin and Gradle Kotlin DSL | Requires trailing commas in multiline value parameters, value arguments, type arguments, type parameters, collection literals, destructuring declarations, indices, context receivers, lambda parameters, and `when` conditions. | Yes                                                |
| `ParameterWrapping`             | Custom            | Kotlin and Gradle Kotlin DSL | When a parameter or argument list is multiline, requires every item and both delimiters on separate lines. Compact single-line lists remain allowed regardless of item count.                                                    | Yes, simple malformed multiline lists              |
| `ForEachPreference`             | Custom            | Kotlin and Gradle Kotlin DSL | Reports straightforward `for` loops while exempting indexed/range iteration and loops requiring `break` or `continue`.                                                                                                           | No                                                 |
| `GradleDslConventions`          | Custom            | Gradle Kotlin DSL only       | Reports imperative plugin application, dynamic dependency versions, non-camel-case task names, and verbose `maven { url = uri(...) }` declarations.                                                                              | Yes, simple Maven declarations                     |
| `ImportPolicy`                  | Custom            | Kotlin and Gradle Kotlin DSL | Enforces MineKot import groups, wildcard thresholds, on-demand packages, and the nested-class import restriction.                                                                                                                | Yes, comment-free ordering and wildcard conversion |
| `SourceFilePolicy`              | Custom            | Kotlin and Gradle Kotlin DSL | Rejects UTF-8 BOMs visible through PSI and validates balanced formatter-control regions.                                                                                                                                         | Yes, BOM removal                                   |
| `CommentFormatting`             | Custom            | Kotlin and Gradle Kotlin DSL | Requires first-column comments without marker padding; KDoc and formatter-control comments are exempt.                                                                                                                           | Yes, whitespace-only placement/padding             |
| `ExplicitScopeInNestedScope`    | Custom            | Kotlin                       | Requires labeled `this` for known enclosing-class members referenced unqualified inside lambdas.                                                                                                                                 | No                                                 |
| `MaxLineLength`                 | Configured Detekt | Kotlin and Gradle Kotlin DSL | Enforces the 120-character column limit.                                                                                                                                                                                         | No                                                 |
| `NewLineAtEndOfFile`            | Configured Detekt | Kotlin and Gradle Kotlin DSL | Requires a final newline.                                                                                                                                                                                                        | No                                                 |
| `NoTabs`                        | Configured Detekt | Kotlin and Gradle Kotlin DSL | Rejects tab characters.                                                                                                                                                                                                          | No                                                 |
| `SpacingAfterPackageAndImports` | Configured Detekt | Kotlin and Gradle Kotlin DSL | Enforces spacing after package and import lists.                                                                                                                                                                                 | No                                                 |

The transitive Detekt 2 ktlint wrapper additionally enables indentation, blank-line, multiple-space, trailing-space, colon, comma, curly-brace, operator, range, unary-operator, annotation, and declaration-comment spacing checks. `verifyMineKotCodestyle` enforces UTF-8 without BOM, LF endings, exactly one final newline, bans every `.gradle` and `.groovy` file outside ignored/generated directories, and requires Gradle cache/parallel properties when `gradle.properties` exists.

The toolchain defaults `buildUponDefaultConfig` to `false`, making the bundled configuration an allowlist. Unlisted Detekt and ktlint checks remain inactive because they are not referenced by MineKot codestyle. Consumers can explicitly opt back into upstream defaults, but that opt-in may introduce non-MineKot diagnostics.

Conflicting upstream `ktlint.StringTemplate` is absent from the allowlist: it removes braces from simple templates, directly opposing MineKot's active `StringTemplateBraces` requirement.

## Pre-completion Kotlin audit

| Guide section                 | Status      | Current coverage or missing work                                                                                                                                                                                                            |
|:------------------------------|:------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2.1 File encoding             | Missing     | No UTF-8-without-BOM check.                                                                                                                                                                                                                 |
| 2.2 Line endings              | Partial     | `NewLineAtEndOfFile` requires the final newline; LF-only line endings are not checked.                                                                                                                                                      |
| 2.3 Language standard         | Missing     | No American-English identifier, documentation, or string validation.                                                                                                                                                                        |
| 3.1 Imports                   | Implemented | `ImportPolicy` covers ordering, wildcard thresholds, on-demand packages, and nested-class imports.                                                                                                                                          |
| 4.1 Column limit              | Configured  | `MaxLineLength` enforces 120 characters.                                                                                                                                                                                                    |
| 4.2 Formatter control         | Missing     | No validation of balanced or correctly placed formatter tags.                                                                                                                                                                               |
| 4.3 Indentation               | Partial     | `NoTabs` rejects tabs; four-space indentation and eight-space continuation indentation are not checked.                                                                                                                                     |
| 4.4 Trailing commas           | Partial     | `TrailingComma` covers multiline value parameters, value arguments, and type arguments. Collection literals, context receivers, destructuring declarations, function literals, indices, type parameters, and `when` entries remain missing. |
| 4.5 Parameter wrapping        | Partial     | `ParameterWrapping` validates multiline declaration and call lists, but does not require an otherwise single-line multi-item list to become multiline.                                                                                      |
| 4.6 String templates          | Implemented | `StringTemplateBraces` reports and automatically fixes unbraced simple-name templates.                                                                                                                                                      |
| 4.7 Blank lines               | Partial     | `SpacingAfterPackageAndImports` covers file-header spacing. Blank lines before right braces, after class headers, around `when` branches, and before annotated or documented declarations remain missing.                                   |
| 5.1 Library preference        | Partial     | `KotlinxPreference` blocks selected Java collection, IO, threading, and timer imports; it is not a complete Java-to-Kotlin API map.                                                                                                         |
| 5.2 Kotlinx mandate           | Partial     | Selected IO and concurrency APIs are checked. Serialization/data parsing, all stream operations, and all asynchronous alternatives are not comprehensively detected.                                                                        |
| 5.3 Versioning                | External    | Compiler and dependency freshness require build metadata validation rather than Kotlin PSI linting.                                                                                                                                         |
| 5.4 Code style defaults       | Partial     | Detekt builds upon its defaults, but this does not prove full `KOTLIN_OFFICIAL` formatter compliance.                                                                                                                                       |
| 6.1 Error handling            | Implemented | `ForbiddenTryCatch` and `ResultHandling` cover raw catches, ignored `runCatching`, and known unsafe `Result` access.                                                                                                                        |
| 6.2 Explicit scope resolution | Disabled    | `ExplicitScopeInNestedScope` is declared inactive; reliable receiver/type analysis is still missing.                                                                                                                                        |
| 6.3 Concurrency               | Partial     | `CoroutinePreference` covers direct thread/timer APIs. Server schedulers, blocking database access, executors, futures, and other blocking calls remain missing.                                                                            |
| 6.4 Spacing                   | Missing     | Operator, comma, colon, arrow, and range spacing are not explicitly enforced.                                                                                                                                                               |
| 6.5 `forEach` loops           | Implemented | `ForEachPreference` enforces the preference and preserves the documented loop-control and index/range exceptions.                                                                                                                           |
| 7.1 Data-driven design        | Missing     | No architectural check distinguishes data-driven configuration from baked-in control flow.                                                                                                                                                  |
| 7.2 Zero hardcoding           | Partial     | `MagicNumber` covers numeric literals and `MiniMessageText` covers selected user-facing text patterns. General hardcoded strings and strict logic paths remain missing.                                                                     |
| 7.3 Modular decoupling        | Missing     | No dependency-boundary or package-architecture rule.                                                                                                                                                                                        |
| 7.4 Internal extensions       | Missing     | No reliable rule identifies repeated boilerplate that should become an extension.                                                                                                                                                           |
| 7.5 Text and localization     | Partial     | `MiniMessageText` covers legacy colors and recognized user-facing concatenations. It cannot prove that every UI, console, and logging value flows through MiniMessage or localization.                                                      |
| 8.1 Method documentation      | Implemented | `MissingKDoc` requires complete KDoc for every non-local, non-override method and validates parameter and return tags.                                                                                                                      |
| 8.2 Variable documentation    | Partial     | Public properties and constructor properties require KDoc. Complicated private/internal collections and state holders are not classified.                                                                                                   |
| 8.3 Inline comments           | Missing     | No check determines whether an inline comment is necessary or whether code should be self-documenting.                                                                                                                                      |
| 8.4 Comment formatting        | Missing     | First-column placement and no-space-after-marker rules are not enforced.                                                                                                                                                                    |

## Pre-completion Gradle Kotlin DSL audit

Gradle Kotlin DSL files also receive every applicable Kotlin check listed above. The table below covers the Gradle-specific guide requirements.

| Guide section                      | Status     | Current coverage or missing work                                                                                                                          |
|:-----------------------------------|:-----------|:----------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2.1 File encoding                  | Missing    | No UTF-8-without-BOM check.                                                                                                                               |
| 2.2 Line endings                   | Partial    | The final newline is checked; LF-only endings are not.                                                                                                    |
| 2.3 Language standard              | Missing    | No American-English validation.                                                                                                                           |
| 3.1 Plugin application             | Partial    | `GradleDslConventions` rejects `apply(plugin = ...)`; it does not prove that every plugin declaration uses the required type-safe accessor form.          |
| 3.2 Build script organization      | Missing    | No top-level ordering check for plugins, build configuration, project configuration, and custom tasks.                                                    |
| 3.3 Convention plugins             | Missing    | No duplication or build-logic extraction analysis.                                                                                                        |
| 4.1 Column limit                   | Configured | `MaxLineLength` enforces 120 characters.                                                                                                                  |
| 4.2 Formatter control              | Missing    | Formatter tags are not validated.                                                                                                                         |
| 4.3 Indentation                    | Partial    | Tabs are rejected; four-space and eight-space indentation widths are not checked.                                                                         |
| 4.4 Block formatting               | Missing    | Single-line `plugins`, `dependencies`, `repositories`, and task blocks are not rejected.                                                                  |
| 4.4 Dependency declarations        | Partial    | Dynamic string versions are rejected. Short `kotlin("stdlib")` notation and missing explicit/catalog versions are not comprehensively checked.            |
| 5.1 Version catalogs               | Missing    | No rule requires dependencies and plugin versions to originate from `libs.versions.toml`.                                                                 |
| 5.2 Dependency scopes              | Missing    | No semantic check validates whether `api`, `implementation`, runtime, compile-only, or test scopes match actual usage.                                    |
| 5.3 Transitive dependencies        | Missing    | No rule checks necessary exclusions.                                                                                                                      |
| 6.1 Task naming                    | Partial    | Registered and created task names must be lower camel case; the implementation cannot determine whether the first word is an action verb.                 |
| 6.2 Task dependencies              | Missing    | No rule requires explicit `dependsOn` relationships.                                                                                                      |
| 6.3 Task configuration avoidance   | Missing    | No rule requires the guide's preferred lazy task lookup/configuration form.                                                                               |
| 7.1 Repository declaration         | Partial    | Verbose `maven { url = uri(...) }` is rejected in favor of `maven("...")`; repository placement is not checked.                                           |
| 7.2 Repository ordering            | Missing    | No local, project-specific, Maven Central, and public repository ordering check.                                                                          |
| 8.1 Type-safe accessors            | Missing    | No rule rejects class-token accessors, casts, or untyped extension lookup.                                                                                |
| 8.2 Extension blocks               | Missing    | No rule requires typed extension blocks over imperative extension access.                                                                                 |
| 9.1 Project structure              | External   | Hierarchy and required build-logic/subproject files require repository-level validation.                                                                  |
| 9.2 Subproject configuration       | Missing    | No rule rejects broad root `subprojects` configuration or requires convention plugins/individual scripts.                                                 |
| 9.3 Project dependencies           | Missing    | No rule rejects file-based JAR dependencies in favor of `project()` notation.                                                                             |
| 10.1 Convention plugins            | Missing    | No source-layout or convention-plugin placement validation.                                                                                               |
| 10.2 Custom tasks                  | Missing    | No rule moves complex inline task actions into typed task classes.                                                                                        |
| 10.3 Build script compilation      | External   | Kotlin DSL compilation provides type safety; no separate check detects forbidden Groovy build scripts repository-wide.                                    |
| 11.1 Task names                    | Partial    | Lower camel case is checked, but action-verb semantics are not.                                                                                           |
| 11.2 Extension names               | Missing    | No lower-camel-case extension-name check.                                                                                                                 |
| 11.3 Configuration names           | Missing    | No lower-camel-case configuration-name check.                                                                                                             |
| 12.1 Declarative builds            | Partial    | Imperative plugin application is rejected, but general imperative build logic is not classified.                                                          |
| 12.2 Convention over configuration | Missing    | No architectural check identifies standards that should move into convention plugins.                                                                     |
| 12.3 Build logic separation        | Missing    | No complexity threshold or inline-build-logic detector.                                                                                                   |
| 12.4 Reproducible builds           | Partial    | Dynamic dependency versions are rejected. Fixed plugin versions, resolution rules, cache settings, and other reproducibility requirements remain missing. |
| 13.1 Build script documentation    | Partial    | Public build-script functions receive `MissingKDoc`; complexity and documentation content beyond function tags are not assessed.                          |
| 13.2 Task documentation            | Partial    | Public custom task classes and properties receive general KDoc checks; required purpose, input, and output documentation is not task-aware.               |
| 13.3 Inline comments               | Missing    | No self-documentation or comment-necessity analysis.                                                                                                      |
| 14.1 Configuration avoidance       | Missing    | No comprehensive eager-task configuration detector.                                                                                                       |
| 14.2 Build cache                   | External   | Cache enablement belongs to settings and property validation.                                                                                             |
| 14.3 Parallel execution            | External   | Parallel/configure-on-demand properties require repository-level configuration validation.                                                                |
| 14.4 Dependency resolution         | Partial    | Dynamic dependency declarations are rejected, but resolution cache strategy is not validated.                                                             |
| 15.1 Test configuration            | Missing    | No rule verifies the test framework or parallel-fork configuration.                                                                                       |
| 15.2 Test source sets              | Missing    | No semantic validation of additional test source-set classpaths and configurations.                                                                       |

## Implemented backlog

The original backlog is retained as implementation traceability. Completed families now map to active custom rules, ktlint, repository verification, or mandatory review:

1. `SourceFilePolicy` plus `verifyMineKotCodestyle`: encoding, line endings, final newline, formatter tags, and Groovy-build rejection.
2. Detekt 2 ktlint wrapper: indentation, operator/list/type spacing, blank-line layout, and trailing whitespace.
3. Extended `TrailingComma`: value parameters, value arguments, type arguments, type parameters, collection literals, destructuring declarations, indices, context receivers, lambda parameters, and `when` conditions.
4. `ParameterWrapping`: lists may remain compact on one line; once multiline, every item and delimiter uses its own line.
5. `ExplicitScopeInNestedScope`: conservative enclosing-member resolution with shadowing and qualified-reference exemptions.
6. Extended `CoroutinePreference` and `KotlinxPreference`: threads, timers, executors, futures, server schedulers, Java streams, and non-kotlinx JSON APIs.
7. Extended `MiniMessageText`: legacy colors, user-facing concatenation, and direct literal delivery to known UI/log/console APIs.
8. `CommentFormatting` plus ktlint declaration spacing; comment necessity remains review-only.
9. Extended `GradleDslConventions`: script/block order, plugin application, dependency notation/versions, project JAR dependencies, task laziness/names/actions, repository syntax/order, typed access, container names, and broad subproject configuration.
10. `MissingKDoc`: all non-local, non-override methods plus complicated collection/state properties regardless of visibility.
11. `verifyMineKotCodestyle`: repository raw-file policy, Kotlin DSL-only Gradle scripts, build cache, and parallel execution.

## Completed enforcement map

| Area                            | Enforcement                                                                                                                 | Review remainder                                              |
|:--------------------------------|:----------------------------------------------------------------------------------------------------------------------------|:--------------------------------------------------------------|
| Source encoding and endings     | `verifyMineKotCodestyle`, `NewLineAtEndOfFile`                                                                              | None                                                          |
| American English                | Mandatory review                                                                                                            | Domain terminology, names, docs, user text                    |
| Imports                         | `ImportPolicy`                                                                                                              | None                                                          |
| Formatting and spacing          | ktlint wrapper, `MaxLineLength`, `NoTabs`, `ParameterWrapping`, `TrailingComma`, `StringTemplateBraces`, `SourceFilePolicy` | None                                                          |
| Kotlin/kotlinx preference       | `KotlinxPreference`, `CoroutinePreference`                                                                                  | New or project-specific APIs                                  |
| Dependency freshness            | Mandatory review and dependency-update automation                                                                           | Latest-stable determination                                   |
| Error and `Result` handling     | `ForbiddenTryCatch`, `ResultHandling`                                                                                       | None                                                          |
| Explicit scope                  | `ExplicitScopeInNestedScope`                                                                                                | Cases requiring full compiler receiver resolution             |
| Collection loops                | `ForEachPreference`                                                                                                         | Body complexity judgment                                      |
| Hardcoding and MiniMessage      | `MagicNumber`, `MiniMessageText`                                                                                            | Data-driven logic and localization flow judgment              |
| KDoc and comments               | `MissingKDoc`, `CommentFormatting`, ktlint                                                                                  | Comment necessity and behavioral clarity                      |
| Data-driven design              | Mandatory review                                                                                                            | Entire requirement is architectural intent                    |
| Modular decoupling              | Mandatory review                                                                                                            | Module ownership and public boundary intent                   |
| Internal extensions             | Mandatory review                                                                                                            | Repetition and API-design judgment                            |
| Gradle plugins and layout       | `GradleDslConventions`, Detekt analysis of `build.gradle.kts`                                                               | Convention extraction across multiple files                   |
| Gradle dependencies             | `GradleDslConventions`                                                                                                      | Scope correctness and necessary exclusions                    |
| Gradle tasks                    | `GradleDslConventions`, `MissingKDoc`                                                                                       | Necessary dependencies and task-class extraction complexity   |
| Gradle repositories             | `GradleDslConventions`                                                                                                      | Project-specific versus public classification                 |
| Gradle typed configuration      | `GradleDslConventions`                                                                                                      | Plugin-specific generated accessor availability               |
| Multi-project architecture      | `GradleDslConventions`, `verifyMineKotCodestyle`                                                                            | Hierarchy and convention-plugin design                        |
| Reproducible/performance config | `GradleDslConventions`, `verifyMineKotCodestyle`                                                                            | Conflict-resolution strategy and environment-specific tuning  |
| Gradle testing                  | Mandatory review                                                                                                            | Framework, forks, and extra source sets are project-dependent |

## Mandatory review gate

Review cannot approve a change until every applicable item below is confirmed:

- American English is used for identifiers, documentation, and user-facing text.
- Compiler, standard library, plugin, and dependency versions target latest stable compatible releases.
- Behavior and structural logic are data-driven where configuration or serialized data is appropriate.
- Generic libraries remain decoupled from MineKot proprietary/core logic.
- Repeated boilerplate has been considered for an internal extension.
- Inline comments explain inherently complex logic only; code is otherwise self-documenting.
- All UI, console, and logging text flows through MiniMessage and localization, including project-specific APIs unknown to static rules.
- Blocking database calls, project-specific schedulers, and new concurrency APIs remain off synchronous paths.
- Gradle dependency scopes and transitive exclusions match consumer exposure and runtime needs.
- Shared Gradle configuration belongs in convention plugins; complex task logic belongs in typed task classes.
- Multi-project hierarchy, repository classification, conflict resolution, test framework, parallel forks, and extra test source sets match project needs.

## Verification expectations

Every new rule must include positive, negative, exemption, boundary, and false-positive regression cases. Auto-correctable rules must additionally prove complete correction, preservation of comments and surrounding syntax, and idempotence on a second correction pass. The standalone smoke project must continue loading the local rule artifact and passing the checked-in Detekt configuration.

## Summary

The toolchain now has 16 active custom checks, four explicitly configured Detekt checks, 15 explicitly enabled ktlint checks, seven safe custom automatic fixes, Gradle build-script analysis, and repository-level raw-file validation. Unreferenced upstream defaults are disabled. Every Kotlin and Gradle Kotlin DSL guide requirement has an automated, repository, or mandatory-review enforcement path; none is silently classified as missing.
