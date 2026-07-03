package org.minekot.toolchain

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class MineKotToolchainPluginTest {
    @Test
    fun `plugin applies and wires enabled dependencies`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                adventure {
                    enabled.set(false)
                }
                lint {
                    enabled.set(false)
                }
            }

            tasks.register("printMineKotDependencies") {
                doLast {
                    configurations.getByName("implementation").dependencies.forEach {
                        println("${'$'}{it.group}:${'$'}{it.name}:${'$'}{it.version}")
                    }
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "printMineKotDependencies")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotDependencies")?.outcome)
        assertTrue(result.output.contains("org.minekot:minekot-kt-common"))
        assertTrue(result.output.contains("org.minekot:minekot-kt-serialization"))
        assertFalse(result.output.contains("org.minekot:minekot-adv-common"))
    }

    @Test
    fun `plugin wires custom library versions and serialization plugin`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                serialization {
                    libraryVersion.set("9.9.9-minekot-test")
                }
                adventure {
                    enabled.set(false)
                }
                lint {
                    enabled.set(false)
                }
            }

            tasks.register("printMineKotSerialization") {
                doLast {
                    println("serializationPlugin=${'$'}{project.plugins.hasPlugin("org.jetbrains.kotlin.plugin.serialization")}")
                    project.configurations.getByName("implementation").dependencies.forEach {
                        println("${'$'}{it.group}:${'$'}{it.name}:${'$'}{it.version}")
                    }
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "printMineKotSerialization")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotSerialization")?.outcome)
        assertTrue(result.output.contains("serializationPlugin=true"))
        assertTrue(result.output.contains("org.jetbrains.kotlinx:kotlinx-serialization-json:9.9.9-minekot-test"))
    }

    @Test
    fun `plugin wires all enabled feature dependencies`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                serialization {
                    libraryVersion.set("1.1.1-minekot-test")
                }
                io {
                    libraryVersion.set("2.2.2-minekot-test")
                }
                coroutines {
                    libraryVersion.set("3.3.3-minekot-test")
                }
                atomic {
                    libraryVersion.set("4.4.4-minekot-test")
                }
                adventure {
                    libraryVersion.set("5.5.5-minekot-test")
                }
                lint {
                    enabled.set(false)
                }
            }

            tasks.register("printMineKotFeatureDependencies") {
                doLast {
                    project.configurations.getByName("implementation").dependencies.forEach {
                        println("${'$'}{it.group}:${'$'}{it.name}:${'$'}{it.version}")
                    }
                    project.configurations.getByName("testImplementation").dependencies.forEach {
                        println("${'$'}{it.group}:${'$'}{it.name}:${'$'}{it.version}")
                    }
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "printMineKotFeatureDependencies")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotFeatureDependencies")?.outcome)
        assertTrue(result.output.contains("org.minekot:minekot-kt-common"))
        assertTrue(result.output.contains("org.minekot:minekot-kt-reflection"))
        assertTrue(result.output.contains("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.1-minekot-test"))
        assertTrue(result.output.contains("org.jetbrains.kotlinx:kotlinx-io-core-jvm:2.2.2-minekot-test"))
        assertTrue(result.output.contains("org.jetbrains.kotlinx:kotlinx-io-bytestring-jvm:2.2.2-minekot-test"))
        assertTrue(result.output.contains("org.jetbrains.kotlinx:kotlinx-coroutines-core:3.3.3-minekot-test"))
        assertTrue(result.output.contains("org.jetbrains.kotlinx:atomicfu-jvm:4.4.4-minekot-test"))
        assertTrue(result.output.contains("net.kyori:adventure-api:5.5.5-minekot-test"))
        assertTrue(result.output.contains("net.kyori:adventure-text-minimessage:5.5.5-minekot-test"))
        assertTrue(result.output.contains("org.minekot:minekot-adv-minimessage"))
        assertTrue(result.output.contains("org.minekot:minekot-kt-testing"))
    }

    @Test
    fun `plugin honors disabled optional feature toggles`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                reflection {
                    enabled.set(false)
                }
                serialization {
                    enabled.set(false)
                }
                io {
                    enabled.set(false)
                }
                coroutines {
                    enabled.set(false)
                }
                atomic {
                    enabled.set(false)
                }
                testing {
                    enabled.set(false)
                }
                adventure {
                    enabled.set(false)
                }
                lint {
                    enabled.set(false)
                }
            }

            tasks.register("printMineKotDisabledFeatures") {
                doLast {
                    println("serializationPlugin=${'$'}{project.plugins.hasPlugin("org.jetbrains.kotlin.plugin.serialization")}")
                    println("detekt=${'$'}{project.plugins.hasPlugin("io.gitlab.arturbosch.detekt")}")
                    project.configurations.getByName("implementation").dependencies.forEach {
                        println("${'$'}{it.group}:${'$'}{it.name}:${'$'}{it.version}")
                    }
                    project.configurations.getByName("testImplementation").dependencies.forEach {
                        println("${'$'}{it.group}:${'$'}{it.name}:${'$'}{it.version}")
                    }
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "printMineKotDisabledFeatures")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotDisabledFeatures")?.outcome)
        assertTrue(result.output.contains("org.minekot:minekot-kt-common"))
        assertTrue(result.output.contains("serializationPlugin=false"))
        assertTrue(result.output.contains("detekt=false"))
        assertFalse(result.output.contains("org.minekot:minekot-kt-reflection"))
        assertFalse(result.output.contains("org.minekot:minekot-kt-serialization"))
        assertFalse(result.output.contains("org.minekot:minekot-kt-io"))
        assertFalse(result.output.contains("org.minekot:minekot-kt-coroutines"))
        assertFalse(result.output.contains("org.minekot:minekot-kt-atomic"))
        assertFalse(result.output.contains("org.minekot:minekot-kt-testing"))
        assertFalse(result.output.contains("org.minekot:minekot-adv-common"))
        assertFalse(result.output.contains("org.jetbrains.kotlinx:kotlinx-serialization-json"))
        assertFalse(result.output.contains("org.jetbrains.kotlinx:kotlinx-io-core-jvm"))
        assertFalse(result.output.contains("org.jetbrains.kotlinx:kotlinx-coroutines-core"))
        assertFalse(result.output.contains("org.jetbrains.kotlinx:atomicfu-jvm"))
        assertFalse(result.output.contains("net.kyori:adventure-api"))
    }

    @Test
    fun `plugin wires build conventions`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                lint {
                    enabled.set(false)
                }
            }

            tasks.register("printMineKotConventions") {
                doLast {
                    val javaExtension = project.extensions.getByType(org.gradle.api.plugins.JavaPluginExtension::class.java)
                    val testTask = project.tasks.named("test").get() as org.gradle.api.tasks.testing.Test
                    println("toolchain=${'$'}{javaExtension.toolchain.languageVersion.get().asInt()}")
                    println("sourcesJar=${'$'}{project.tasks.names.contains("sourcesJar")}")
                    println("javadocJar=${'$'}{project.tasks.names.contains("javadocJar")}")
                    println("junit=${'$'}{testTask.options.javaClass.simpleName}")
                    println("codestyle=${'$'}{project.tasks.names.contains("writeMineKotCodestyle")}")
                    println("projectFiles=${'$'}{project.tasks.names.contains("writeMineKotProjectFiles")}")
                    println("smoke=${'$'}{project.tasks.names.contains("mineKotSmokeTest")}")
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "printMineKotConventions")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotConventions")?.outcome)
        assertTrue(result.output.contains("toolchain=21"))
        assertTrue(result.output.contains("sourcesJar=true"))
        assertTrue(result.output.contains("javadocJar=true"))
        assertTrue(result.output.contains("junit=JUnitPlatformOptions"))
        assertTrue(result.output.contains("codestyle=true"))
        assertTrue(result.output.contains("projectFiles=true"))
        assertTrue(result.output.contains("smoke=true"))
    }

    @Test
    fun `plugin configures kotlin build options`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                build {
                    javaVersion.set(17)
                    allWarningsAsErrors.set(true)
                    contextParameters.set(false)
                }
                lint {
                    enabled.set(false)
                }
            }

            tasks.register("printMineKotBuildOptions") {
                doLast {
                    val extension = project.extensions.getByType(org.minekot.toolchain.MineKotToolchainExtension::class.java)
                    val javaExtension = project.extensions.getByType(org.gradle.api.plugins.JavaPluginExtension::class.java)
                    val kotlinCompile = project.tasks.named("compileKotlin").get() as org.jetbrains.kotlin.gradle.tasks.KotlinCompile
                    println("toolchain=${'$'}{javaExtension.toolchain.languageVersion.get().asInt()}")
                    println("jvmTarget=${'$'}{kotlinCompile.compilerOptions.jvmTarget.get().target}")
                    println("allWarningsAsErrors=${'$'}{kotlinCompile.compilerOptions.allWarningsAsErrors.get()}")
                    println("contextParameters=${'$'}{extension.build.contextParameters.get()}")
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "printMineKotBuildOptions")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotBuildOptions")?.outcome)
        assertTrue(result.output.contains("toolchain=17"))
        assertTrue(result.output.contains("jvmTarget=17"))
        assertTrue(result.output.contains("allWarningsAsErrors=true"))
        assertTrue(result.output.contains("contextParameters=false"))
    }

    @Test
    fun `plugin wires smoke verification dependencies`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                lint {
                    enabled.set(false)
                }
            }

            tasks.register("printMineKotSmokeDependencies") {
                doLast {
                    val smoke = project.tasks.named("mineKotSmokeTest").get()
                    smoke.taskDependencies.getDependencies(smoke).forEach {
                        println("dependency=${'$'}{it.name}")
                    }
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "printMineKotSmokeDependencies")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotSmokeDependencies")?.outcome)
        assertTrue(result.output.contains("dependency=check"))
        assertTrue(result.output.contains("dependency=writeMineKotCodestyle"))
    }

    @Test
    fun `plugin configures project repositories`() {
        val projectDirectory = createProject(repositoriesMode = "PREFER_PROJECT")
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                repositories {
                    mavenCentral.set(false)
                    mavenLocal.set(false)
                    minekotReleases.set(true)
                    minekotSnapshots.set(false)
                }
                lint {
                    enabled.set(false)
                }
            }

            tasks.register("printMineKotRepositories") {
                doLast {
                    project.repositories.withType(org.gradle.api.artifacts.repositories.MavenArtifactRepository::class.java).forEach {
                        println("${'$'}{it.name}:${'$'}{it.url}")
                    }
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "printMineKotRepositories")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotRepositories")?.outcome)
        assertTrue(result.output.contains("minekotReleases"))
        assertTrue(result.output.contains("https://maven.minekot.org/releases"))
        assertFalse(result.output.contains("minekotSnapshots"))
        assertFalse(result.output.contains("MavenLocal"))
        assertFalse(result.output.contains("MavenRepo"))
    }

    @Test
    fun `plugin configures publishing`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            version = "1.2.3-SNAPSHOT"

            minekotToolchain {
                publishing {
                    staticRepositoryDirectory.set(layout.buildDirectory.dir("static-repo"))
                }
                lint {
                    enabled.set(false)
                }
            }

            tasks.register("printMineKotPublishing") {
                doLast {
                    val publishing = project.extensions.getByType(org.gradle.api.publish.PublishingExtension::class.java)
                    println("mavenPublish=${'$'}{project.plugins.hasPlugin("maven-publish")}")
                    publishing.publications.forEach {
                        println("publication=${'$'}{it.name}")
                    }
                    publishing.repositories.forEach {
                        println("repository=${'$'}{it.name}")
                    }
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "printMineKotPublishing")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotPublishing")?.outcome)
        assertTrue(result.output.contains("mavenPublish=true"))
        assertTrue(result.output.contains("publication=mavenJava"))
        assertTrue(result.output.contains("repository=minekot"))
        assertTrue(result.output.contains("repository=static"))
    }

    @Test
    fun `plugin skips publishing when disabled`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                publishing {
                    enabled.set(false)
                }
                lint {
                    enabled.set(false)
                }
            }

            tasks.register("printMineKotNoPublishing") {
                doLast {
                    println("mavenPublish=${'$'}{project.plugins.hasPlugin("maven-publish")}")
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "printMineKotNoPublishing")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotNoPublishing")?.outcome)
        assertTrue(result.output.contains("mavenPublish=false"))
    }

    @Test
    fun `plugin configures static publishing repository only`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                publishing {
                    minekotRepository.set(false)
                    staticRepositoryDirectory.set(layout.buildDirectory.dir("static-repo"))
                }
                lint {
                    enabled.set(false)
                }
            }

            tasks.register("printMineKotStaticPublishing") {
                doLast {
                    val publishing = project.extensions.getByType(org.gradle.api.publish.PublishingExtension::class.java)
                    publishing.repositories.forEach {
                        println("repository=${'$'}{it.name}")
                    }
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "printMineKotStaticPublishing")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotStaticPublishing")?.outcome)
        assertTrue(result.output.contains("repository=static"))
        assertFalse(result.output.contains("repository=minekot"))
    }

    @Test
    fun `plugin configures shadow jar`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                shadow {
                    enabled.set(true)
                    classifier.set("bundle")
                }
                lint {
                    enabled.set(false)
                }
            }

            tasks.register("printMineKotShadow") {
                doLast {
                    val shadowJar = project.tasks.named("shadowJar").get() as com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
                    println("shadow=${'$'}{project.plugins.hasPlugin("com.gradleup.shadow")}")
                    println("classifier=${'$'}{shadowJar.archiveClassifier.get()}")
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "printMineKotShadow")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotShadow")?.outcome)
        assertTrue(result.output.contains("shadow=true"))
        assertTrue(result.output.contains("classifier=bundle"))
    }

    @Test
    fun `plugin honors disabled shadow service merge`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                shadow {
                    enabled.set(true)
                    mergeServiceFiles.set(false)
                }
                lint {
                    enabled.set(false)
                }
            }

            tasks.register("printMineKotShadowMerge") {
                doLast {
                    val shadowJar = project.tasks.named("shadowJar").get() as com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
                    println("transformers=${'$'}{shadowJar.transformers.get().size}")
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "printMineKotShadowMerge")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotShadowMerge")?.outcome)
        assertTrue(result.output.contains("transformers=0"))
    }

    @Test
    fun `plugin wires lint integration`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            tasks.register("printMineKotLint") {
                doLast {
                    println("detekt=${'$'}{project.plugins.hasPlugin("io.gitlab.arturbosch.detekt")}")
                    project.configurations.getByName("detektPlugins").dependencies.forEach {
                        println("${'$'}{it.group}:${'$'}{it.name}:${'$'}{it.version}")
                    }
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "printMineKotLint")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotLint")?.outcome)
        assertTrue(result.output.contains("detekt=true"))
        assertTrue(result.output.contains("org.minekot:minekot-toolchain-lint-rules"))
    }

    @Test
    fun `plugin configures lint options`() {
        val projectDirectory = createProject()
        val configFile = projectDirectory.resolve("minekot-detekt.yml")
        configFile.toFile().writeText("minekot:\n    ForbiddenTryCatch:\n        active: true\n")
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                lint {
                    autoCorrect.set(true)
                    buildUponDefaultConfig.set(false)
                    configFile.set(layout.projectDirectory.file("minekot-detekt.yml"))
                }
            }

            tasks.register("printMineKotLintOptions") {
                doLast {
                    val detekt = project.extensions.getByType(io.gitlab.arturbosch.detekt.extensions.DetektExtension::class.java)
                    println("autoCorrect=${'$'}{detekt.autoCorrect}")
                    println("buildUponDefaultConfig=${'$'}{detekt.buildUponDefaultConfig}")
                    detekt.config.files.forEach {
                        println("config=${'$'}{it.name}")
                    }
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "printMineKotLintOptions")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotLintOptions")?.outcome)
        assertTrue(result.output.contains("autoCorrect=true"))
        assertTrue(result.output.contains("buildUponDefaultConfig=false"))
        assertTrue(result.output.contains("config=minekot-detekt.yml"))
    }

    @Test
    fun `plugin writes codestyle files`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                lint {
                    enabled.set(false)
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "writeMineKotCodestyle")

        assertEquals(TaskOutcome.SUCCESS, result.task(":writeMineKotCodestyle")?.outcome)
        assertTrue(Files.exists(projectDirectory.resolve(".editorconfig")))
        assertTrue(Files.exists(projectDirectory.resolve("config/detekt/minekot.yml")))
        assertTrue(Files.exists(projectDirectory.resolve(".idea/codeStyles/MineKot.xml")))
    }

    @Test
    fun `plugin writes missing project files`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                lint {
                    enabled.set(false)
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "writeMineKotProjectFiles")

        assertEquals(TaskOutcome.SUCCESS, result.task(":writeMineKotProjectFiles")?.outcome)
        assertTrue(Files.exists(projectDirectory.resolve(".gitattributes")))
        assertTrue(Files.exists(projectDirectory.resolve("README.md")))
        assertTrue(Files.exists(projectDirectory.resolve("CHANGELOG.md")))
        assertTrue(Files.exists(projectDirectory.resolve("NOTICE")))
        assertTrue(Files.exists(projectDirectory.resolve("LICENSE")))
    }

    @Test
    fun `plugin initializes project files and codestyle`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                lint {
                    enabled.set(false)
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "mineKotInitializeProject")

        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotInitializeProject")?.outcome)
        assertTrue(Files.exists(projectDirectory.resolve("LICENSE")))
        assertTrue(Files.exists(projectDirectory.resolve(".editorconfig")))
        assertTrue(Files.exists(projectDirectory.resolve("config/detekt/minekot.yml")))
    }

    @Test
    fun `plugin exposes descriptor backed lifecycle task dependencies`() {
        val projectDirectory = createProject()
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                lint {
                    enabled.set(false)
                }
            }

            tasks.register("printMineKotInitializeDependencies") {
                doLast {
                    val initialize = project.tasks.named("mineKotInitializeProject").get()
                    initialize.taskDependencies.getDependencies(initialize).forEach {
                        println("dependency=${'$'}{it.name}")
                    }
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "printMineKotInitializeDependencies")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotInitializeDependencies")?.outcome)
        assertTrue(result.output.contains("dependency=writeMineKotProjectFiles"))
        assertTrue(result.output.contains("dependency=writeMineKotCodestyle"))
    }

    @Test
    fun `descriptors define real codestyle and library entries`() {
        mineKotCodestyleDescriptors.forEach { descriptor ->
            assertNotNull(javaClass.classLoader.getResource(descriptor.resourcePath))
            assertTrue(descriptor.outputPath.isNotBlank())
        }
        assertTrue(mineKotLibraryModuleDescriptors.any { it.artifactId == "minekot-codegen-core" })
        assertTrue(mineKotLibraryModuleDescriptors.any { it.artifactId == "minekot-ksp" })
    }

    @Test
    fun `plugin keeps existing project files`() {
        val projectDirectory = createProject()
        projectDirectory.resolve("README.md").toFile().writeText("keep me")
        writeBuildFile(
            projectDirectory,
            """
            plugins {
                id("org.minekot.toolchain")
            }

            minekotToolchain {
                lint {
                    enabled.set(false)
                }
            }
            """.trimIndent(),
        )

        val result = runGradle(projectDirectory, "writeMineKotProjectFiles")

        assertEquals(TaskOutcome.SUCCESS, result.task(":writeMineKotProjectFiles")?.outcome)
        assertEquals("keep me", projectDirectory.resolve("README.md").toFile().readText())
    }

    private fun createProject(repositoriesMode: String = "FAIL_ON_PROJECT_REPOS"): Path {
        val projectDirectory = Files.createTempDirectory("minekot-toolchain-test")
        projectDirectory.resolve("settings.gradle.kts").toFile().writeText(
            """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                    mavenLocal()
                }
            }
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.${repositoriesMode})
                repositories {
                    mavenCentral()
                    mavenLocal()
                    maven("https://maven.minekot.org/releases")
                    maven("https://maven.minekot.org/snapshots")
                }
            }
            rootProject.name = "smoke"
            """.trimIndent(),
        )
        return projectDirectory
    }

    private fun writeBuildFile(projectDirectory: Path, text: String) {
        projectDirectory.resolve("build.gradle.kts").toFile().writeText(text)
    }

    private fun runGradle(projectDirectory: Path, vararg arguments: String) =
        GradleRunner.create()
            .withProjectDir(projectDirectory.toFile())
            .withPluginClasspath()
            .withArguments(*arguments, "--stacktrace")
            .build()
}
