package org.minekot.toolchain

import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * MineKot toolchain Gradle extension.
 */
abstract class MineKotToolchainExtension @Inject constructor(objects: ObjectFactory) {
    /**
     * Maven group used for MineKot toolchain dependencies.
     */
    val dependencyGroup: Property<String> = objects.property(String::class.java)

    /**
     * Version used for MineKot toolchain dependencies.
     */
    val toolchainVersion: Property<String> = objects.property(String::class.java)

    /**
     * Kotlin common utilities. Always enabled.
     */
    val common: RequiredFeatureBlock = objects.newInstance(RequiredFeatureBlock::class.java)

    /**
     * Java and Kotlin build conventions.
     */
    val build: BuildFeatureBlock = objects.buildFeatureBlock()

    /**
     * Project repository conventions.
     */
    val repositories: RepositoriesFeatureBlock = objects.repositoriesFeatureBlock()

    /**
     * Maven publishing conventions.
     */
    val publishing: PublishingFeatureBlock = objects.publishingFeatureBlock()

    /**
     * Shadow JAR conventions.
     */
    val shadow: ShadowFeatureBlock = objects.shadowFeatureBlock()

    /**
     * Kotlin reflection utilities.
     */
    val reflection: FeatureBlock = objects.featureBlock(true)

    /**
     * Kotlinx serialization utilities.
     */
    val serialization: VersionedFeatureBlock = objects.versionedFeatureBlock(true, "1.11.0")

    /**
     * Kotlinx IO utilities.
     */
    val io: VersionedFeatureBlock = objects.versionedFeatureBlock(true, "0.9.1")

    /**
     * Kotlin coroutines utilities.
     */
    val coroutines: VersionedFeatureBlock = objects.versionedFeatureBlock(true, "1.11.0")

    /**
     * Atomic utilities.
     */
    val atomic: VersionedFeatureBlock = objects.versionedFeatureBlock(true, "0.33.0")

    /**
     * Code generation and KSP helpers.
     */
    val codegen: FeatureBlock = objects.featureBlock(false)

    /**
     * Test dependencies and helpers.
     */
    val testing: FeatureBlock = objects.featureBlock(true)

    /**
     * Adventure and MiniMessage dependencies.
     */
    val adventure: VersionedFeatureBlock = objects.versionedFeatureBlock(true, "5.2.0")

    /**
     * MineKot Detekt and codestyle integration.
     */
    val lint: LintFeatureBlock = objects.lintFeatureBlock(true)

    /**
     * Root-project CI/CD conventions.
     */
    val ciCd: CiCdFeatureBlock = objects.ciCdFeatureBlock(false)

    /**
     * Configures common utilities.
     */
    fun common(action: Action<in RequiredFeatureBlock>) {
        action.execute(common)
    }

    /**
     * Configures Java and Kotlin build conventions.
     */
    fun build(action: Action<in BuildFeatureBlock>) {
        action.execute(build)
    }

    /**
     * Configures project repositories.
     */
    fun repositories(action: Action<in RepositoriesFeatureBlock>) {
        action.execute(repositories)
    }

    /**
     * Configures Maven publishing.
     */
    fun publishing(action: Action<in PublishingFeatureBlock>) {
        action.execute(publishing)
    }

    /**
     * Configures Shadow JAR.
     */
    fun shadow(action: Action<in ShadowFeatureBlock>) {
        action.execute(shadow)
    }

    /**
     * Configures root-project CI/CD conventions.
     *
     * @param action CI/CD configuration action.
     */
    fun ciCd(action: Action<in CiCdFeatureBlock>) {
        action.execute(ciCd)
    }

    /**
     * Configures reflection utilities.
     */
    fun reflection(action: Action<in FeatureBlock>) {
        action.execute(reflection)
    }

    /**
     * Configures serialization utilities.
     */
    fun serialization(action: Action<in VersionedFeatureBlock>) {
        action.execute(serialization)
    }

    /**
     * Configures IO utilities.
     */
    fun io(action: Action<in VersionedFeatureBlock>) {
        action.execute(io)
    }

    /**
     * Configures coroutine utilities.
     */
    fun coroutines(action: Action<in VersionedFeatureBlock>) {
        action.execute(coroutines)
    }

    /**
     * Configures atomic utilities.
     */
    fun atomic(action: Action<in VersionedFeatureBlock>) {
        action.execute(atomic)
    }

    /**
     * Configures code generation and KSP helpers.
     */
    fun codegen(action: Action<in FeatureBlock>) {
        action.execute(codegen)
    }

    /**
     * Configures testing utilities.
     */
    fun testing(action: Action<in FeatureBlock>) {
        action.execute(testing)
    }

    /**
     * Configures Adventure utilities.
     */
    fun adventure(action: Action<in VersionedFeatureBlock>) {
        action.execute(adventure)
    }

    /**
     * Configures lint integration.
     */
    fun lint(action: Action<in LintFeatureBlock>) {
        action.execute(lint)
    }
}

/**
 * Non-disableable extension block.
 */
abstract class RequiredFeatureBlock

/**
 * Java and Kotlin build convention options.
 */
abstract class BuildFeatureBlock @Inject constructor(objects: ObjectFactory) {
    /**
     * Java toolchain and Kotlin JVM target version.
     */
    val javaVersion: Property<Int> = objects.property(Int::class.java)

    /**
     * Whether Kotlin compiler warnings should fail compilation.
     */
    val allWarningsAsErrors: Property<Boolean> = objects.property(Boolean::class.java)

    /**
     * Whether Kotlin context parameters should be enabled.
     */
    val contextParameters: Property<Boolean> = objects.property(Boolean::class.java)
}

/**
 * Project repository convention options.
 */
abstract class RepositoriesFeatureBlock @Inject constructor(objects: ObjectFactory) {
    /**
     * Whether Maven Central should be added.
     */
    val mavenCentral: Property<Boolean> = objects.property(Boolean::class.java)

    /**
     * Whether Maven Local should be added.
     */
    val mavenLocal: Property<Boolean> = objects.property(Boolean::class.java)

    /**
     * Whether MineKot release repository should be added.
     */
    val minekotReleases: Property<Boolean> = objects.property(Boolean::class.java)

    /**
     * Whether MineKot snapshot repository should be added.
     */
    val minekotSnapshots: Property<Boolean> = objects.property(Boolean::class.java)

    /**
     * MineKot release repository URL.
     */
    val releasesUrl: Property<String> = objects.property(String::class.java)

    /**
     * MineKot snapshot repository URL.
     */
    val snapshotsUrl: Property<String> = objects.property(String::class.java)
}

/**
 * Maven publishing convention options.
 */
abstract class PublishingFeatureBlock @Inject constructor(objects: ObjectFactory) {
    /**
     * Whether Maven publishing should be configured.
     */
    val enabled: Property<Boolean> = objects.property(Boolean::class.java)

    /**
     * Whether the MineKot Maven repository should be added.
     */
    val minekotRepository: Property<Boolean> = objects.property(Boolean::class.java)

    /**
     * Optional static Maven repository output directory.
     */
    val staticRepositoryDirectory: DirectoryProperty = objects.directoryProperty()

    /**
     * MineKot release publishing repository URL.
     */
    val releasesUrl: Property<String> = objects.property(String::class.java)

    /**
     * MineKot snapshot publishing repository URL.
     */
    val snapshotsUrl: Property<String> = objects.property(String::class.java)
}

/**
 * Root-project CI/CD convention options.
 */
abstract class CiCdFeatureBlock @Inject constructor(objects: ObjectFactory) : FeatureBlock(objects) {
    /** Tag prefix used for releases. */
    val tagPrefix: Property<String> = objects.property(String::class.java)

    /** Supported target versions emitted to CI. */
    val supportedVersions: ListProperty<String> = objects.listProperty(String::class.java)

    /** Allowed changelog fragment categories. */
    val changelogCategories: ListProperty<String> = objects.listProperty(String::class.java)

    /** Expected release artifact file names. */
    val expectedArtifacts: ListProperty<String> = objects.listProperty(String::class.java)

    /** Required stable-release evidence checklist identifiers. */
    val requiredEvidenceItems: ListProperty<String> = objects.listProperty(String::class.java)

    /** Project paths whose Maven publications form the supported library set. */
    val publicationProjects: ListProperty<String> = objects.listProperty(String::class.java)

    /** Directory containing unreleased changelog fragments. */
    val fragmentsDirectory: DirectoryProperty = objects.directoryProperty()

    /** Checked-in full changelog file. */
    val changelogFile: RegularFileProperty = objects.fileProperty()

    /** Directory containing checked-in stable-release evidence. */
    val evidenceDirectory: DirectoryProperty = objects.directoryProperty()

    /** Directory containing release artifacts collected by CI. */
    val artifactsDirectory: DirectoryProperty = objects.directoryProperty()

    /** Verified release bundle output directory. */
    val releaseBundleDirectory: DirectoryProperty = objects.directoryProperty()

    /** CI/CD report output directory. */
    val reportDirectory: DirectoryProperty = objects.directoryProperty()
}

/**
 * Shadow JAR convention options.
 */
abstract class ShadowFeatureBlock @Inject constructor(objects: ObjectFactory) {
    /**
     * Whether Shadow JAR should be configured.
     */
    val enabled: Property<Boolean> = objects.property(Boolean::class.java)

    /**
     * Classifier used for the shaded artifact.
     */
    val classifier: Property<String> = objects.property(String::class.java)

    /**
     * Whether service files should be merged.
     */
    val mergeServiceFiles: Property<Boolean> = objects.property(Boolean::class.java)
}

/**
 * Toggleable extension block.
 */
abstract class FeatureBlock @Inject constructor(objects: ObjectFactory) {
    /**
     * Whether this feature should be configured.
     */
    val enabled: Property<Boolean> = objects.property(Boolean::class.java)
}

/**
 * Toggleable extension block with the upstream library version.
 */
abstract class VersionedFeatureBlock @Inject constructor(objects: ObjectFactory) : FeatureBlock(objects) {
    /**
     * Upstream dependency version used by this feature.
     */
    val libraryVersion: Property<String> = objects.property(String::class.java)
}

/**
 * MineKot Detekt lint integration options.
 */
abstract class LintFeatureBlock @Inject constructor(objects: ObjectFactory) : FeatureBlock(objects) {
    /**
     * Whether Detekt should apply auto-corrections when supported.
     */
    val autoCorrect: Property<Boolean> = objects.property(Boolean::class.java)

    /**
     * Whether Detekt should merge MineKot config with Detekt defaults.
     */
    val buildUponDefaultConfig: Property<Boolean> = objects.property(Boolean::class.java)

    /**
     * Optional Detekt config file.
     */
    val configFile: RegularFileProperty = objects.fileProperty()

    /** Optional versioned assisted-fix request document. */
    val assistRequestFile: RegularFileProperty = objects.fileProperty()

    /** Assisted-fix preview report directory. */
    val assistReportDirectory: DirectoryProperty = objects.directoryProperty()

    /** Checked-in semantic style-guide review document. */
    val semanticReviewFile: RegularFileProperty = objects.fileProperty()

    /** Maximum accepted semantic-review age in days. */
    val semanticReviewMaxAgeDays: Property<Int> = objects.property(Int::class.java)
}

private fun ObjectFactory.featureBlock(enabled: Boolean): FeatureBlock =
    newInstance(FeatureBlock::class.java).apply {
        this.enabled.convention(enabled)
    }

private fun ObjectFactory.buildFeatureBlock(): BuildFeatureBlock =
    newInstance(BuildFeatureBlock::class.java).apply {
        javaVersion.convention(21)
        allWarningsAsErrors.convention(false)
        contextParameters.convention(true)
    }

private fun ObjectFactory.repositoriesFeatureBlock(): RepositoriesFeatureBlock =
    newInstance(RepositoriesFeatureBlock::class.java).apply {
        mavenCentral.convention(true)
        mavenLocal.convention(true)
        minekotReleases.convention(true)
        minekotSnapshots.convention(true)
    }

private fun ObjectFactory.publishingFeatureBlock(): PublishingFeatureBlock =
    newInstance(PublishingFeatureBlock::class.java).apply {
        enabled.convention(true)
        minekotRepository.convention(true)
    }

private fun ObjectFactory.shadowFeatureBlock(): ShadowFeatureBlock =
    newInstance(ShadowFeatureBlock::class.java).apply {
        enabled.convention(false)
        classifier.convention("all")
        mergeServiceFiles.convention(true)
    }

private fun ObjectFactory.ciCdFeatureBlock(enabled: Boolean): CiCdFeatureBlock =
    newInstance(CiCdFeatureBlock::class.java).apply {
        this.enabled.convention(enabled)
        tagPrefix.convention("v")
        changelogCategories.convention(
            listOf(
                "breaking",
                "dependency",
                "deprecation",
                "documentation",
                "feature",
                "fix",
                "internal",
                "security",
            ),
        )
    }

private fun ObjectFactory.versionedFeatureBlock(enabled: Boolean, version: String): VersionedFeatureBlock =
    newInstance(VersionedFeatureBlock::class.java).apply {
        this.enabled.convention(enabled)
        libraryVersion.convention(version)
    }

private fun ObjectFactory.lintFeatureBlock(enabled: Boolean): LintFeatureBlock =
    newInstance(LintFeatureBlock::class.java).apply {
        this.enabled.convention(enabled)
        autoCorrect.convention(false)
        buildUponDefaultConfig.convention(false)
        semanticReviewMaxAgeDays.convention(30)
    }
