# MineKot toolchain

Gradle plugin, Detekt rules, and MineKot utility libraries.

## Gradle plugin

```kotlin
plugins {
    id("org.minekot.toolchain") version "x.y.z"
}

minekotToolchain {
    toolchainVersion = ("x.y.z")
    serialization {
        enabled.set(true)
        libraryVersion.set("1.11.0")
    }
    adventure {
        enabled.set(true)
        libraryVersion = ("5.2.0")
    }
}
```

Useful tasks:

```shell
./gradlew mineKotInitializeProject
./gradlew writeMineKotProjectFiles
./gradlew writeMineKotCodestyle
./gradlew mineKotFormat
./gradlew check
./gradlew mineKotSmokeTest
```

`mineKotInitializeProject` writes missing project files and codestyle files. Project files are discovered from `plugin/toolchain/src/main/resources/project`; special resource names map to normal project paths: `gitattributes` to `.gitattributes`, `license` to `LICENSE`, `notice` to `NOTICE`, `readme` to `README.md`, and `changelog` to `CHANGELOG.md`.

`writeMineKotCodestyle` also writes IntelliJ Actions on Save and commit cleanup defaults to `.idea/workspace.xml`.

`mineKotFormat` stages Kotlin sources, applies one correction pass, snapshots source hashes, and requires a byte-identical second correction pass. It validates each Kotlin/JVM compilation independently with its original classpath, compiler options/plugins, generated-source dependencies, and associated staged outputs before applying changes. Staged compiler tasks resolve classpaths only when executed and replace original associated-compilation outputs with staged outputs, so task discovery and staged validation do not schedule original compile or KSP tasks. `mineKotAssistCompileStaged` uses the same model. Failed validation leaves repository sources unchanged, and apply rejects concurrent repository edits using pre-stage byte hashes. Kotlin Multiplatform projects are rejected explicitly until target-specific staged runners exist.

KSP 2.3.10 tasks are cloned automatically with staged source roots and isolated Kotlin/class outputs. Staged KSP caching is disabled. Java generation and path-like processor options fail closed because Java compilation and external processor outputs are not safely isolated; supported processors emit through KSP `CodeGenerator`. Other source-derived generators require explicit original, formatter, and assisted output contracts; unregistered generators reading repository Kotlin fail before compilation. Staged output properties must retain producer-task providers whose declared inputs point into matching staging roots.

```kotlin
minekotToolchain {
    stagedCompilation {
        generators.register("schema") {
            compilationName.set("main")
            originalKotlinOutput.set(generatedSchema)
            formatKotlinOutput.set(generateFormatSchema.map { formatSchemaOutput.get() })
            assistKotlinOutput.set(generateAssistSchema.map { assistSchemaOutput.get() })
        }
    }
}
```

`check` depends only on deterministic verification. It does not require a semantic-review file or other human approval receipt. Lint verification includes plain build-script analysis plus compilation-aware `detektMain` and `detektTest` tasks so symbol-based rules run with source-set classpaths.

`mineKotSmokeTest` aggregates the consumer project's `check` and raw codestyle verification tasks. It does not independently run the plugin's TestKit suite or write codestyle files.

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

- `minekot-kt-common`: `keys-results-validation`, `parsing-boilerplate`, `typed-result-boundaries`; typed IDs/keys, validation, safe UUID/enum parsing, Result collection/failure mapping, and typed cancellation-safe exception boundaries.
- `minekot-kt-io`: `paths-resources`, `path-url-boilerplate`; UTF-8 text/lines/bytes, atomic writes, safe paths, recursive delete, URL class loaders, missing-only file/resource copies.
- `minekot-kt-serialization`: `json-config`, `json-migrations`; strict/lenient JSON defaults, config load/create, typed JSON fields, migration chains, JSON Result helpers.
- `minekot-kt-coroutines`: `coroutine-results`, `coroutine-lifecycle`; dispatcher helpers, timeout Result, supervised launch, managed scopes, retry, bounded parallel map, Result flows.
- `minekot-kt-atomic`: `atomic-state`, `atomic-gates`; atomic references, once gates, close gates, once values, conditional updates, atomic state holder.
- `minekot-kt-reflection`: `reflection-access`, `reflection-services`; property lookup/maps, named primary-constructor calls, annotations, sealed subclass lookup, ServiceLoader helpers.
- `minekot-kt-testing`: `test-fixtures`; Result assertions, temp project cleanup, path assertions, Gradle fixture files.
- `minekot-codegen-core`: `kotlinpoet-models`, `kotlinpoet-shapes`, `kotlinpoet-expressions`; KotlinPoet-safe names, generated headers, object/data-class/enum/sealed-interface render models, and typed expression/statement trees for source bodies.
- `minekot-ksp-helpers`: `ksp-symbols`; type-safe annotation, argument, class, and symbol readers plus validation and file writes.
- `minekot-adv-common`: `component-basics`, `component-layout`; component builders, word/line joins, key/value rows, bullet lists, plain-text serialization.
- `minekot-adv-minimessage`: `minimessage-templates`, `minimessage-validation`; string/component placeholders, parse Result, tag allowlists, legacy-color rejection.
- `minekot-adv-json`: `adventure-json`; component JSON Result helpers and list round trips.
- `minekot-adv-ansi`: `ansi-console`; ANSI serialization Result and escape stripping.

### Boilerplate replacement

- Replace repeated `try { UUID.fromString(...) }` and `Enum.valueOf(...)` with `toMineKotUuidResult` and `toMineKotEnumResult`.
- Replace module jar URL plumbing with `toMineKotUrlClassLoaderResult`.
- Replace default config creation and version migration glue with `readMineKotConfigResult` and `migrateMineKotJsonConfig`.
- Replace repeated `SupervisorJob() + dispatcher` setup with `mineKotManagedScope`.

## Lint

Bundled Detekt rules are registered through descriptors that record lifecycle disposition, default reporting, correction safety, replacements, and Analysis API requirements. Active rules report unsafe exception boundaries and Result handling, enforce string-template braces and bounded Adventure text/KDoc policy, and cover selected coroutine, value-extraction, receiver-scope, and resolved API preferences. Semantic rules resolve exact supported callable/receiver shapes during full analysis. Heuristic `KotlinxPreference` is deprecated and inactive. Corrections requiring unresolved semantic proof are disabled.

Run:

```shell
./gradlew check
```
