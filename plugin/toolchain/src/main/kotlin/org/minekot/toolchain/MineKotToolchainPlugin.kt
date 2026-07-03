package org.minekot.toolchain

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
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

        project.pluginManager.apply("java-library")
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        project.extensions.configure(JavaPluginExtension::class.java) {
            it.toolchain.languageVersion.set(JavaLanguageVersion.of(21))
            it.withSourcesJar()
        }
        project.tasks.withType(Test::class.java).configureEach {
            it.useJUnitPlatform()
        }

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
            it.dependsOn("check", "writeMineKotCodestyle")
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
                    repository.url = URI.create("https://maven.minekot.org/releases")
                }
            }
        }
        if (repositories.minekotSnapshots.get()) {
            project.addRepositoryIfAllowed {
                it.maven { repository ->
                    repository.name = "minekotSnapshots"
                    repository.url = URI.create("https://maven.minekot.org/snapshots")
                }
            }
        }
    }

    private fun configureBuild(project: Project, build: BuildFeatureBlock) {
        val javaVersion = build.javaVersion.get()
        project.extensions.configure(JavaPluginExtension::class.java) {
            it.toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
            it.withJavadocJar()
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
                                "https://maven.minekot.org/snapshots"
                            } else {
                                "https://maven.minekot.org/releases"
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
        project.pluginManager.apply("com.gradleup.shadow")
        project.tasks.withType(ShadowJar::class.java).configureEach {
            it.archiveClassifier.set(shadow.classifier)
            if (shadow.mergeServiceFiles.get()) {
                it.mergeServiceFiles()
            }
        }
    }

    private fun configureDependencies(project: Project, extension: MineKotToolchainExtension) {
        val toolchainVersion = javaClass.`package`.implementationVersion ?: "1.0-SNAPSHOT"

        project.dependencies.add("implementation", minekotDependency("minekot-kt-common", toolchainVersion))

        dependencyFeatureDescriptors(project, extension, toolchainVersion).forEach { descriptor ->
            if (descriptor.enabled()) {
                descriptor.beforeAdd()
                project.addDependencies(descriptor.configurationName, *descriptor.notations().toTypedArray())
            }
        }
    }

    private fun configureLint(project: Project, lint: LintFeatureBlock) {
        val toolchainVersion = javaClass.`package`.implementationVersion ?: "1.0-SNAPSHOT"
        project.pluginManager.apply("io.gitlab.arturbosch.detekt")
        project.extensions.configure(DetektExtension::class.java) {
            it.autoCorrect = lint.autoCorrect.get()
            it.buildUponDefaultConfig = lint.buildUponDefaultConfig.get()
            val configFile = lint.configFile.orNull?.asFile
                ?: project.layout.projectDirectory.file("config/detekt/minekot.yml").asFile.takeIf { file -> file.isFile }
            if (configFile != null) {
                it.config.setFrom(configFile)
            }
        }
        project.dependencies.add("detektPlugins", "org.minekot:minekot-toolchain-lint-rules:${toolchainVersion}")
    }

    private fun Project.addDependencies(configurationName: String, vararg notations: String) {
        notations.forEach { notation ->
            dependencies.add(configurationName, notation)
        }
    }

    private fun minekotDependency(moduleName: String, version: String): String =
        "org.minekot:${moduleName}:${version}"

    private fun dependencyFeatureDescriptors(
        project: Project,
        extension: MineKotToolchainExtension,
        toolchainVersion: String,
    ): List<DependencyFeatureDescriptor> = listOf(
        DependencyFeatureDescriptor(
            configurationName = "implementation",
            enabled = { extension.reflection.enabled.get() },
            notations = { listOf(minekotDependency("minekot-kt-reflection", toolchainVersion)) },
        ),
        DependencyFeatureDescriptor(
            configurationName = "implementation",
            enabled = { extension.serialization.enabled.get() },
            beforeAdd = { project.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization") },
            notations = {
                listOf(
                    "org.jetbrains.kotlinx:kotlinx-serialization-json:${extension.serialization.libraryVersion.get()}",
                    minekotDependency("minekot-kt-serialization", toolchainVersion),
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
                    minekotDependency("minekot-kt-io", toolchainVersion),
                )
            },
        ),
        DependencyFeatureDescriptor(
            configurationName = "implementation",
            enabled = { extension.coroutines.enabled.get() },
            notations = {
                listOf(
                    "org.jetbrains.kotlinx:kotlinx-coroutines-core:${extension.coroutines.libraryVersion.get()}",
                    minekotDependency("minekot-kt-coroutines", toolchainVersion),
                )
            },
        ),
        DependencyFeatureDescriptor(
            configurationName = "implementation",
            enabled = { extension.atomic.enabled.get() },
            notations = {
                listOf(
                    "org.jetbrains.kotlinx:atomicfu-jvm:${extension.atomic.libraryVersion.get()}",
                    minekotDependency("minekot-kt-atomic", toolchainVersion),
                )
            },
        ),
        DependencyFeatureDescriptor(
            configurationName = "implementation",
            enabled = { extension.adventure.enabled.get() },
            notations = {
                adventureModules.map { moduleName -> "net.kyori:${moduleName}:${extension.adventure.libraryVersion.get()}" } +
                        minekotAdventureModules.map { moduleName -> minekotDependency(moduleName, toolchainVersion) }
            },
        ),
        DependencyFeatureDescriptor(
            configurationName = "testImplementation",
            enabled = { extension.testing.enabled.get() },
            notations = { listOf(minekotDependency("minekot-kt-testing", toolchainVersion)) },
        ),
    )

    private data class DependencyFeatureDescriptor(
        val configurationName: String,
        val enabled: () -> Boolean,
        val notations: () -> List<String>,
        val beforeAdd: () -> Unit = {},
    )

    private companion object {
        private val adventureModules: List<String> = listOf(
            "adventure-api",
            "adventure-text-serializer-ansi",
            "adventure-text-serializer-gson",
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
}
