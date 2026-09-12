# MineKot toolchain host checks

This roadmap owns Gradle/Detekt host execution and repository-wide validation. Concrete Kotlin and Gradle Kotlin DSL inspection coverage belongs to [`minekot-rules`](https://github.com/MineKotLang/minekot-rules/blob/master/CHECKS.md). Shared SPI, adapters, loading, and verification belong to [`minekot-inspections`](https://github.com/MineKotLang/minekot-inspections/blob/master/CHECKS.md). IntelliJ host behavior belongs to `minekot-toolkit`.

## Dynamic rules host

- [x] Resolve exact default or atomic project tag and manifest-digest lock during task execution.
- [x] Materialize one verified generation for `check`, Detekt, and every formatting pass.
- [x] Reuse verified exact artifacts offline and fail before analysis when exact artifact is absent.
- [x] Deduplicate cache work through configuration-cache-compatible shared build service.
- [x] Reject stale, overlapping, or conflicting corrections; reparse between bounded rounds and require fixed point.
- [x] Keep rules, core SPI, adapters, Kotlin PSI, and Detekt in correct classloader and dependency scopes.

## Repository-wide checks

- [x] Verify UTF-8 without BOM, LF endings, and one final newline from raw bytes.
- [x] Enforce Markdown formatting and repository policy outside Kotlin PSI.
- [x] Validate version catalogs, `gradle.properties`, project graphs, and cross-project build conventions.
- [x] Keep staged formatting and assisted transformations transactional.
- [x] Preserve KSP and registered source-generator output ownership across staging rounds.
- [x] Reject generated Detekt baselines and verify baseline tasks remain skipped.

## Verification

- [x] Cover exact default and override locks, configuration-cache reuse, parallel resolution, offline cache success, and cache-miss failure with TestKit.
- [x] Cover correction conflicts, round caps, fixed points, staged output, and repository smoke projects.
- [x] Exercise published signed rules release through canonical CI and retain terminal workflow evidence in [rules run 34605481455](https://github.com/MineKotLang/minekot-rules/actions/runs/34605481455).

Run:

```bash
./gradlew check mineKotSmokeTest --no-scan --no-configuration-cache
```
