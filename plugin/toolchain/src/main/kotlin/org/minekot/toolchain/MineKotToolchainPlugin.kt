package org.minekot.toolchain

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.JarURLConnection
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Applies MineKot build, dependency, publishing, and lint conventions.
 */
class MineKotToolchainPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "minekotToolchain",
            MineKotToolchainExtension::class.java,
        )
        extension.lint.assistRequestFile.convention(project.layout.projectDirectory.file("minekot-fixes.json"))
        extension.lint.assistReportDirectory.convention(project.layout.buildDirectory.dir("reports/minekot"))
        extension.ciCd.fragmentsDirectory.convention(project.rootProject.layout.projectDirectory.dir(".changes"))
        extension.ciCd.changelogFile.convention(project.rootProject.layout.projectDirectory.file("CHANGELOG.md"))
        extension.ciCd.evidenceDirectory.convention(project.rootProject.layout.projectDirectory.dir("docs/releases"))
        extension.ciCd.artifactsDirectory.convention(project.rootProject.layout.buildDirectory.dir("ci/artifacts"))
        extension.ciCd.releaseBundleDirectory.convention(project.rootProject.layout.buildDirectory.dir("ci/release"))
        extension.ciCd.reportDirectory.convention(project.rootProject.layout.buildDirectory.dir("reports/minekot/cicd"))
        configureConventions(project, extension)

        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            throw GradleException(
                "MineKot toolchain currently supports Kotlin/JVM projects only; " +
                        "Kotlin Multiplatform staged compilation is not yet supported.",
            )
        }
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("com.gradleup.shadow")

        project.extensions.configure(JavaPluginExtension::class.java) {
            it.toolchain.languageVersion.convention(
                extension.build.javaVersion.map { javaVersion -> JavaLanguageVersion.of(javaVersion) },
            )
            it.withSourcesJar()
        }
        project.tasks.withType(Test::class.java).configureEach {
            it.useJUnitPlatform()
            it.maxParallelForks = Runtime.getRuntime().availableProcessors()
        }
        project.configurations.configureEach {
            it.resolutionStrategy.cacheDynamicVersionsFor(10, "minutes")
            it.resolutionStrategy.cacheChangingModulesFor(0, "seconds")
        }
        project.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        val stagedCompilerClasspath = project.configurations.create("mineKotStagedCompiler") {
            it.isCanBeConsumed = false
            it.isCanBeResolved = true
        }
        project.dependencies.add(
            stagedCompilerClasspath.name,
            "org.jetbrains.kotlin:kotlin-compiler-embeddable:${kotlinCompilerVersion()}",
        )

        val writeMineKotCodestyle =
            project.tasks.register("writeMineKotCodestyle", WriteMineKotCodestyleTask::class.java) {
                it.group = "minekot"
                it.description = "Writes MineKot codestyle templates into the project root."
                it.outputDirectory.set(project.layout.projectDirectory)
            }
        project.tasks.register("writeMineKotProjectFiles", WriteMineKotProjectFilesTask::class.java) {
            it.group = "minekot"
            it.description = "Writes missing MineKot project standard files into the project root."
            it.outputDirectory.set(project.layout.projectDirectory)
        }
        project.tasks.register("mineKotSmokeTest") {
            it.group = "verification"
            it.description = "Runs the MineKot smoke verification task set."
            it.dependsOn("check", "verifyMineKotCodestyle")
        }
        val verifyMineKotCodestyle =
            project.tasks.register("verifyMineKotCodestyle", VerifyMineKotCodestyleTask::class.java) {
                it.group = "verification"
                it.description = "Verifies raw source files and repository-level MineKot codestyle policy."
                it.projectDirectory.set(project.layout.projectDirectory)
            }
        project.tasks.named("check") {
            it.outputs.upToDateWhen { false }
            it.doLast {}
            it.dependsOn(verifyMineKotCodestyle)
        }
        project.tasks.register("mineKotAssistPreview", MineKotAssistPreviewTask::class.java) {
            it.group = "minekot"
            it.description = "Previews fingerprinted MineKot assisted fixes without changing sources."
            it.projectDirectory.set(project.layout.projectDirectory)
            it.requestFile.set(extension.lint.assistRequestFile)
            it.reportDirectory.set(extension.lint.assistReportDirectory)
        }
        val assistStage = project.tasks.register("mineKotAssistStage", MineKotAssistStageTask::class.java) {
            it.group = "minekot"
            it.description = "Stages one explicitly confirmed MineKot assisted-fix plan."
            it.projectDirectory.set(project.layout.projectDirectory)
            it.requestFile.set(extension.lint.assistRequestFile)
            it.reportDirectory.set(extension.lint.assistReportDirectory)
        }
        val assistCompilation = project.tasks.register("mineKotAssistCompileStaged") { task ->
            task.group = "verification"
            task.description = "Compiles each confirmed staged Kotlin/JVM assisted-fix compilation."
            task.dependsOn(assistStage)
        }
        project.tasks.register("mineKotAssistApply", MineKotAssistApplyTask::class.java) {
            it.group = "minekot"
            it.description = "Applies one explicitly confirmed MineKot assisted-fix plan."
            it.dependsOn(assistCompilation)
            it.projectDirectory.set(project.layout.projectDirectory)
            it.requestFile.set(extension.lint.assistRequestFile)
            it.reportDirectory.set(extension.lint.assistReportDirectory)
        }
        project.tasks.register("mineKotFormat") {
            it.group = "minekot"
            it.description = "Applies deterministic MineKot corrections and verifies second-pass idempotence."
        }

        project.tasks.withType(ProcessResources::class.java).configureEach {
            it.mustRunAfter(writeMineKotCodestyle)
        }
        project.tasks.withType(KotlinCompile::class.java).configureEach {
            it.mustRunAfter(writeMineKotCodestyle)
        }
        project.tasks.register("mineKotInitializeProject") {
            it.group = "minekot"
            it.description = "Writes MineKot project and codestyle files into the project root."
            it.dependsOn("writeMineKotProjectFiles", "writeMineKotCodestyle")
        }

        project.afterEvaluate {
            configureRepositories(project, extension.repositories)
            configureBuild(project, extension.build)
            if (extension.publishing.enabled.get()) {
                configurePublishing(project, extension.publishing)
            }
            if (extension.shadow.enabled.get()) {
                configureShadow(project, extension.shadow)
            }
            configureDependencies(project, extension)
            configureMineKotAssistCompilation(project)
            if (extension.lint.enabled.get()) {
                configureLint(project, extension.lint)
            }
            if (extension.ciCd.enabled.get()) {
                configureCiCd(project, extension.ciCd)
            }
        }
    }

    private fun configureConventions(project: Project, extension: MineKotToolchainExtension) {
        val fallbackToolchainVersion = javaClass.`package`.implementationVersion ?: "1.0-SNAPSHOT"

        extension.dependencyGroup.gradlePropertyConvention(project, "minekotToolchain.dependencyGroup", "org.minekot")
        extension.toolchainVersion.gradlePropertyConvention(
            project,
            "minekotToolchain.toolchainVersion",
            fallbackToolchainVersion,
        )

        extension.build.javaVersion.gradlePropertyConvention(project, "minekotToolchain.build.javaVersion", 21)
        extension.build.allWarningsAsErrors.gradlePropertyConvention(
            project,
            "minekotToolchain.build.allWarningsAsErrors",
            false,
        )
        extension.build.contextParameters.gradlePropertyConvention(
            project,
            "minekotToolchain.build.contextParameters",
            true,
        )

        extension.repositories.mavenCentral.gradlePropertyConvention(
            project,
            "minekotToolchain.repositories.mavenCentral",
            true,
        )
        extension.repositories.mavenLocal.gradlePropertyConvention(
            project,
            "minekotToolchain.repositories.mavenLocal",
            true,
        )
        extension.repositories.minekotReleases.gradlePropertyConvention(
            project,
            "minekotToolchain.repositories.minekotReleases",
            true,
        )
        extension.repositories.minekotSnapshots.gradlePropertyConvention(
            project,
            "minekotToolchain.repositories.minekotSnapshots",
            true,
        )
        extension.repositories.releasesUrl.gradlePropertyConvention(
            project,
            "minekotToolchain.repositories.releasesUrl",
            DEFAULT_RELEASES_URL,
        )
        extension.repositories.snapshotsUrl.gradlePropertyConvention(
            project,
            "minekotToolchain.repositories.snapshotsUrl",
            DEFAULT_SNAPSHOTS_URL,
        )

        extension.publishing.enabled.gradlePropertyConvention(project, "minekotToolchain.publishing.enabled", true)
        extension.publishing.minekotRepository.gradlePropertyConvention(
            project,
            "minekotToolchain.publishing.minekotRepository",
            true,
        )
        extension.publishing.releasesUrl.gradlePropertyConvention(
            project,
            "minekotToolchain.publishing.releasesUrl",
            DEFAULT_RELEASES_URL,
        )
        extension.publishing.snapshotsUrl.gradlePropertyConvention(
            project,
            "minekotToolchain.publishing.snapshotsUrl",
            DEFAULT_SNAPSHOTS_URL,
        )

        extension.shadow.enabled.gradlePropertyConvention(project, "minekotToolchain.shadow.enabled", false)
        extension.shadow.classifier.gradlePropertyConvention(project, "minekotToolchain.shadow.classifier", "all")
        extension.shadow.mergeServiceFiles.gradlePropertyConvention(
            project,
            "minekotToolchain.shadow.mergeServiceFiles",
            true,
        )

        extension.reflection.enabled.gradlePropertyConvention(project, "minekotToolchain.reflection.enabled", true)
        extension.serialization.configureVersionedConventions(project, "serialization", true, "1.11.0")
        extension.io.configureVersionedConventions(project, "io", true, "0.9.1")
        extension.coroutines.configureVersionedConventions(project, "coroutines", true, "1.11.0")
        extension.atomic.configureVersionedConventions(project, "atomic", true, "0.33.0")
        extension.codegen.enabled.gradlePropertyConvention(project, "minekotToolchain.codegen.enabled", false)
        extension.testing.enabled.gradlePropertyConvention(project, "minekotToolchain.testing.enabled", true)
        extension.adventure.configureVersionedConventions(project, "adventure", true, "5.2.0")
        extension.lint.enabled.gradlePropertyConvention(project, "minekotToolchain.lint.enabled", true)
        extension.ciCd.enabled.gradlePropertyConvention(project, "minekotToolchain.ciCd.enabled", false)
        extension.ciCd.tagPrefix.gradlePropertyConvention(project, "minekotToolchain.ciCd.tagPrefix", "v")
        extension.lint.autoCorrect.gradlePropertyConvention(project, "minekotToolchain.lint.autoCorrect", false)
        extension.lint.buildUponDefaultConfig.gradlePropertyConvention(
            project,
            "minekotToolchain.lint.buildUponDefaultConfig",
            false,
        )
    }

    private fun configureCiCd(project: Project, ciCd: CiCdFeatureBlock) {
        require(project == project.rootProject) { "MineKot CI/CD can only be enabled on the root project." }
        val verifyChanges =
            project.tasks.register("verifyMineKotChanges", VerifyMineKotChangesTask::class.java) { task ->
                task.group = "verification"
                task.description = "Verifies typed MineKot changelog fragments."
                task.fragmentsDirectory.set(ciCd.fragmentsDirectory)
                task.categories.set(ciCd.changelogCategories)
            }
        project.tasks.named("check") { task -> task.dependsOn(verifyChanges) }
        project.tasks.register("writeMineKotCiMetadata", WriteMineKotCiMetadataTask::class.java) { task ->
            task.group = "minekot"
            task.description = "Writes stable MineKot CI matrix metadata."
            task.supportedVersions.set(ciCd.supportedVersions)
            task.publicationProjects.set(ciCd.publicationProjects)
            task.tagPrefix.set(ciCd.tagPrefix)
            task.outputFile.set(ciCd.reportDirectory.file("metadata.json"))
        }
        project.tasks.register("prepareMineKotRelease", PrepareMineKotReleaseTask::class.java) { task ->
            task.group = "minekot"
            task.description = "Folds changelog fragments into a checked-in release section."
            task.fragmentsDirectory.set(ciCd.fragmentsDirectory)
            task.categories.set(ciCd.changelogCategories)
            task.changelogFile.set(ciCd.changelogFile)
            task.releaseVersion.set(project.providers.gradleProperty("releaseVersion"))
            task.releaseDate.set(project.providers.gradleProperty("releaseDate"))
        }
        project.tasks.register("verifyMineKotRelease", VerifyMineKotReleaseTask::class.java) { task ->
            task.group = "verification"
            task.description = "Verifies MineKot release tag, branch head, and stable evidence."
            task.releaseTag.set(project.providers.gradleProperty("releaseTag"))
            task.releaseVersion.set(project.providers.gradleProperty("releaseVersion"))
            task.releaseCommit.set(project.providers.gradleProperty("releaseCommit"))
            task.protectedBranchCommit.set(project.providers.gradleProperty("protectedBranchCommit"))
            task.tagPrefix.set(ciCd.tagPrefix)
            task.evidenceDirectory.set(ciCd.evidenceDirectory)
            task.requiredEvidenceItems.set(ciCd.requiredEvidenceItems)
        }
        project.tasks.register("assembleMineKotRelease", AssembleMineKotReleaseTask::class.java) { task ->
            task.group = "build"
            task.description = "Creates verified MineKot release bundle and checksums."
            task.artifactsDirectory.set(ciCd.artifactsDirectory)
            task.expectedArtifacts.set(ciCd.expectedArtifacts)
            task.bundleDirectory.set(ciCd.releaseBundleDirectory)
        }
        project.tasks.register(
            "verifyMineKotRemotePublication",
            VerifyMineKotRemotePublicationTask::class.java,
        ) { task ->
            task.group = "verification"
            task.description = "Rejects partial remote MineKot Maven publications."
            task.repositoryUrl.set(project.providers.gradleProperty("mineKotCiCdRepositoryUrl"))
            task.expectedPaths.set(
                project.providers.gradleProperty("mineKotCiCdRemotePaths").map { value ->
                    value.split(',').map(String::trim).filter(String::isNotEmpty)
                },
            )
            task.username.set(project.providers.environmentVariable("MINEKOT_MAVEN_USERNAME"))
            task.password.set(project.providers.environmentVariable("MINEKOT_MAVEN_PASSWORD"))
            task.reportFile.set(ciCd.reportDirectory.file("remote-publication.json"))
        }
        project.tasks.register("stageMineKotPublication") { task ->
            task.group = "publishing"
            task.description = "Stages selected MineKot Maven publications in the configured static repository."
            task.dependsOn(
                ciCd.publicationProjects.map { paths ->
                    paths.map { path -> "${path}:publishMavenJavaPublicationToStaticRepository" }
                },
            )
        }
        project.tasks.register("publishMineKotPublication") { task ->
            task.group = "publishing"
            task.description = "Publishes selected MineKot Maven publications to the MineKot repository."
            task.dependsOn(
                ciCd.publicationProjects.map { paths ->
                    paths.map { path -> "${path}:publishMavenJavaPublicationToMinekotRepository" }
                },
            )
        }
    }

    private fun configureRepositories(project: Project, repositories: RepositoriesFeatureBlock) {
        if (repositories.mavenCentral.get()) {
            project.addRepositoryIfAllowed {
                it.mavenCentral()
            }
        }
        if (repositories.mavenLocal.get()) {
            project.addRepositoryIfAllowed {
                it.mavenLocal()
            }
        }
        if (repositories.minekotReleases.get()) {
            project.addRepositoryIfAllowed {
                it.maven { repository ->
                    repository.name = "minekotReleases"
                    repository.url = URI.create(repositories.releasesUrl.get())
                }
            }
        }
        if (repositories.minekotSnapshots.get()) {
            project.addRepositoryIfAllowed {
                it.maven { repository ->
                    repository.name = "minekotSnapshots"
                    repository.url = URI.create(repositories.snapshotsUrl.get())
                }
            }
        }
    }

    private fun configureBuild(project: Project, build: BuildFeatureBlock) {
        val javaVersion = build.javaVersion.get()
        project.extensions.configure(JavaPluginExtension::class.java) {
            it.withJavadocJar()
        }
        project.tasks.withType(JavaCompile::class.java).configureEach {
            it.options.release.set(javaVersion)
            it.options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
        }
        project.tasks.withType(KotlinCompile::class.java).configureEach {
            it.compilerOptions.jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
            it.compilerOptions.allWarningsAsErrors.set(build.allWarningsAsErrors.get())
        }
    }

    private fun configurePublishing(project: Project, publishing: PublishingFeatureBlock) {
        project.pluginManager.apply("maven-publish")
        project.extensions.configure(PublishingExtension::class.java) {
            if (it.publications.findByName("mavenJava") == null) {
                it.publications.create("mavenJava", MavenPublication::class.java) { publication ->
                    publication.from(project.components.getByName("java"))
                }
            }
            it.repositories { repositories ->
                if (publishing.minekotRepository.get()) {
                    repositories.maven { repository ->
                        repository.name = "minekot"
                        repository.url = URI.create(
                            if (project.version.toString().isMineKotSnapshotVersion()) {
                                publishing.snapshotsUrl.get()
                            } else {
                                publishing.releasesUrl.get()
                            },
                        )
                        repository.credentials { credentials ->
                            credentials.username =
                                project.providers.environmentVariable("MINEKOT_MAVEN_USERNAME").orNull
                            credentials.password =
                                project.providers.environmentVariable("MINEKOT_MAVEN_PASSWORD").orNull
                        }
                    }
                }
                publishing.staticRepositoryDirectory.orNull?.asFile?.let { directory ->
                    repositories.maven { repository ->
                        repository.name = "static"
                        repository.url = directory.toURI()
                    }
                }
            }
        }
    }

    private fun configureShadow(project: Project, shadow: ShadowFeatureBlock) {
        project.tasks.withType(ShadowJar::class.java).configureEach {
            it.archiveClassifier.set(shadow.classifier)
            it.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            it.filesMatching(listOf("META-INF/services/**", "META-INF/*.kotlin_module")) { details ->
                details.duplicatesStrategy = DuplicatesStrategy.INCLUDE
            }
            if (shadow.mergeServiceFiles.get()) {
                it.mergeServiceFiles()
            }
        }
    }

    private fun configureDependencies(project: Project, extension: MineKotToolchainExtension) {
        val dependencyGroup = extension.dependencyGroup.get()
        val toolchainVersion = extension.toolchainVersion.get()

        project.dependencies.add(
            "implementation",
            minekotDependency(dependencyGroup, "minekot-kt-common", toolchainVersion),
        )

        dependencyFeatureDescriptors(project, extension, dependencyGroup, toolchainVersion).forEach { descriptor ->
            if (descriptor.enabled()) {
                descriptor.beforeAdd()
                project.addDependencies(descriptor.configurationName, *descriptor.notations().toTypedArray())
            }
        }
    }

    private fun configureLint(project: Project, lint: LintFeatureBlock) {
        val extension = project.extensions.getByType(MineKotToolchainExtension::class.java)
        project.pluginManager.apply("dev.detekt")
        val javaVersion = extension.build.javaVersion.get()
        project.extensions.configure(DetektExtension::class.java) {
            it.autoCorrect.set(lint.autoCorrect)
            it.buildUponDefaultConfig.set(lint.buildUponDefaultConfig)
            val configFile =
                lint.configFile.orNull?.asFile
                    ?: project.layout.projectDirectory.file("config/detekt/minekot.yml").asFile.takeIf { file ->
                        file.isFile
                    }
            if (configFile != null) {
                it.config.setFrom(configFile)
            }
        }
        project.tasks.withType(Detekt::class.java).configureEach {
            it.jvmTarget.set(javaVersion.toString())
        }
        val buildRoot = project.layout.buildDirectory.get().asFile.toPath().toAbsolutePath().normalize()
        project.tasks.withType(Detekt::class.java).configureEach { task ->
            if (task.name !in stagedFormatDetektTaskNames) {
                // Generated sources must compile, but their producer-owned layout is not handwritten codestyle input.
                task.exclude { details ->
                    details.file.toPath().toAbsolutePath().normalize().startsWith(buildRoot)
                }
            }
        }
        project.tasks.named("detekt", Detekt::class.java) {
            it.source(project.buildFile)
        }
        val fullAnalysisTasks = project.tasks.matching { task -> task.name in fullAnalysisTaskNames }
        project.tasks.named("check").configure { check ->
            check.dependsOn(fullAnalysisTasks)
        }
        project.tasks.withType(DetektCreateBaselineTask::class.java).configureEach {
            it.enabled = false
        }
        project.dependencies.add("detektPlugins", project.files(detektProviderFiles()))
        configureMineKotFormat(project)
    }

    private fun detektProviderFiles(): List<java.io.File> =
        javaClass.classLoader.getResources(DETEKT_PROVIDER_SERVICE)
            .toList()
            .mapNotNull { resource ->
                val connection = resource.openConnection() as? JarURLConnection ?: return@mapNotNull null
                runCatching { java.io.File(connection.jarFileURL.toURI()) }.getOrNull()
            }
            .distinct()

    private fun configureMineKotFormat(project: Project) {
        val stagingDirectory = project.layout.buildDirectory.dir("tmp/minekot/format-sources")
        val baselineFile = project.layout.buildDirectory.file("tmp/minekot/format-baseline.json")
        val stage = project.tasks.register("mineKotFormatStage", MineKotFormatStageTask::class.java) {
            it.group = "minekot"
            it.description = "Stages Kotlin sources for transactional formatting."
            it.projectDirectory.set(project.layout.projectDirectory)
            it.stagingDirectory.set(stagingDirectory)
            it.baselineFile.set(baselineFile)
        }
        val sourceTree = project.fileTree(stagingDirectory) {
            it.include("**/*.kt", "**/*.kts")
        }
        val firstPass = project.tasks.register("mineKotFormatFirstPass", Detekt::class.java) {
            it.group = "minekot"
            it.description = "Applies first deterministic MineKot correction pass."
            it.dependsOn(stage)
            it.autoCorrect.set(true)
            it.ignoreFailures.set(true)
            it.source(sourceTree)
            it.useOriginalSourceDiagnosticPaths(stagingDirectory, project.layout.projectDirectory)
            it.pluginClasspath.setFrom(project.configurations.getByName("detektPlugins"))
            it.outputs.upToDateWhen { false }
            it.outputs.cacheIf { false }
        }
        val snapshotFile = project.layout.buildDirectory.file("tmp/minekot/format-first-pass.json")
        val snapshot = project.tasks.register("mineKotFormatSnapshot", MineKotFormatSnapshotTask::class.java) {
            it.group = "minekot"
            it.description = "Captures MineKot source hashes after the first correction pass."
            it.dependsOn(firstPass)
            it.projectDirectory.set(stagingDirectory)
            it.snapshotFile.set(snapshotFile)
            it.outputs.upToDateWhen { false }
        }
        val secondPass = project.tasks.register("mineKotFormatSecondPass", Detekt::class.java) {
            it.group = "verification"
            it.description = "Applies the second MineKot correction pass for byte-identity verification."
            it.dependsOn(snapshot)
            it.autoCorrect.set(true)
            it.ignoreFailures.set(true)
            it.source(sourceTree)
            it.useOriginalSourceDiagnosticPaths(stagingDirectory, project.layout.projectDirectory)
            it.pluginClasspath.setFrom(project.configurations.getByName("detektPlugins"))
            it.outputs.upToDateWhen { false }
            it.outputs.cacheIf { false }
        }
        val idempotence = project.tasks.register("mineKotFormatIdempotence", MineKotFormatIdempotenceTask::class.java) {
            it.group = "verification"
            it.description = "Requires a byte-identical second MineKot formatter pass."
            it.dependsOn(secondPass)
            it.projectDirectory.set(stagingDirectory)
            it.snapshotFile.set(snapshotFile)
        }
        val stagedSourceVerification =
            project.tasks.register("mineKotFormatVerifyStagedSources", VerifyMineKotCodestyleTask::class.java) {
                it.group = "verification"
                it.description = "Verifies raw codestyle policy against staged Kotlin sources."
                it.dependsOn(idempotence)
                it.projectDirectory.set(stagingDirectory)
            }
        val stagedCompilation = project.tasks.register("mineKotFormatCompileStaged") { task ->
            task.group = "verification"
            task.description = "Compiles each staged Kotlin/JVM compilation before publication."
        }
        registerStagedKotlinCompilationChildren(
            project = project,
            stagingDirectory = stagingDirectory,
            prerequisite = idempotence,
            aggregate = stagedCompilation,
            childTaskPrefix = "mineKotFormatCompileStaged",
            outputDirectoryName = "format-classes",
        )
        val apply = project.tasks.register("mineKotFormatApply", MineKotFormatApplyTask::class.java) {
            it.group = "minekot"
            it.description = "Transactionally applies the validated staged formatting result."
            it.dependsOn(stagedSourceVerification, stagedCompilation)
            it.projectDirectory.set(project.layout.projectDirectory)
            it.stagingDirectory.set(stagingDirectory)
            it.baselineFile.set(baselineFile)
        }
        project.tasks.named("mineKotFormat") {
            it.dependsOn(apply)
        }
    }

    private fun Project.addDependencies(configurationName: String, vararg notations: String) {
        notations.forEach { notation ->
            dependencies.add(configurationName, notation)
        }
    }

    /**
     * Reports staged formatter findings with repository-relative source paths.
     *
     * The staged mirror preserves every repository-relative path. Detekt therefore strips only the disposable mirror
     * prefix, leaving diagnostics such as `docs/cookbook/menu.mkot.kts` that resolve to the original project file.
     */
    private fun Detekt.useOriginalSourceDiagnosticPaths(
        stagingDirectory: Provider<Directory>,
        projectDirectory: Directory,
    ) {
        basePath.set(stagingDirectory.map { directory -> directory.asFile.absolutePath })
        reports.checkstyle.required.set(true)
        logging.captureStandardOutput(LogLevel.DEBUG)
        logging.captureStandardError(LogLevel.DEBUG)
        doLast {
            reportStagedDetektFindings(projectDirectory)
        }
    }

    /** Re-emits Detekt's relative Checkstyle findings without accessing project state during task execution. */
    private fun Detekt.reportStagedDetektFindings(projectDirectory: Directory) {
        val report = reports.checkstyle.outputLocation.get().asFile
        if (!report.isFile) return
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report)
        val files = document.getElementsByTagName("file")
        repeat(files.length) { fileIndex ->
            val file = files.item(fileIndex)
            val relativePath = file.attributes.getNamedItem("name").nodeValue
            val originalPath = projectDirectory.file(relativePath).asFile.absolutePath
            val errors = file.childNodes
            repeat(errors.length) { errorIndex ->
                val error = errors.item(errorIndex)
                if (error.nodeName == "error") {
                    val attributes = error.attributes
                    val line = attributes.getNamedItem("line").nodeValue
                    val column = attributes.getNamedItem("column").nodeValue
                    val message = attributes.getNamedItem("message").nodeValue
                    val rule = attributes.getNamedItem("source").nodeValue.substringAfterLast('.')
                    logger.error("e: ${originalPath}:${line}:${column} ${message} [${rule}]")
                }
            }
        }
    }

    @OptIn(InternalKotlinGradlePluginApi::class)
    private fun configureMineKotAssistCompilation(project: Project) {
        val extension = project.extensions.getByType(MineKotToolchainExtension::class.java)
        registerStagedKotlinCompilationChildren(
            project = project,
            stagingDirectory = extension.lint.assistReportDirectory.map { directory -> directory.dir("staged") },
            prerequisite = project.tasks.named("mineKotAssistStage"),
            aggregate = project.tasks.named("mineKotAssistCompileStaged"),
            childTaskPrefix = "mineKotAssistCompileStaged",
            outputDirectoryName = "assist-classes",
        )
    }

    /**
     * Registers isolated compiler tasks without resolving compilation classpaths during task discovery.
     *
     * Original associated-compilation outputs are removed from each staged classpath before their staged equivalents
     * are added. This prevents Gradle from inferring dependencies on the original compile and generation tasks.
     *
     * @param project Kotlin/JVM project receiving the staged compiler tasks.
     * @param stagingDirectory isolated mirror containing staged repository sources.
     * @param prerequisite task that prepares and validates the staged source mirror.
     * @param aggregate lifecycle task that depends on every staged compiler task.
     * @param childTaskPrefix prefix used for per-compilation task names.
     * @param outputDirectoryName build-directory segment receiving staged compiler outputs.
     */
    @Suppress("DEPRECATION")
    @OptIn(ExperimentalKotlinGradlePluginApi::class, InternalKotlinGradlePluginApi::class)
    private fun registerStagedKotlinCompilationChildren(
        project: Project,
        stagingDirectory: Provider<Directory>,
        prerequisite: TaskProvider<out org.gradle.api.Task>,
        aggregate: TaskProvider<out org.gradle.api.Task>,
        childTaskPrefix: String,
        outputDirectoryName: String,
    ) {
        val kotlin = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
        val compilations = orderKotlinCompilations(kotlin.target.compilations.toList())
        validateStagedGeneratorRegistrations(project, compilations)
        val stagedTasks = mutableMapOf<KotlinCompilation<*>, TaskProvider<MineKotStagedKotlinCompileTask>>()
        compilations.forEach { compilation ->
            val compileTask = project.tasks.named(compilation.compileKotlinTaskName, KotlinCompile::class.java)
            val originalCompile = compileTask.get()
            val compilationName = compilation.name
            val taskSuffix = compilationName.replaceFirstChar(Char::uppercaseChar)
            val destination = project.layout.buildDirectory.dir("tmp/minekot/${outputDirectoryName}/${compilationName}")
            val associatedStagedOutputs = compilation.allAssociatedCompilations.mapNotNull { associated ->
                val staged = stagedTasks[associated] ?: return@mapNotNull null
                val original = project.tasks.named(
                    associated.compileKotlinTaskName,
                    KotlinCompile::class.java,
                ).get()
                original.destinationDirectory.get().asFile.absolutePath to
                        staged.flatMap { stagedTask -> stagedTask.destination }
            }.toMap()
            val kspTaskName = compileTask.name.replaceFirst("compile", "ksp")
            val stagedKsp = if (project.pluginManager.hasPlugin(KSP_PLUGIN_ID)) {
                MineKotKspStaging.register(
                    project = project,
                    compilation = compilation,
                    compileTask = compileTask,
                    stagingDirectory = stagingDirectory,
                    prerequisite = prerequisite,
                    childTaskPrefix = childTaskPrefix,
                    outputDirectoryName = outputDirectoryName,
                    associatedStagedOutputs = associatedStagedOutputs,
                )
            } else {
                null
            }
            val generatorPlan = stagedGeneratorPlan(
                project = project,
                compilationName = compilationName,
                outputDirectoryName = outputDirectoryName,
                originalCompile = originalCompile,
                kspTaskPath = project.tasks.findByName(kspTaskName)?.path,
            )
            val projectRoot = project.layout.projectDirectory.asFile.toPath().toAbsolutePath().normalize()
            val buildRoot = project.layout.buildDirectory.get().asFile.toPath().toAbsolutePath().normalize()
            val stagingRoot = stagingDirectory.get().asFile.toPath().toAbsolutePath().normalize()
            val originalKotlinSourcePaths = originalCompile.sources.files
                .filter { source -> source.isFile && source.extension in setOf("kt", "kts") }
                .map(java.io.File::getAbsolutePath)
                .sorted()
            val replacedDependencyTaskPaths = compilation.allAssociatedCompilations
                .flatMap { associatedCompilation ->
                    associatedCompilation.output.classesDirs.buildDependencies
                        .getDependencies(originalCompile)
                        .map(org.gradle.api.Task::getPath) +
                            listOf(
                                project.tasks.named(associatedCompilation.compileKotlinTaskName).get().path,
                                project.tasks.named(associatedCompilation.compileAllTaskName).get().path,
                            )
                }
                .toSet()
            val sourceMappings = originalCompile.sources.files
                .asSequence()
                .filter(java.io.File::isFile)
                .filter { source -> source.extension in setOf("kt", "kts") }
                .map { source -> source.toPath().toAbsolutePath().normalize() }
                .filter { source -> source.startsWith(projectRoot) && !source.startsWith(buildRoot) }
                .associate { source ->
                    source.toString() to stagingRoot.resolve(projectRoot.relativize(source)).toString()
                }
            val stagedTask = project.tasks.register(
                "${childTaskPrefix}${taskSuffix}",
                MineKotStagedKotlinCompileTask::class.java,
            ) { task ->
                task.group = "verification"
                task.description = "Compiles staged Kotlin ${compilationName} sources."
                task.dependsOn(
                    prerequisite,
                    generatorPlan.retainedProducers,
                    generatorPlan.stagedProducers,
                )
                task.compilerClasspath.from(project.configurations.getByName("mineKotStagedCompiler"))
                task.compilationClasspath.from(
                    project.provider {
                        originalCompile.libraries.files.filter { dependency ->
                            val replacedRoots = (stagedKsp?.originalOutputRoots?.get().orEmpty() +
                                    generatorPlan.originalOutputRoots + associatedStagedOutputs.keys)
                                .map { path -> java.io.File(path).toPath().toAbsolutePath().normalize() }
                            val path = dependency.toPath().toAbsolutePath().normalize()
                            !path.startsWith(buildRoot) && replacedRoots.none(path::startsWith)
                        }
                    },
                )
                task.dependsOn(
                    originalCompile.libraries.buildDependencies.getDependencies(originalCompile)
                        .filterNot { dependency ->
                            dependency == originalCompile || dependency.path in replacedDependencyTaskPaths ||
                                    dependency.path == stagedKsp?.let { project.tasks.named(kspTaskName).get().path }
                        },
                )
                task.compilerArguments.set(
                    project.provider {
                        ArgumentUtils.convertArgumentsToStringList(
                            originalCompile.createCompilerArguments(
                                KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.default,
                            ),
                        )
                    },
                )
                task.sourcePathMappings.set(sourceMappings)
                task.associatedOutputMappings.putAll(generatorPlan.outputMappings)
                task.unsupportedGeneratorProvenance.set(generatorPlan.unsupportedGenerators)
                task.originalKotlinSourcePaths.set(originalKotlinSourcePaths)
                task.compilationKotlinSources.from(sourceMappings.values)
                task.compilationKotlinSources.from(
                    project.provider {
                        val replacedRoots = (stagedKsp?.originalOutputRoots?.get().orEmpty() +
                                generatorPlan.originalOutputRoots + associatedStagedOutputs.keys)
                            .map { path -> java.io.File(path).toPath().toAbsolutePath().normalize() }
                        originalCompile.sources.files.filter { source ->
                            val path = source.toPath().toAbsolutePath().normalize()
                            source.isFile && source.extension in setOf("kt", "kts") && path.startsWith(buildRoot) &&
                                    replacedRoots.none(path::startsWith)
                        }
                    },
                )
                generatorPlan.stagedOutputs.forEach { stagedOutput ->
                    task.compilationKotlinSources.from(
                        project.fileTree(stagedOutput) { files -> files.include("**/*.kt", "**/*.kts") },
                    )
                }
                if (stagedKsp != null) {
                    task.dependsOn(stagedKsp.task)
                    task.associatedOutputMappings.putAll(stagedKsp.outputMappings)
                    task.compilationClasspath.from(stagedKsp.classOutput)
                    task.unsupportedGeneratedJavaSources.from(
                        project.fileTree(stagedKsp.javaOutput) { files -> files.include("**/*.java") },
                    )
                    task.compilationKotlinSources.from(
                        project.fileTree(stagedKsp.kotlinOutput) { files -> files.include("**/*.kt", "**/*.kts") },
                    )
                }
                task.originalDestinationPath.set(
                    originalCompile.destinationDirectory.get().asFile.absolutePath,
                )
                task.destination.set(destination)
            }
            stagedTasks[compilation] = stagedTask
            aggregate.configure { task -> task.dependsOn(stagedTask) }
        }
        compilations.forEach { compilation ->
            val stagedTask = stagedTasks.getValue(compilation)
            compilation.allAssociatedCompilations.forEach { associatedCompilation ->
                val associatedStagedTask = stagedTasks[associatedCompilation] ?: return@forEach
                val associatedCompileTask = project.tasks.named(
                    associatedCompilation.compileKotlinTaskName,
                    KotlinCompile::class.java,
                )
                val originalAssociatedCompile = project.providers.provider { associatedCompileTask.get() }
                stagedTask.configure { task ->
                    task.dependsOn(associatedStagedTask)
                    task.compilationClasspath.from(associatedStagedTask.flatMap { staged -> staged.destination })
                    task.associatedOutputMappings.putAll(
                        originalAssociatedCompile.zip(associatedStagedTask) { original, staged ->
                            mapOf(
                                original.destinationDirectory.get().asFile.absolutePath to
                                        staged.destination.get().asFile.absolutePath,
                            )
                        },
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    private fun orderKotlinCompilations(
        compilations: List<KotlinCompilation<*>>,
    ): List<KotlinCompilation<*>> {
        val ordered = mutableListOf<KotlinCompilation<*>>()
        val visiting = mutableSetOf<KotlinCompilation<*>>()
        val visited = mutableSetOf<KotlinCompilation<*>>()
        fun visit(compilation: KotlinCompilation<*>) {
            if (compilation in visited) return
            require(visiting.add(compilation)) {
                "Cyclic Kotlin compilation association at ${compilation.name}."
            }
            compilation.allAssociatedCompilations
                .filter(compilations::contains)
                .forEach(::visit)
            visiting.remove(compilation)
            visited += compilation
            ordered += compilation
        }
        compilations.forEach(::visit)
        return ordered
    }

    private fun validateStagedGeneratorRegistrations(
        project: Project,
        compilations: List<KotlinCompilation<*>>,
    ) {
        val compilationNames = compilations.mapTo(mutableSetOf()) { compilation -> compilation.name }
        val extension = project.extensions.getByType(MineKotToolchainExtension::class.java)
        extension.stagedCompilation.generators.forEach { generator ->
            require(generator.compilationName.isPresent) {
                "Staged Kotlin generator ${generator.name} requires compilationName."
            }
            require(generator.compilationName.get() in compilationNames) {
                "Staged Kotlin generator ${generator.name} references unknown compilation " +
                        "${generator.compilationName.get()}."
            }
            require(generator.originalKotlinOutput.isPresent) {
                "Staged Kotlin generator ${generator.name} requires originalKotlinOutput."
            }
            require(generator.formatKotlinOutput.isPresent && generator.assistKotlinOutput.isPresent) {
                "Staged Kotlin generator ${generator.name} requires formatKotlinOutput and assistKotlinOutput."
            }
        }
    }

    private fun stagedGeneratorPlan(
        project: Project,
        compilationName: String,
        outputDirectoryName: String,
        originalCompile: KotlinCompile,
        kspTaskPath: String?,
    ): StagedGeneratorPlan {
        val extension = project.extensions.getByType(MineKotToolchainExtension::class.java)
        val buildRoot = project.layout.buildDirectory.get().asFile.toPath().toAbsolutePath().normalize()
        val isolatedRoot = buildRoot.resolve("tmp/minekot")
        val registrations = extension.stagedCompilation.generators
            .filter { generator -> generator.compilationName.orNull == compilationName }
        val originalOutputRoots = registrations.map { generator ->
            generator.originalKotlinOutput.get().asFile.absolutePath
        }
        requireNoOverlappingPaths(originalOutputRoots, "original", compilationName)
        val outputMappings = registrations.associate { generator ->
            val stagedOutput = if (outputDirectoryName.startsWith("format")) {
                generator.formatKotlinOutput
            } else {
                generator.assistKotlinOutput
            }
            val formatInputRoot = buildRoot.resolve("tmp/minekot/format-sources")
            val assistInputRoot = extension.lint.assistReportDirectory.get().asFile.toPath()
                .toAbsolutePath()
                .normalize()
                .resolve("staged")
            listOf(
                generator.formatKotlinOutput to formatInputRoot,
                generator.assistKotlinOutput to assistInputRoot,
            ).forEach { (output, expectedInputRoot) ->
                val path = output.get().asFile.toPath().toAbsolutePath().normalize()
                require(path.startsWith(isolatedRoot)) {
                    "Staged Kotlin generator ${generator.name} output must be under ${isolatedRoot}."
                }
                val producers = project.files(output).buildDependencies.getDependencies(originalCompile)
                require(producers.isNotEmpty()) {
                    "Staged Kotlin generator ${generator.name} output must have a producer task."
                }
                require(
                    producers.all { producer ->
                        producer.outputs.files.files.all { producerOutput ->
                            producerOutput.toPath().toAbsolutePath().normalize().startsWith(isolatedRoot)
                        }
                    },
                ) {
                    "Staged Kotlin generator ${generator.name} producer outputs must remain under ${isolatedRoot}."
                }
                require(
                    producers.any { producer ->
                        producer.outputs.files.files.any { producerOutput ->
                            val producerPath = producerOutput.toPath().toAbsolutePath().normalize()
                            path.startsWith(producerPath) || producerPath.startsWith(path)
                        }
                    },
                ) {
                    "Staged Kotlin generator ${generator.name} producer does not own ${path}."
                }
                require(
                    producers.any { producer ->
                        producer.inputs.files.files.any { input ->
                            input.toPath().toAbsolutePath().normalize().startsWith(expectedInputRoot)
                        }
                    },
                ) {
                    "Staged Kotlin generator ${generator.name} producer must declare input under ${expectedInputRoot}."
                }
            }
            generator.originalKotlinOutput.get().asFile.absolutePath to stagedOutput.get().asFile.absolutePath
        }
        val sourceProducers = originalCompile.sources.buildDependencies.getDependencies(originalCompile)
            .filterNot { producer -> producer == originalCompile || producer.path == kspTaskPath }
        registrations.forEach { generator ->
            val originalRoot = generator.originalKotlinOutput.get().asFile.toPath().toAbsolutePath().normalize()
            val owned = sourceProducers.any { producer ->
                producer.outputs.files.files.any { output ->
                    val path = output.toPath().toAbsolutePath().normalize()
                    originalRoot.startsWith(path) || path.startsWith(originalRoot)
                }
            }
            require(owned) {
                "Staged Kotlin generator ${generator.name} original output is not produced for " +
                        "compilation ${compilationName}."
            }
        }
        requireNoOverlappingPaths(
            registrations.flatMap { generator ->
                listOf(generator.formatKotlinOutput, generator.assistKotlinOutput)
                    .map { output -> output.get().asFile.absolutePath }
            },
            "staged",
            compilationName,
        )
        val originalRootPaths = originalOutputRoots.map { path ->
            java.io.File(path).toPath().toAbsolutePath().normalize()
        }
        val retainedProducers = mutableListOf<org.gradle.api.Task>()
        val unsupported = mutableListOf<String>()
        sourceProducers.forEach { producer ->
            val producerOutputs = producer.outputs.files.files.map { output ->
                output.toPath().toAbsolutePath().normalize()
            }
            val registered = producerOutputs.any { output ->
                originalRootPaths.any { root -> root.startsWith(output) || output.startsWith(root) }
            }
            if (!registered) {
                val repositoryKotlinInputs = producer.inputs.files.files.filter { input ->
                    val path = input.toPath().toAbsolutePath().normalize()
                    input.isFile && input.extension in setOf("kt", "kts") &&
                            path.startsWith(
                                project.layout.projectDirectory.asFile.toPath().toAbsolutePath().normalize(),
                            ) &&
                            !path.startsWith(buildRoot)
                }
                if (repositoryKotlinInputs.isEmpty()) {
                    retainedProducers += producer
                } else {
                    unsupported += "${compilationName}: ${producer.path} reads repository Kotlin; register " +
                            "minekotToolchain.stagedCompilation.generators.${producer.name} with isolated format/assist outputs"
                }
            }
        }
        return StagedGeneratorPlan(
            originalOutputRoots = originalOutputRoots,
            outputMappings = outputMappings,
            stagedOutputs = registrations.map { generator ->
                if (outputDirectoryName.startsWith("format")) {
                    generator.formatKotlinOutput
                } else {
                    generator.assistKotlinOutput
                }
            },
            retainedProducers = retainedProducers,
            stagedProducers = registrations.flatMap { generator ->
                val stagedOutput = if (outputDirectoryName.startsWith("format")) {
                    generator.formatKotlinOutput
                } else {
                    generator.assistKotlinOutput
                }
                project.files(stagedOutput).buildDependencies.getDependencies(originalCompile)
            },
            unsupportedGenerators = unsupported,
        )
    }

    private fun requireNoOverlappingPaths(paths: List<String>, kind: String, compilationName: String) {
        val normalized = paths.map { path -> java.io.File(path).toPath().toAbsolutePath().normalize() }
        val overlaps = normalized.indices.any { first ->
            normalized.indices.any { second ->
                first != second &&
                        (normalized[first].startsWith(normalized[second]) || normalized[second].startsWith(normalized[first]))
            }
        }
        require(!overlaps) {
            "Staged Kotlin generator ${kind} outputs overlap for compilation ${compilationName}."
        }
    }

    private data class StagedGeneratorPlan(
        val originalOutputRoots: List<String>,
        val outputMappings: Map<String, String>,
        val stagedOutputs: List<Provider<Directory>>,
        val retainedProducers: List<org.gradle.api.Task>,
        val stagedProducers: List<org.gradle.api.Task>,
        val unsupportedGenerators: List<String>,
    )

    private fun kotlinCompilerVersion(): String =
        requireNotNull(KotlinCompile::class.java.`package`.implementationVersion) {
            "Kotlin Gradle plugin does not expose its implementation version."
        }.substringBefore("-release")

    private fun minekotDependency(group: String, moduleName: String, version: String): String =
        "${group}:${moduleName}:${version}"

    private fun dependencyFeatureDescriptors(
        project: Project,
        extension: MineKotToolchainExtension,
        dependencyGroup: String,
        toolchainVersion: String,
    ): List<DependencyFeatureDescriptor> = listOf(
        DependencyFeatureDescriptor(
            configurationName = "implementation",
            enabled = { extension.reflection.enabled.get() },
            notations = { listOf(minekotDependency(dependencyGroup, "minekot-kt-reflection", toolchainVersion)) },
        ),
        DependencyFeatureDescriptor(
            configurationName = "implementation",
            enabled = { extension.serialization.enabled.get() },
            notations = {
                listOf(
                    "org.jetbrains.kotlinx:kotlinx-serialization-json:${extension.serialization.libraryVersion.get()}",
                    minekotDependency(dependencyGroup, "minekot-kt-serialization", toolchainVersion),
                )
            },
        ),
        DependencyFeatureDescriptor(
            configurationName = "implementation",
            enabled = { extension.io.enabled.get() },
            notations = {
                listOf(
                    "org.jetbrains.kotlinx:kotlinx-io-core-jvm:${extension.io.libraryVersion.get()}",
                    "org.jetbrains.kotlinx:kotlinx-io-bytestring-jvm:${extension.io.libraryVersion.get()}",
                    minekotDependency(dependencyGroup, "minekot-kt-io", toolchainVersion),
                )
            },
        ),
        DependencyFeatureDescriptor(
            configurationName = "implementation",
            enabled = { extension.coroutines.enabled.get() },
            notations = {
                listOf(
                    "org.jetbrains.kotlinx:kotlinx-coroutines-core:${extension.coroutines.libraryVersion.get()}",
                    minekotDependency(dependencyGroup, "minekot-kt-coroutines", toolchainVersion),
                )
            },
        ),
        DependencyFeatureDescriptor(
            configurationName = "implementation",
            enabled = { extension.atomic.enabled.get() },
            notations = {
                listOf(
                    "org.jetbrains.kotlinx:atomicfu-jvm:${extension.atomic.libraryVersion.get()}",
                    minekotDependency(dependencyGroup, "minekot-kt-atomic", toolchainVersion),
                )
            },
        ),
        DependencyFeatureDescriptor(
            configurationName = "implementation",
            enabled = { extension.codegen.enabled.get() },
            notations = { listOf(minekotDependency(dependencyGroup, "minekot-ksp-helpers", toolchainVersion)) },
        ),
        DependencyFeatureDescriptor(
            configurationName = "implementation",
            enabled = { extension.adventure.enabled.get() },
            notations = {
                adventureModules.map { moduleName ->
                    "net.kyori:${moduleName}:${extension.adventure.libraryVersion.get()}"
                } +
                        minekotAdventureModules.map { moduleName ->
                            minekotDependency(dependencyGroup, moduleName, toolchainVersion)
                        }
            },
        ),
        DependencyFeatureDescriptor(
            configurationName = "testImplementation",
            enabled = { extension.testing.enabled.get() },
            notations = { listOf(minekotDependency(dependencyGroup, "minekot-kt-testing", toolchainVersion)) },
        ),
    )

    private data class DependencyFeatureDescriptor(
        val configurationName: String,
        val enabled: () -> Boolean,
        val notations: () -> List<String>,
        val beforeAdd: () -> Unit = {},
    )

    private companion object {
        private const val DETEKT_PROVIDER_SERVICE: String = "META-INF/services/dev.detekt.api.RuleSetProvider"
        private const val KSP_PLUGIN_ID: String = "com.google.devtools.ksp"
        private val fullAnalysisTaskNames: Set<String> = setOf("detektMain", "detektTest")
        private val stagedFormatDetektTaskNames: Set<String> =
            setOf("mineKotFormatFirstPass", "mineKotFormatSecondPass")
        private const val DEFAULT_RELEASES_URL = "https://maven2.minekot.org/releases"
        private const val DEFAULT_SNAPSHOTS_URL = "https://maven2.minekot.org/snapshots"

        private val adventureModules: List<String> = listOf(
            "adventure-api",
            "adventure-text-serializer-ansi",
            "adventure-text-serializer-json",
            "adventure-text-serializer-legacy",
            "adventure-text-minimessage",
            "adventure-text-serializer-plain",
        )

        private val minekotAdventureModules: List<String> = listOf(
            "minekot-adv-common",
            "minekot-adv-ansi",
            "minekot-adv-json",
            "minekot-adv-minimessage",
        )
    }

    private fun Project.addRepositoryIfAllowed(action: Action<in org.gradle.api.artifacts.dsl.RepositoryHandler>) {
        runCatching {
            action.execute(repositories)
        }
    }

    private fun String.isMineKotSnapshotVersion(): Boolean = endsWith("SNAPSHOT") || "-dev." in this

    private fun VersionedFeatureBlock.configureVersionedConventions(
        project: Project,
        featureName: String,
        enabled: Boolean,
        version: String,
    ) {
        this.enabled.gradlePropertyConvention(project, "minekotToolchain.${featureName}.enabled", enabled)
        this.libraryVersion.gradlePropertyConvention(project, "minekotToolchain.${featureName}.libraryVersion", version)
    }

    private fun Property<String>.gradlePropertyConvention(project: Project, name: String, defaultValue: String) {
        convention(project.providers.gradleProperty(name).orElse(defaultValue))
    }

    private fun Property<Boolean>.gradlePropertyConvention(project: Project, name: String, defaultValue: Boolean) {
        convention(
            project.providers.gradleProperty(name).map { value -> value.toMineKotBooleanProperty(name) }
                .orElse(defaultValue),
        )
    }

    private fun Property<Int>.gradlePropertyConvention(project: Project, name: String, defaultValue: Int) {
        convention(
            project.providers.gradleProperty(name).map { value -> value.toMineKotIntProperty(name) }
                .orElse(defaultValue),
        )
    }

    private fun String.toMineKotBooleanProperty(name: String): Boolean =
        when (lowercase()) {
            "true" -> true
            "false" -> false
            else -> throw GradleException("Gradle property ${name} must be true or false, but was '${this}'.")
        }

    private fun String.toMineKotIntProperty(name: String): Int =
        toIntOrNull() ?: throw GradleException("Gradle property ${name} must be an integer, but was '${this}'.")
}
