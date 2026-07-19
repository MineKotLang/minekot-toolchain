package org.minekot.kotlin.common

/**
 * Public feature metadata for MineKot library modules.
 *
 * @property module Gradle module path.
 * @property feature Feature name.
 * @property publicSymbols Public symbols owned by the feature.
 * @property dependencyReason Reason this feature belongs in the module.
 * @property testClass Test class covering the feature.
 */
data class MineKotLibraryFeatureDescriptor(
    val module: String,
    val feature: String,
    val publicSymbols: List<String>,
    val dependencyReason: String,
    val testClass: String,
)

/**
 * Data-driven registry of MineKot library features.
 */
val mineKotLibraryFeatureDescriptors: List<MineKotLibraryFeatureDescriptor> = listOf(
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:kotlin:minekot-kt-common",
        feature = "keys-results-validation",
        publicSymbols = listOf("MineKotId", "MineKotKey", "collectMineKotResults"),
        dependencyReason = "Common validation and Result helpers have no external dependency.",
        testClass = "org.minekot.kotlin.common.MineKotCommonTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:kotlin:minekot-kt-common",
        feature = "parsing-boilerplate",
        publicSymbols = listOf("MineKotFailure", "toMineKotUuidResult", "toMineKotEnumResult"),
        dependencyReason = "Safe parsing helpers replace repeated UUID and enum try/catch code.",
        testClass = "org.minekot.kotlin.common.MineKotCommonTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:kotlin:minekot-kt-io",
        feature = "paths-resources",
        publicSymbols = listOf("writeMineKotTextAtomic", "copyMineKotDirectoryMissingOnly"),
        dependencyReason = "Path and resource helpers centralize UTF-8 and missing-only IO behavior.",
        testClass = "org.minekot.kotlin.io.MineKotIoTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:kotlin:minekot-kt-io",
        feature = "path-url-boilerplate",
        publicSymbols = listOf("resolveMineKotSafe", "toMineKotUrlClassLoaderResult", "deleteMineKotRecursivelyResult"),
        dependencyReason = "Path safety and URL loader helpers replace repeated platform module loading glue.",
        testClass = "org.minekot.kotlin.io.MineKotIoTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:kotlin:minekot-kt-serialization",
        feature = "json-config",
        publicSymbols = listOf("MineKotJson", "StrictMineKotJson", "readMineKotConfig"),
        dependencyReason = "JSON config helpers belong beside kotlinx.serialization defaults.",
        testClass = "org.minekot.kotlin.serialization.MineKotSerializationTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:kotlin:minekot-kt-serialization",
        feature = "json-migrations",
        publicSymbols = listOf("MineKotJsonMigration", "migrateMineKotJsonConfig", "mineKotString"),
        dependencyReason = "Versioned config and typed field helpers replace repeated JSON object plumbing.",
        testClass = "org.minekot.kotlin.serialization.MineKotSerializationTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:kotlin:minekot-kt-coroutines",
        feature = "coroutine-results",
        publicSymbols = listOf("runMineKotCatching", "mineKotTimeoutResult", "asMineKotResultFlow"),
        dependencyReason = "Coroutine Result helpers depend on kotlinx.coroutines.",
        testClass = "org.minekot.kotlin.coroutines.MineKotCoroutinesTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:kotlin:minekot-kt-coroutines",
        feature = "coroutine-lifecycle",
        publicSymbols = listOf("mineKotManagedScope", "retryMineKot", "mapMineKotParallel"),
        dependencyReason = "Managed scopes and retry helpers replace repeated platform lifecycle coroutine code.",
        testClass = "org.minekot.kotlin.coroutines.MineKotCoroutinesTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:kotlin:minekot-kt-atomic",
        feature = "atomic-state",
        publicSymbols = listOf("mineKotAtomic", "MineKotAtomicState", "openMineKotOnce"),
        dependencyReason = "Atomic helpers depend on kotlinx.atomicfu.",
        testClass = "org.minekot.kotlin.atomic.MineKotAtomicTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:kotlin:minekot-kt-atomic",
        feature = "atomic-gates",
        publicSymbols = listOf("MineKotCloseGate", "MineKotOnceValue", "updateMineKotIf"),
        dependencyReason = "Gate helpers replace repeated close-once and initialize-once state.",
        testClass = "org.minekot.kotlin.atomic.MineKotAtomicTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:kotlin:minekot-kt-reflection",
        feature = "reflection-access",
        publicSymbols = listOf("callPrimaryConstructor", "memberPropertyValue", "mineKotAnnotationResult"),
        dependencyReason = "Reflection helpers depend on kotlin-reflect.",
        testClass = "org.minekot.kotlin.reflection.MineKotReflectionTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:kotlin:minekot-kt-reflection",
        feature = "reflection-services",
        publicSymbols = listOf("loadMineKotServicesResult", "memberPropertyMap", "sealedSubclassNamed"),
        dependencyReason = "ServiceLoader and property map helpers replace repeated discovery glue.",
        testClass = "org.minekot.kotlin.reflection.MineKotReflectionTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:kotlin:minekot-kt-testing",
        feature = "test-fixtures",
        publicSymbols = listOf("assertMineKotSuccess", "useMineKotTestProject", "createMineKotGradleProject"),
        dependencyReason = "Testing helpers depend on JUnit and test libraries.",
        testClass = "org.minekot.kotlin.testing.MineKotTestingTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:adventure:minekot-adv-common",
        feature = "component-basics",
        publicSymbols = listOf("mineKotText", "joinMineKotWords", "toMineKotPlainTextResult"),
        dependencyReason = "Base Adventure component helpers depend on Adventure API.",
        testClass = "org.minekot.adventure.common.MineKotAdventureTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:adventure:minekot-adv-common",
        feature = "component-layout",
        publicSymbols = listOf("mineKotKeyValueComponent", "toMineKotBulletComponent", "orMineKotEmpty"),
        dependencyReason = "Common component layout helpers replace repeated platform message assembly.",
        testClass = "org.minekot.adventure.common.MineKotAdventureTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:adventure:minekot-adv-minimessage",
        feature = "minimessage-templates",
        publicSymbols = listOf("mineKotMiniMessage", "MineKotMiniMessagePlaceholder", "requireMineKotMiniMessageText"),
        dependencyReason = "MiniMessage helpers depend on Adventure MiniMessage.",
        testClass = "org.minekot.adventure.minimessage.MineKotMiniMessageTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:adventure:minekot-adv-minimessage",
        feature = "minimessage-validation",
        publicSymbols = listOf("mineKotPlaceholder", "requireMineKotMiniMessageTags", "escapeMineKotMiniMessage"),
        dependencyReason = "Template validation helpers replace repeated user-facing text checks.",
        testClass = "org.minekot.adventure.minimessage.MineKotMiniMessageTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:adventure:minekot-adv-json",
        feature = "adventure-json",
        publicSymbols = listOf("toMineKotAdventureJson", "mineKotAdventureJsonResult"),
        dependencyReason = "Adventure JSON helpers depend on Adventure JSON serializer.",
        testClass = "org.minekot.adventure.json.MineKotAdventureJsonTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:adventure:minekot-adv-ansi",
        feature = "ansi-console",
        publicSymbols = listOf("toMineKotAnsi", "stripMineKotAnsi"),
        dependencyReason = "ANSI helpers depend on Adventure ANSI serializer.",
        testClass = "org.minekot.adventure.ansi.MineKotAnsiTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:codegen:minekot-codegen-core",
        feature = "kotlinpoet-models",
        publicSymbols = listOf("renderMineKotObject", "renderMineKotDataClass"),
        dependencyReason = "Codegen render models depend on KotlinPoet.",
        testClass = "org.minekot.codegen.core.MineKotCodegenTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:codegen:minekot-codegen-core",
        feature = "kotlinpoet-shapes",
        publicSymbols = listOf("renderMineKotEnum", "renderMineKotSealedInterface", "MineKotGeneratedEnum"),
        dependencyReason = "Common generated shapes replace handwritten string codegen.",
        testClass = "org.minekot.codegen.core.MineKotCodegenTest",
    ),
    MineKotLibraryFeatureDescriptor(
        module = ":libraries:codegen:minekot-ksp-helpers",
        feature = "ksp-symbols",
        publicSymbols = listOf(
            "mineKotAnnotatedClasses",
            "mineKotAnnotation",
            "mineKotArgument",
            "mineKotClassDeclaration",
            "mineKotRequiredArgument",
            "mineKotTypedArgument",
            "isMineKotType",
            "writeMineKotTo",
        ),
        dependencyReason = "KSP helpers depend on KSP APIs and KotlinPoet KSP.",
        testClass = "org.minekot.codegen.ksp.MineKotKspTest",
    ),
)
