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
        writeBuildFixture(projectDirectory, "dependencies-enabled.gradle.kts")

        val result = runGradle(projectDirectory, "printMineKotDependencies")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotDependencies")?.outcome)
        assertTrue(result.output.contains("org.minekot:minekot-kt-common"))
        assertTrue(result.output.contains("org.minekot:minekot-kt-serialization"))
        assertFalse(result.output.contains("org.minekot:minekot-adv-common"))
    }

    @Test
    fun `plugin wires custom library versions and serialization plugin`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "serialization-custom.gradle.kts")

        val result = runGradle(projectDirectory, "printMineKotSerialization")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotSerialization")?.outcome)
        assertTrue(result.output.contains("serializationPlugin=true"))
        assertTrue(result.output.contains("org.jetbrains.kotlinx:kotlinx-serialization-json:9.9.9-minekot-test"))
    }

    @Test
    fun `plugin wires all enabled feature dependencies`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "all-features.gradle.kts")

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
        writeBuildFixture(projectDirectory, "disabled-features.gradle.kts")

        val result = runGradle(projectDirectory, "printMineKotDisabledFeatures")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotDisabledFeatures")?.outcome)
        assertTrue(result.output.contains("org.minekot:minekot-kt-common"))
        assertTrue(result.output.contains("serializationPlugin=true"))
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
        writeBuildFixture(projectDirectory, "conventions.gradle.kts")

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
        writeBuildFixture(projectDirectory, "build-options.gradle.kts")

        val result = runGradle(projectDirectory, "printMineKotBuildOptions")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotBuildOptions")?.outcome)
        assertTrue(result.output.contains("toolchain=17"))
        assertTrue(result.output.contains("jvmTarget=17"))
        assertTrue(result.output.contains("allWarningsAsErrors=true"))
        assertTrue(result.output.contains("contextParameters=false"))
    }

    @Test
    fun `plugin uses gradle properties as conventions`() {
        val projectDirectory = createProject(repositoriesMode = "PREFER_PROJECT")
        writeGradleProperties(
            projectDirectory,
            "minekotToolchain.dependencyGroup=com.example",
            "minekotToolchain.toolchainVersion=9.8.7-test",
            "minekotToolchain.build.javaVersion=17",
            "minekotToolchain.serialization.enabled=false",
            "minekotToolchain.shadow.enabled=true",
            "minekotToolchain.repositories.releasesUrl=https://example.test/releases",
            "minekotToolchain.repositories.snapshotsUrl=https://example.test/snapshots",
        )
        writeBuildFixture(projectDirectory, "properties-conventions.gradle.kts")

        val result = runGradle(projectDirectory, "printMineKotPropertyConventions")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotPropertyConventions")?.outcome)
        assertTrue(result.output.contains("com.example:minekot-kt-common:9.8.7-test"))
        assertTrue(result.output.contains("repository=minekotReleases:https://example.test/releases"))
        assertTrue(result.output.contains("repository=minekotSnapshots:https://example.test/snapshots"))
        assertTrue(result.output.contains("toolchain=17"))
        assertTrue(result.output.contains("serialization=false"))
        assertTrue(result.output.contains("shadow=true"))
    }

    @Test
    fun `plugin reports invalid gradle property values`() {
        val projectDirectory = createProject()
        writeGradleProperties(projectDirectory, "minekotToolchain.build.javaVersion=nope")
        writeBuildFixture(projectDirectory, "default-disabled-lint.gradle.kts")

        val result = runGradleAndFail(projectDirectory, "tasks")

        assertTrue(
            result.output.contains(
                "Gradle property minekotToolchain.build.javaVersion must be an integer, but was 'nope'.",
            ),
        )
    }

    @Test
    fun `plugin wires smoke verification dependencies`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "smoke-dependencies.gradle.kts")

        val result = runGradle(projectDirectory, "printMineKotSmokeDependencies")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotSmokeDependencies")?.outcome)
        assertTrue(result.output.contains("dependency=check"))
        assertFalse(result.output.contains("dependency=writeMineKotCodestyle"))
    }

    @Test
    fun `plugin configures project repositories`() {
        val projectDirectory = createProject(repositoriesMode = "PREFER_PROJECT")
        writeBuildFixture(projectDirectory, "repositories.gradle.kts")

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
        writeBuildFixture(projectDirectory, "publishing.gradle.kts")

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
        writeBuildFixture(projectDirectory, "no-publishing.gradle.kts")

        val result = runGradle(projectDirectory, "printMineKotNoPublishing")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotNoPublishing")?.outcome)
        assertTrue(result.output.contains("mavenPublish=false"))
    }

    @Test
    fun `plugin configures static publishing repository only`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "static-publishing.gradle.kts")

        val result = runGradle(projectDirectory, "printMineKotStaticPublishing")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotStaticPublishing")?.outcome)
        assertTrue(result.output.contains("repository=static"))
        assertFalse(result.output.contains("repository=minekot"))
    }

    @Test
    fun `plugin configures shadow jar`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "shadow.gradle.kts")

        val result = runGradle(projectDirectory, "printMineKotShadow")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotShadow")?.outcome)
        assertTrue(result.output.contains("shadow=true"))
        assertTrue(result.output.contains("classifier=bundle"))
        assertTrue(result.output.contains("duplicates=EXCLUDE"))
    }

    @Test
    fun `plugin honors disabled shadow service merge`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "shadow-merge.gradle.kts")

        val result = runGradle(projectDirectory, "printMineKotShadowMerge")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotShadowMerge")?.outcome)
        assertTrue(result.output.contains("transformers=0"))
    }

    @Test
    fun `plugin wires lint integration`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "lint.gradle.kts")

        val result = runGradle(projectDirectory, "printMineKotLint")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotLint")?.outcome)
        assertTrue(result.output.contains("detekt=true"))
        assertTrue(result.output.contains("buildUponDefaultConfig=false"))
        assertTrue(result.output.contains("org.minekot:minekot-toolchain-lint-rules"))
    }

    @Test
    fun `plugin configures lint options`() {
        val projectDirectory = createProject()
        val configFile = projectDirectory.resolve("minekot-detekt.yml")
        configFile.toFile().writeText("minekot:\n    ForbiddenTryCatch:\n        active: true\n")
        writeBuildFixture(projectDirectory, "lint-options.gradle.kts")

        val result = runGradle(projectDirectory, "printMineKotLintOptions")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotLintOptions")?.outcome)
        assertTrue(result.output.contains("autoCorrect=true"))
        assertTrue(result.output.contains("buildUponDefaultConfig=false"))
        assertTrue(result.output.contains("config=minekot-detekt.yml"))
    }

    @Test
    fun `plugin writes codestyle files`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "default-disabled-lint.gradle.kts")

        val result = runGradle(projectDirectory, "writeMineKotCodestyle")

        assertEquals(TaskOutcome.SUCCESS, result.task(":writeMineKotCodestyle")?.outcome)
        assertTrue(Files.exists(projectDirectory.resolve("config/detekt/minekot.yml")))
        assertTrue(Files.exists(projectDirectory.resolve(".idea/codeStyles/MineKot.xml")))
        assertTrue(Files.exists(projectDirectory.resolve(".idea/workspace.xml")))

        val detektConfig = projectDirectory.resolve("config/detekt/minekot.yml").toFile().readText()
        val configuredMineKotRules = Regex("^    ([A-Z][A-Za-z]+):$", RegexOption.MULTILINE)
            .findAll(detektConfig.substringBefore("\nstyle:"))
            .map { match -> match.groupValues[1] }
            .toSet()
        val activeMineKotRules = setOf(
            "ForbiddenTryCatch",
            "StringTemplateBraces",
            "MiniMessageText",
            "MissingKDoc",
            "CoroutinePreference",
            "MagicNumber",
            "ResultHandling",
            "KotlinxPreference",
            "TrailingComma",
            "ParameterWrapping",
            "ForEachPreference",
            "GradleDslConventions",
            "ImportPolicy",
            "SourceFilePolicy",
            "CommentFormatting",
        )

        assertEquals(activeMineKotRules + "ExplicitScopeInNestedScope", configuredMineKotRules)
        activeMineKotRules.forEach { ruleName ->
            assertTrue(detektConfig.contains("    ${ruleName}:\n        active: true"))
        }
        assertTrue(detektConfig.contains("    ExplicitScopeInNestedScope:\n        active: false"))
        val autoCorrectableMineKotRules = setOf(
            "StringTemplateBraces",
            "TrailingComma",
            "ParameterWrapping",
            "GradleDslConventions",
            "ImportPolicy",
            "SourceFilePolicy",
            "CommentFormatting",
        )
        autoCorrectableMineKotRules.forEach { ruleName ->
            assertTrue(detektConfig.contains("    ${ruleName}:\n        active: true\n        autoCorrect: true"))
        }
        assertTrue(detektConfig.contains("maxLineLength: 120"))
        assertTrue(detektConfig.contains("    NewLineAtEndOfFile:\n        active: true"))
        assertTrue(detektConfig.contains("    NoTabs:\n        active: true"))
        assertTrue(detektConfig.contains("    SpacingAfterPackageAndImports:\n        active: true"))
        assertTrue(detektConfig.contains("    MagicNumber:\n        active: false"))
        assertTrue(detektConfig.contains("    WildcardImport:\n        active: false"))
        assertTrue(detektConfig.contains("    SleepInsteadOfDelay:\n        active: false"))

        val workspace = projectDirectory.resolve(".idea/workspace.xml").toFile().readText()

        assertTrue(workspace.contains("<component name=\"FormatOnSaveOptions\">"))
        assertTrue(workspace.contains("<component name=\"OptimizeOnSaveOptions\">"))
        assertTrue(workspace.contains("\"code.cleanup.on.save\":\"true\""))
        assertTrue(workspace.contains("\"rearrange.code.on.save\":\"true\""))
        assertTrue(workspace.contains("\"settings.editor.selected.configurable\":\"actions.on.save\""))
        assertTrue(workspace.contains("name=\"CHECK_CODE_CLEANUP_BEFORE_PROJECT_COMMIT\" value=\"true\""))
        assertTrue(workspace.contains("name=\"OPTIMIZE_IMPORTS_BEFORE_PROJECT_COMMIT\" value=\"true\""))
        assertTrue(workspace.contains("name=\"REFORMAT_BEFORE_PROJECT_COMMIT\" value=\"true\""))
        assertTrue(workspace.contains("name=\"REARRANGE_BEFORE_PROJECT_COMMIT\" value=\"true\""))
    }

    @Test
    fun `plugin updates existing intellij workspace settings`() {
        val projectDirectory = createProject()
        val workspacePath = writeExistingWorkspaceFile(projectDirectory)
        writeBuildFixture(projectDirectory, "default-disabled-lint.gradle.kts")

        val result = runGradle(projectDirectory, "writeMineKotCodestyle")
        val workspace = workspacePath.toFile().readText()

        assertEquals(TaskOutcome.SUCCESS, result.task(":writeMineKotCodestyle")?.outcome)
        assertTrue(workspace.contains("<component name=\"ExistingComponent\">"))
        assertTrue(workspace.contains("name=\"keepMe\" value=\"true\""))
        assertTrue(workspace.contains("\"existing.key\":\"kept\""))
        assertTrue(workspace.contains("\"code.cleanup.on.save\":\"true\""))
        assertTrue(workspace.contains("name=\"REFORMAT_BEFORE_PROJECT_COMMIT\" value=\"true\""))
    }

    @Test
    fun `plugin writes missing project files`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "default-disabled-lint.gradle.kts")

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
        writeBuildFixture(projectDirectory, "default-disabled-lint.gradle.kts")

        val result = runGradle(projectDirectory, "mineKotInitializeProject")

        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotInitializeProject")?.outcome)
        assertTrue(Files.exists(projectDirectory.resolve("LICENSE")))
        assertTrue(Files.exists(projectDirectory.resolve("config/detekt/minekot.yml")))
        assertTrue(Files.exists(projectDirectory.resolve(".idea/workspace.xml")))
    }

    @Test
    fun `plugin exposes descriptor backed lifecycle task dependencies`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "initialize-dependencies.gradle.kts")

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
        assertTrue(mineKotLibraryModuleDescriptors.any { it.artifactId == "minekot-ksp-helpers" })
    }

    @Test
    fun `codestyle verification accepts compliant repository files`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "default-disabled-lint.gradle.kts")
        writeGradleProperties(
            projectDirectory,
            "org.gradle.caching=true",
            "org.gradle.parallel=true",
        )
        val sourceFile = projectDirectory.resolve("src/main/kotlin/Example.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText("fun main(): Unit = Unit\n")

        val result = runGradle(projectDirectory, "verifyMineKotCodestyle")

        assertEquals(TaskOutcome.SUCCESS, result.task(":verifyMineKotCodestyle")?.outcome)
    }

    @Test
    fun `codestyle verification reports every repository policy violation`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "default-disabled-lint.gradle.kts")
        writeGradleProperties(
            projectDirectory,
            "org.gradle.caching=false",
            "org.gradle.parallel=false",
        )
        val sourceFile = projectDirectory.resolve("src/main/kotlin/Bad.kt")
        Files.createDirectories(sourceFile.parent)
        Files.write(
            sourceFile,
            byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
                    "fun main(): Unit = Unit\r\n".toByteArray(),
        )
        val noFinalNewlineFile = projectDirectory.resolve("src/main/kotlin/NoFinalNewline.kt")
        noFinalNewlineFile.toFile().writeText("val answer: Int = 42")
        val groovyBuildFile = projectDirectory.resolve("legacy/build.gradle")
        Files.createDirectories(groovyBuildFile.parent)
        groovyBuildFile.toFile().writeText("plugins {}\n")
        projectDirectory.resolve("legacy/Helper.groovy").toFile().writeText("class Helper {}\n")

        val result = runGradleAndFail(projectDirectory, "verifyMineKotCodestyle")

        assertEquals(TaskOutcome.FAILED, result.task(":verifyMineKotCodestyle")?.outcome)
        assertTrue(result.output.contains("src/main/kotlin/Bad.kt: remove UTF-8 BOM"))
        assertTrue(result.output.contains("src/main/kotlin/Bad.kt: use LF line endings"))
        assertTrue(result.output.contains("src/main/kotlin/NoFinalNewline.kt: end with exactly one newline"))
        assertTrue(result.output.contains("legacy/build.gradle: Groovy is forbidden; use Kotlin instead"))
        assertTrue(result.output.contains("legacy/Helper.groovy: Groovy is forbidden; use Kotlin instead"))
        assertTrue(result.output.contains("gradle.properties: set org.gradle.caching=true"))
        assertTrue(result.output.contains("gradle.properties: set org.gradle.parallel=true"))
    }

    @Test
    fun `plugin keeps existing project files`() {
        val projectDirectory = createProject()
        projectDirectory.resolve("README.md").toFile().writeText("keep me")
        writeBuildFixture(projectDirectory, "default-disabled-lint.gradle.kts")

        val result = runGradle(projectDirectory, "writeMineKotProjectFiles")

        assertEquals(TaskOutcome.SUCCESS, result.task(":writeMineKotProjectFiles")?.outcome)
        assertEquals("keep me", projectDirectory.resolve("README.md").toFile().readText())
    }

    private fun createProject(repositoriesMode: String = "FAIL_ON_PROJECT_REPOS"): Path {
        val projectDirectory = Files.createTempDirectory("minekot-toolchain-test")
        writeSettingsFile(projectDirectory, repositoriesMode)
        return projectDirectory
    }

    private fun writeSettingsFile(projectDirectory: Path, repositoriesMode: String) {
        val settings = readFixture("settings.gradle.kts.template")
            .replace("__REPOSITORIES_MODE__", repositoriesMode)
        projectDirectory.resolve("settings.gradle.kts").toFile().writeText(settings)
    }

    private fun writeBuildFixture(projectDirectory: Path, fixtureName: String) {
        val fixture = readFixture("build/${fixtureName}")
        projectDirectory.resolve("build.gradle.kts").toFile().writeText(fixture)
    }

    private fun writeGradleProperties(projectDirectory: Path, vararg lines: String) {
        projectDirectory.resolve("gradle.properties").toFile().writeText(lines.joinToString(separator = "\n"))
    }

    private fun writeExistingWorkspaceFile(projectDirectory: Path): Path {
        val workspacePath = projectDirectory.resolve(".idea/workspace.xml")
        Files.createDirectories(workspacePath.parent)
        workspacePath.toFile().writeText(readFixture("existing-workspace.xml"))
        return workspacePath
    }

    private fun runGradle(projectDirectory: Path, vararg arguments: String) =
        GradleRunner.create()
            .withProjectDir(projectDirectory.toFile())
            .withPluginClasspath()
            .withArguments(*arguments, "--stacktrace")
            .build()

    private fun runGradleAndFail(projectDirectory: Path, vararg arguments: String) =
        GradleRunner.create()
            .withProjectDir(projectDirectory.toFile())
            .withPluginClasspath()
            .withArguments(*arguments, "--stacktrace")
            .buildAndFail()

    private fun readFixture(path: String): String {
        return requireNotNull(javaClass.classLoader.getResource("fixtures/${path}")) {
            "Missing test fixture ${path}."
        }.readText()
    }
}
