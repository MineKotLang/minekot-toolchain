package org.minekot.toolchain

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.detekt.gradle.Detekt
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
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
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
        }
        project.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

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
            it.dependsOn(verifyMineKotCodestyle)
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
        extension.testing.enabled.gradlePropertyConvention(project, "minekotToolchain.testing.enabled", true)
        extension.adventure.configureVersionedConventions(project, "adventure", true, "5.2.0")
        extension.lint.enabled.gradlePropertyConvention(project, "minekotToolchain.lint.enabled", true)
        extension.lint.autoCorrect.gradlePropertyConvention(project, "minekotToolchain.lint.autoCorrect", false)
        extension.lint.buildUponDefaultConfig.gradlePropertyConvention(
            project,
            "minekotToolchain.lint.buildUponDefaultConfig",
            false,
        )
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
                            if (project.version.toString().endsWith("SNAPSHOT")) {
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
            it.source(project.buildFile)
        }
        project.dependencies.add(
            "detektPlugins",
            minekotDependency(
                extension.dependencyGroup.get(),
                "minekot-toolchain-lint-rules",
                extension.toolchainVersion.get(),
            ),
        )
    }

    private fun Project.addDependencies(configurationName: String, vararg notations: String) {
        notations.forEach { notation ->
            dependencies.add(configurationName, notation)
        }
    }

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
        private const val DEFAULT_RELEASES_URL = "https://maven.minekot.org/releases"
        private const val DEFAULT_SNAPSHOTS_URL = "https://maven.minekot.org/snapshots"

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
