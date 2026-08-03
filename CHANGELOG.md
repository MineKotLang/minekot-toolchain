# Changelog

## 1.3.3 - 2026-08-03

### Fixes

- Exclude generated build sources from handwritten codestyle analysis while retaining compilation validation.
- Disable noisy outer-instance qualification findings by default; retain stricter semantic analysis as opt-in.
- Align standalone comments with neighboring code during deterministic formatting.
- Omit absent optional directories and original-module build outputs from staged classpaths so warnings-as-errors and Gradle producer validation do not block formatting; ignore processor-free KSP tasks that cannot generate staged output.
- Deduplicate KSP source and library views before processors generate declarations during isolated staged validation.

## 1.3.2 - 2026-08-03

### Fixes

- Exclude companion-object constants and functions from outer-instance receiver findings inside nested lambdas.

## 1.3.1 - 2026-08-03

### Fixes

- Restrict explicit nested-scope findings to implicit outer class and object members, excluding safe-qualified calls and DSL lambda receivers.

## 1.3.0 - 2026-07-30

### Breaking changes

- Remove semantic-review receipt tasks and extension properties; `check` now contains deterministic verification only.
- Reject Kotlin Multiplatform projects explicitly until target-specific staged compilers are supported.

### Features

- Add typed `mineKotRunCatching` boundaries that preserve cancellation, fatal errors, and unrelated failures.
- Add compiler-analysis Detekt rules for bounded API, coroutine, Adventure text, and nested-receiver checks.
- Compile formatter and assisted-fix staging per Kotlin/JVM compilation with original compiler options, plugins, classpaths, generated-source producers, and associated staged outputs.
- Add explicit staged-generator contracts and a KSP 2.3.10 adapter with isolated formatter and assisted outputs.

### Fixes

- Require formatter corrections to converge after one pass and remain byte-identical on the second pass.
- Reject unsafe generated-source provenance, generated KSP Java, concurrent formatter edits, and stale assisted source snapshots before publication.
- Keep staged compiler registration lazy and replace original associated-compilation outputs without scheduling original compile or KSP tasks.
- Align bundled codestyle verification with MineKot's two-space YAML indentation standard.
- Parse the release version from canonically spaced Gradle properties before publishing.
- Run compilation-aware Detekt tasks from `check` so Analysis API rules receive source-set classpaths.

### Deprecations

- Disable heuristic `KotlinxPreference`; use resolved API preference checks while compatibility remains.

## 1.2.1 - 2026-07-22

### Fixes

- Exclude `.agents` and `.codex` metadata from repository codestyle verification.
- Bootstrap current-version artifacts locally before CI and release verification.

## 1.2.0 - 2026-07-22

### Features

- Add disabled-by-default typed CI/CD conventions for metadata, changelog, release evidence, artifact bundles, checksums, Maven preflight, staging, and publication.
- Move default MineKot repository URLs to `https://maven2.minekot.org`.
