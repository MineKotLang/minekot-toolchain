# MineKot Toolchain

Gradle plugin, Detekt rules, and MineKot utility libraries.

## Gradle Plugin

```kotlin
plugins {
    id("org.minekot.toolchain") version "x.y.z"
}

minekotToolchain {
    toolchainVersion.set("x.y.z")
    serialization {
        enabled.set(true)
        libraryVersion.set("1.11.0")
    }
    adventure {
        enabled.set(true)
        libraryVersion.set("5.2.0")
    }
}
```

Useful tasks:

```shell
./gradlew mineKotInitializeProject
./gradlew writeMineKotProjectFiles
./gradlew writeMineKotCodestyle
./gradlew mineKotSmokeTest
```

`mineKotInitializeProject` writes missing project files and codestyle files. Project files are discovered from
`plugin/toolchain/src/main/resources/project`; special resource names map to normal project paths:
`gitattributes` to `.gitattributes`, `license` to `LICENSE`, `notice` to `NOTICE`, `readme` to `README.md`, and
`changelog` to `CHANGELOG.md`.
`writeMineKotCodestyle` also writes IntelliJ Actions on Save and commit cleanup defaults to `.idea/workspace.xml`.
`mineKotSmokeTest` runs the standalone sample project against the current checkout; it does not write codestyle files.

DSL values can also be supplied as Gradle project properties. Build script values override property conventions.

```properties
minekotToolchain.toolchainVersion=x.y.z
minekotToolchain.dependencyGroup=org.minekot
minekotToolchain.build.javaVersion=21
minekotToolchain.serialization.libraryVersion=1.11.0
minekotToolchain.repositories.releasesUrl=https://maven2.minekot.org/releases
minekotToolchain.repositories.snapshotsUrl=https://maven2.minekot.org/snapshots
```

## CI/CD

Root projects can opt into typed CI/CD tasks. Feature stays disabled by default and fails when enabled on a subproject.

```kotlin
minekotToolchain {
    ciCd {
        enabled.set(true)
        supportedVersions.set(listOf("1.21.8", "1.21.9"))
        publicationProjects.set(listOf(":api", ":core"))
        expectedArtifacts.set(listOf("paper.jar", "velocity.jar"))
        requiredEvidenceItems.set(listOf("fabric-1.21.8", "neoforge-1.21.8"))
    }
}
```

`writeMineKotCiMetadata` emits stable JSON for workflow matrices. `verifyMineKotChanges` validates typed Markdown fragments. `prepareMineKotRelease` folds fragments into `CHANGELOG.md`. `verifyMineKotRelease` enforces SemVer tags, protected-branch head, and stable evidence. `assembleMineKotRelease` creates exact artifact bundle, JSON manifest, and `SHA256SUMS`. `verifyMineKotRemotePublication` rejects partial Maven versions. `stageMineKotPublication` and `publishMineKotPublication` target only configured projects.

Versions ending in `-SNAPSHOT` or containing `-dev.` publish to snapshot repository. Immutable CI versions should use `<base>-dev.<run>.<commit>` so staged and remote Maven paths remain identical.

## Libraries

Library features are tracked by `mineKotLibraryFeatureDescriptors` so docs/tests stay data-driven.

- `minekot-kt-common`: `keys-results-validation`, `parsing-boilerplate`; typed IDs/keys, validation, safe UUID/enum parsing, Result collection and failure mapping.
- `minekot-kt-io`: `paths-resources`, `path-url-boilerplate`; UTF-8 text/lines/bytes, atomic writes, safe paths, recursive delete, URL class loaders, missing-only file/resource copies.
- `minekot-kt-serialization`: `json-config`, `json-migrations`; strict/lenient JSON defaults, config load/create, typed JSON fields, migration chains, JSON Result helpers.
- `minekot-kt-coroutines`: `coroutine-results`, `coroutine-lifecycle`; dispatcher helpers, timeout Result, supervised launch, managed scopes, retry, bounded parallel map, Result flows.
- `minekot-kt-atomic`: `atomic-state`, `atomic-gates`; atomic references, once gates, close gates, once values, conditional updates, atomic state holder.
- `minekot-kt-reflection`: `reflection-access`, `reflection-services`; property lookup/maps, named primary-constructor calls, annotations, sealed subclass lookup, ServiceLoader helpers.
- `minekot-kt-testing`: `test-fixtures`; Result assertions, temp project cleanup, path assertions, Gradle fixture files.
- `minekot-codegen-core`: `kotlinpoet-models`, `kotlinpoet-shapes`; KotlinPoet-safe names, generated headers, object/data-class/enum/sealed-interface render models.
- `minekot-ksp-helpers`: `ksp-symbols`; type-safe annotation, argument, class, and symbol readers plus validation and file writes.
- `minekot-adv-common`: `component-basics`, `component-layout`; component builders, word/line joins, key/value rows, bullet lists, plain-text serialization.
- `minekot-adv-minimessage`: `minimessage-templates`, `minimessage-validation`; string/component placeholders, parse Result, tag allowlists, legacy-color rejection.
- `minekot-adv-json`: `adventure-json`; component JSON Result helpers and list round trips.
- `minekot-adv-ansi`: `ansi-console`; ANSI serialization Result and escape stripping.

### Boilerplate Replacement

- Replace repeated `try { UUID.fromString(...) }` and `Enum.valueOf(...)` with `toMineKotUuidResult` and `toMineKotEnumResult`.
- Replace module jar URL plumbing with `toMineKotUrlClassLoaderResult`.
- Replace default config creation and version migration glue with `readMineKotConfigResult` and `migrateMineKotJsonConfig`.
- Replace repeated `SupervisorJob() + dispatcher` setup with `mineKotManagedScope`.

## Lint

Bundled Detekt rules are registered through a descriptor list. Active default rules cover try/catch, string template
braces, MiniMessage text, KDoc, coroutine preference, magic numbers, Result handling, and Kotlin/kotlinx API
preference.

Run:

```shell
./gradlew check
```
