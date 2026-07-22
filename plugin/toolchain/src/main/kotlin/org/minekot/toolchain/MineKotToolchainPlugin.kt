package org.minekot.toolchain

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.JarURLConnection
import java.net.URI

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
        extension.lint.semanticReviewFile.convention(project.rootProject.layout.projectDirectory.file("minekot-review.json"))
        extension.ciCd.fragmentsDirectory.convention(project.rootProject.layout.projectDirectory.dir(".changes"))
        extension.ciCd.changelogFile.convention(project.rootProject.layout.projectDirectory.file("CHANGELOG.md"))
        extension.ciCd.evidenceDirectory.convention(project.rootProject.layout.projectDirectory.dir("docs/releases"))
        extension.ciCd.artifactsDirectory.convention(project.rootProject.layout.buildDirectory.dir("ci/artifacts"))
        extension.ciCd.releaseBundleDirectory.convention(project.rootProject.layout.buildDirectory.dir("ci/release"))
        extension.ciCd.reportDirectory.convention(project.rootProject.layout.buildDirectory.dir("reports/minekot/cicd"))
        configureConventions(project, extension)

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
            "org.jetbrains.kotlin:kotlin-compiler-embeddable:${KotlinVersion.CURRENT}",
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
        project.tasks.register("mineKotReviewPreview", MineKotReviewPreviewTask::class.java) {
            it.group = "minekot"
            it.description = "Writes a source-fingerprinted MineKot semantic-review template."
            it.projectDirectory.set(project.rootProject.layout.projectDirectory)
            it.reportDirectory.set(extension.lint.assistReportDirectory)
        }
        val verifyMineKotSemanticReview =
            project.tasks.register("verifyMineKotSemanticReview", VerifyMineKotSemanticReviewTask::class.java) {
                it.group = "verification"
                it.description = "Verifies current MineKot semantic style-guide confirmation."
                it.projectDirectory.set(project.rootProject.layout.projectDirectory)
                it.reviewFile.set(extension.lint.semanticReviewFile)
                it.maximumAgeDays.set(extension.lint.semanticReviewMaxAgeDays)
            }
        project.tasks.named("check") {
            it.outputs.upToDateWhen { false }
            it.doLast {}
            it.dependsOn(verifyMineKotCodestyle, verifyMineKotSemanticReview)
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
        val assistSourceTree = project.fileTree(
            extension.lint.assistReportDirectory.map { directory ->
                directory.dir("staged")
            },
        ) {
            it.include("src/**/*.kt", "**/src/**/*.kt")
        }
        val assistCompilation = project.tasks.register("mineKotAssistCompileStaged", JavaExec::class.java) { task ->
            task.group = "verification"
            task.description = "Compiles confirmed staged assisted fixes before publication."
            task.dependsOn(assistStage)
            task.classpath(stagedCompilerClasspath)
            task.mainClass.set("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
            val destination = project.layout.buildDirectory.dir("tmp/minekot/assist-classes")
            task.outputs.dir(destination)
            task.doFirst {
                destination.get().asFile.mkdirs()
                task.setArgs(
                    stagedCompilerArguments(
                        assistSourceTree.files,
                        project.configurations.getByName("testCompileClasspath").asPath,
                        destination.get().asFile.absolutePath,
                        extension.build.javaVersion.get(),
                    ),
                )
            }
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
        project.tasks.named("detekt", Detekt::class.java) {
            it.source(project.buildFile)
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
        val stage = project.tasks.register("mineKotFormatStage", MineKotFormatStageTask::class.java) {
            it.group = "minekot"
            it.description = "Stages Kotlin sources for transactional formatting."
            it.projectDirectory.set(project.layout.projectDirectory)
            it.stagingDirectory.set(stagingDirectory)
        }
        val sourceTree = project.fileTree(stagingDirectory) {
            it.include("**/*.kt", "**/*.kts")
        }
        val compilableSourceTree = project.fileTree(stagingDirectory) {
            it.include("src/**/*.kt", "**/src/**/*.kt")
        }
        val firstPass = project.tasks.register("mineKotFormatFirstPass", Detekt::class.java) {
            it.group = "minekot"
            it.description = "Applies first deterministic MineKot correction pass."
            it.dependsOn(stage)
            it.autoCorrect.set(true)
            it.ignoreFailures.set(true)
            it.source(sourceTree)
            it.pluginClasspath.setFrom(project.configurations.getByName("detektPlugins"))
            it.outputs.upToDateWhen { false }
            it.outputs.cacheIf { false }
        }
        val correctionPasses = (2..8).runningFold(firstPass) { previous, number ->
            project.tasks.register(
                if (number == 2) "mineKotFormatSecondPass" else "mineKotFormatConvergencePass${number}",
                Detekt::class.java,
            ) {
                it.group = "minekot"
                it.description = "Applies deterministic MineKot correction convergence pass ${number}."
                it.dependsOn(previous)
                it.autoCorrect.set(true)
                it.ignoreFailures.set(true)
                it.source(sourceTree)
                it.pluginClasspath.setFrom(project.configurations.getByName("detektPlugins"))
                it.outputs.upToDateWhen { false }
                it.outputs.cacheIf { false }
            }
        }
        val convergedCorrections = correctionPasses.last()
        val snapshot = project.tasks.register("mineKotFormatSnapshot", MineKotFormatSnapshotTask::class.java) {
            it.group = "minekot"
            it.description = "Captures MineKot source hashes after correction convergence."
            it.dependsOn(convergedCorrections)
            it.projectDirectory.set(stagingDirectory)
            it.snapshotFile.set(project.layout.buildDirectory.file("tmp/minekot/format-converged.json"))
            it.outputs.upToDateWhen { false }
        }
        val verificationPass = project.tasks.register("mineKotFormatVerificationPass", Detekt::class.java) {
            it.group = "verification"
            it.description = "Verifies MineKot corrections are clean and idempotent."
            it.dependsOn(snapshot)
            it.autoCorrect.set(true)
            it.ignoreFailures.set(false)
            it.source(sourceTree)
            it.pluginClasspath.setFrom(project.configurations.getByName("detektPlugins"))
            it.outputs.upToDateWhen { false }
            it.outputs.cacheIf { false }
        }
        val idempotence = project.tasks.register("mineKotFormatIdempotence", MineKotFormatIdempotenceTask::class.java) {
            it.group = "verification"
            it.description = "Requires byte-identical MineKot formatter verification pass."
            it.dependsOn(verificationPass)
            it.projectDirectory.set(stagingDirectory)
            it.snapshotFile.set(project.layout.buildDirectory.file("tmp/minekot/format-converged.json"))
        }
        val stagedSourceVerification =
            project.tasks.register("mineKotFormatVerifyStagedSources", VerifyMineKotCodestyleTask::class.java) {
                it.group = "verification"
                it.description = "Verifies raw codestyle policy against staged Kotlin sources."
                it.dependsOn(idempotence)
                it.projectDirectory.set(stagingDirectory)
            }
        val stagedCompilation = project.tasks.register("mineKotFormatCompileStaged", JavaExec::class.java) { task ->
            task.group = "verification"
            task.description = "Compiles staged production and test Kotlin sources before publication."
            task.dependsOn(idempotence)
            task.classpath(project.configurations.getByName("mineKotStagedCompiler"))
            task.mainClass.set("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
            val destination = project.layout.buildDirectory.dir("tmp/minekot/format-classes")
            task.outputs.dir(destination)
            task.doFirst {
                destination.get().asFile.mkdirs()
                task.setArgs(
                    stagedCompilerArguments(
                        compilableSourceTree.files,
                        project.configurations.getByName("testCompileClasspath").asPath,
                        destination.get().asFile.absolutePath,
                        project.extensions.getByType(MineKotToolchainExtension::class.java).build.javaVersion.get(),
                    ),
                )
            }
        }
        val apply = project.tasks.register("mineKotFormatApply", MineKotFormatApplyTask::class.java) {
            it.group = "minekot"
            it.description = "Transactionally applies the validated staged formatting result."
            it.dependsOn(stagedSourceVerification, stagedCompilation)
            it.projectDirectory.set(project.layout.projectDirectory)
            it.stagingDirectory.set(stagingDirectory)
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

    private fun stagedCompilerArguments(
        sourceFiles: Set<java.io.File>,
        compilationClasspath: String,
        destination: String,
        javaVersion: Int,
    ): List<String> =
        listOf(
            "-classpath",
            compilationClasspath,
            "-d",
            destination,
            "-jvm-target",
            javaVersion.toString(),
        ) + sourceFiles.map(java.io.File::getAbsolutePath).sorted()

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
