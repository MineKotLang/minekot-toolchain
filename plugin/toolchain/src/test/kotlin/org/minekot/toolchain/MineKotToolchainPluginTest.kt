package org.minekot.toolchain

import kotlinx.serialization.json.Json
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

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
        assertTrue(result.output.contains("org.minekot:minekot-ksp-helpers"))
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
    fun `plugin disables up to date checks for check tasks`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "default-disabled-lint.gradle.kts")

        runGradle(projectDirectory, "check", "-x", "verifyMineKotCodestyle")
        val result = runGradle(projectDirectory, "check", "-x", "verifyMineKotCodestyle")

        assertEquals(TaskOutcome.SUCCESS, result.task(":check")?.outcome)
    }

    @Test
    fun `plugin exposes deterministic verification tasks only`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "default-disabled-lint.gradle.kts")

        val result = runGradle(projectDirectory, "tasks", "--all")

        assertTrue(result.output.contains("verifyMineKotCodestyle"))
        assertFalse(result.output.contains("mineKotReviewPreview"))
        assertFalse(result.output.contains("verifyMineKotSemanticReview"))
    }

    @Test
    fun `staged compiler registration does not resolve compilation classpaths`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "default-disabled-lint.gradle.kts")
        writeGradleProperties(projectDirectory, "minekotToolchain.toolchainVersion=unpublished-test-version")

        val result = runGradle(projectDirectory, "tasks", "--all")

        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        assertTrue(result.output.contains("mineKotAssistCompileStagedMain"))
    }

    @Test
    fun `check includes full analysis Detekt tasks`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "format.gradle.kts")

        val result = runGradle(projectDirectory, "check", "--dry-run")

        assertTrue(result.output.contains(":detektMain SKIPPED"), result.output)
        assertTrue(result.output.contains(":detektTest SKIPPED"), result.output)
    }

    @Test
    fun `all regular Detekt tasks exclude generated build sources from codestyle findings`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "format.gradle.kts")
        projectDirectory.resolve("build.gradle.kts").toFile().appendText(
            """

            kotlin { sourceSets.test { kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/test/kotlin")) } }
            tasks.register<dev.detekt.gradle.Detekt>("detektGeneratedProbe") {
                source(layout.buildDirectory.dir("generated/ksp/test/kotlin"))
            }
            """.trimIndent() + "\n",
        )
        val generatedSource = projectDirectory.resolve("build/generated/ksp/test/kotlin/Generated.kt")
        Files.createDirectories(generatedSource.parent)
        generatedSource.toFile().writeText(
            "internal fun generated(): Unit {\n  println(\"generated\")\n}\n",
        )
        val handwrittenSource = projectDirectory.resolve("src/test/kotlin/Handwritten.kt")
        Files.createDirectories(handwrittenSource.parent)
        handwrittenSource.toFile().writeText("internal fun handwritten(): Unit = Unit\n")

        val result = runGradle(projectDirectory, "detektTest", "detektGeneratedProbe")

        assertEquals(TaskOutcome.SUCCESS, result.task(":detektTest")?.outcome)
        assertEquals(TaskOutcome.NO_SOURCE, result.task(":detektGeneratedProbe")?.outcome)
        assertFalse(result.output.contains("Unexpected indentation"), result.output)
    }

    @Test
    fun `plugin configures project repositories`() {
        val projectDirectory = createProject(repositoriesMode = "PREFER_PROJECT")
        writeBuildFixture(projectDirectory, "repositories.gradle.kts")

        val result = runGradle(projectDirectory, "printMineKotRepositories")

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMineKotRepositories")?.outcome)
        assertTrue(result.output.contains("minekotReleases"))
        assertTrue(result.output.contains("https://maven2.minekot.org/releases"))
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
        assertTrue(result.output.contains("detektPluginFile=minekot-toolchain-lint-rules"))
    }

    @Test
    fun `plugin disables detekt baseline generation`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "lint.gradle.kts")

        val result = runGradle(projectDirectory, "detektBaseline")

        assertEquals(TaskOutcome.SKIPPED, result.task(":detektBaseline")?.outcome)
        assertFalse(Files.exists(projectDirectory.resolve("detekt-baseline.xml")))
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
        // MineKot YAML style uses two spaces for each mapping level.
        val configuredMineKotRules = Regex("^  ([A-Z][A-Za-z]+):$", RegexOption.MULTILINE)
            .findAll(detektConfig.substringBefore("\nstyle:"))
            .map { match -> match.groupValues[1] }
            .toSet()
        val configuredRuleNames = setOf(
            "ForbiddenTryCatch",
            "StringTemplateBraces",
            "MiniMessageText",
            "MissingKDoc",
            "CoroutinePreference",
            "MagicNumber",
            "ResultHandling",
            "KotlinxPreference",
            "TrailingComma",
            "LineWrapping",
            "ForEachPreference",
            "GradleDslConventions",
            "ImportPolicy",
            "SourceFilePolicy",
            "CommentFormatting",
            "ExplicitScopeInNestedScope",
            "ResolvedApiPreference",
        )
        val inactiveMineKotRules = setOf("ExplicitScopeInNestedScope", "KotlinxPreference")
        val activeMineKotRules = configuredRuleNames - inactiveMineKotRules

        assertEquals(configuredRuleNames, configuredMineKotRules)
        activeMineKotRules.forEach { ruleName ->
            assertTrue(detektConfig.contains("  ${ruleName}:\n    active: true"))
        }
        inactiveMineKotRules.forEach { ruleName ->
            assertTrue(detektConfig.contains("  ${ruleName}:\n    active: false"))
        }
        val autoCorrectableMineKotRules = setOf(
            "StringTemplateBraces",
            "TrailingComma",
            "GradleDslConventions",
            "SourceFilePolicy",
            "CommentFormatting",
            "LineWrapping",
        )
        autoCorrectableMineKotRules.forEach { ruleName ->
            assertTrue(detektConfig.contains("  ${ruleName}:\n    active: true\n    autoCorrect: true"))
        }
        setOf(
            "ResultHandling",
            "KotlinxPreference",
            "ForEachPreference",
            "ImportPolicy",
            "ExplicitScopeInNestedScope",
        ).forEach { ruleName ->
            val ruleConfig = detektConfig.lineSequence()
                .dropWhile { line -> line != "  ${ruleName}:" }
                .drop(1)
                .takeWhile { line -> line.startsWith("    ") }
                .joinToString("\n")
            assertFalse(ruleConfig.contains("autoCorrect: true"))
        }
        assertTrue(detektConfig.contains("maxLineLength: 120"))
        assertTrue(detektConfig.contains("  NewLineAtEndOfFile:\n    active: true"))
        assertTrue(detektConfig.contains("  NoTabs:\n    active: true"))
        assertTrue(detektConfig.contains("  SpacingAfterPackageAndImports:\n    active: true"))
        assertTrue(detektConfig.contains("  MagicNumber:\n    active: false"))
        assertTrue(detektConfig.contains("  WildcardImport:\n    active: false"))
        assertTrue(detektConfig.contains("  SleepInsteadOfDelay:\n    active: false"))
        setOf("Indentation", "NoConsecutiveBlankLines").forEach { ruleName ->
            assertTrue(detektConfig.contains("  ${ruleName}:\n    active: false"))
        }

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
            "org.gradle.configureondemand=true",
        )
        val sourceFile = projectDirectory.resolve("src/main/kotlin/Example.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText("fun main(): Unit = Unit\n")
        val codexMemory = projectDirectory.resolve(".codex/memories/MEMORY.md")
        Files.createDirectories(codexMemory.parent)
        codexMemory.toFile().writeText("# AGENT METADATA\nWrapped\nparagraph.\n")
        val agentMetadata = projectDirectory.resolve(".agents/notes.md")
        Files.createDirectories(agentMetadata.parent)
        agentMetadata.toFile().writeText("# AGENT METADATA\nWrapped\nparagraph.\n")

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
            "org.gradle.configureondemand=false",
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
        val baselineFile = projectDirectory.resolve("config/detekt/MyBaseline.XML")
        Files.createDirectories(baselineFile.parent)
        baselineFile.toFile().writeText("<SmellBaseline/>\n")

        val result = runGradleAndFail(projectDirectory, "verifyMineKotCodestyle")

        assertEquals(TaskOutcome.FAILED, result.task(":verifyMineKotCodestyle")?.outcome)
        assertTrue(result.output.contains("src/main/kotlin/Bad.kt: remove UTF-8 BOM"))
        assertTrue(result.output.contains("src/main/kotlin/Bad.kt: use LF line endings"))
        assertTrue(result.output.contains("src/main/kotlin/NoFinalNewline.kt: end with exactly one newline"))
        assertTrue(result.output.contains("legacy/build.gradle: Groovy is forbidden; use Kotlin instead"))
        assertTrue(result.output.contains("legacy/Helper.groovy: Groovy is forbidden; use Kotlin instead"))
        assertTrue(result.output.contains("config/detekt/MyBaseline.XML: Detekt baseline artifacts are forbidden"))
        assertTrue(result.output.contains("gradle.properties: set org.gradle.caching=true"))
        assertTrue(result.output.contains("gradle.properties: set org.gradle.parallel=true"))
        assertTrue(result.output.contains("gradle.properties: set org.gradle.configureondemand=true"))
    }

    @Test
    fun `codestyle verification validates deterministic Markdown rules`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "default-disabled-lint.gradle.kts")
        writeGradleProperties(
            projectDirectory,
            "org.gradle.caching=true",
            "org.gradle.parallel=true",
            "org.gradle.configureondemand=true",
        )
        projectDirectory.resolve("README.md").toFile().writeText(
            "# BROKEN TITLE\nText wrapped\nacross lines.\n\n```\nvalue\n```\n\n[link](#missing)\n",
        )

        val result = runGradleAndFail(projectDirectory, "verifyMineKotCodestyle")

        assertTrue(result.output.contains("README.md:1: headings must use sentence case"))
        assertTrue(result.output.contains("paragraph text must not contain hard source wrapping"))
        assertTrue(result.output.contains("fenced code blocks require a language identifier"))
        assertTrue(result.output.contains("link text must describe its destination"))
        assertTrue(result.output.contains("internal heading link #missing does not exist"))
    }

    @Test
    fun `assisted preview never mutates and confirmed apply is fingerprint gated`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "default-disabled-lint.gradle.kts")
        val sourceFile = projectDirectory.resolve("src/main/kotlin/Example.kt")
        Files.createDirectories(sourceFile.parent)
        val originalSource = "fun answer(): Int = 42\n"
        sourceFile.toFile().writeText(originalSource)

        val discovery = runGradle(projectDirectory, "mineKotAssistPreview")
        val reportFile = projectDirectory.resolve("build/reports/minekot/assisted-fixes.json")
        val discoveryReport = assistJson.decodeFromString<MineKotAssistReport>(reportFile.toFile().readText())
        val candidate =
            discoveryReport.candidates.single { it.action == "magic-number" && it.path.endsWith("Example.kt") }

        assertEquals(TaskOutcome.SUCCESS, discovery.task(":mineKotAssistPreview")?.outcome)
        assertEquals(originalSource, sourceFile.toFile().readText())

        val requestedDocument = MineKotAssistRequestDocument(
            requests = listOf(
                MineKotAssistRequest(
                    findingId = candidate.findingId,
                    action = candidate.action,
                    options = MineKotMagicNumberOptions(constantName = "ANSWER_VALUE"),
                ),
            ),
        )
        projectDirectory.resolve("minekot-fixes.json").toFile().writeText(
            assistJson.encodeToString(requestedDocument) + "\n",
        )
        runGradle(projectDirectory, "mineKotAssistPreview")
        val requestedReport = assistJson.decodeFromString<MineKotAssistReport>(reportFile.toFile().readText())
        val confirmedDocument = requestedDocument.copy(
            confirmation = MineKotAssistConfirmation(requestedReport.planId, confirmed = true),
        )
        projectDirectory.resolve("minekot-fixes.json").toFile().writeText(
            assistJson.encodeToString(confirmedDocument) + "\n",
        )

        val apply = runGradle(projectDirectory, "mineKotAssistApply")

        assertEquals(TaskOutcome.SUCCESS, apply.task(":mineKotAssistCompileStagedMain")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, apply.task(":mineKotAssistCompileStagedTest")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, apply.task(":mineKotAssistApply")?.outcome)
        assertTrue(sourceFile.toFile().readText().contains("private const val ANSWER_VALUE: Int = 42"))
        assertTrue(sourceFile.toFile().readText().contains("fun answer(): Int = ANSWER_VALUE"))
    }

    @Test
    fun `assisted apply rejects stale confirmation without source mutation`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "default-disabled-lint.gradle.kts")
        val sourceFile = projectDirectory.resolve("src/main/kotlin/Example.kt")
        Files.createDirectories(sourceFile.parent)
        val originalSource = "fun answer(): Int = 42\n"
        sourceFile.toFile().writeText(originalSource)
        projectDirectory.resolve("minekot-fixes.json").toFile().writeText(
            assistJson.encodeToString(
                MineKotAssistRequestDocument(
                    confirmation = MineKotAssistConfirmation("stale-plan", confirmed = true),
                ),
            ) + "\n",
        )

        val result = runGradleAndFail(projectDirectory, "mineKotAssistApply")

        assertTrue(result.output.contains("confirmation is missing or stale"))
        assertEquals(originalSource, sourceFile.toFile().readText())
    }

    @Test
    fun `magic number assisted extraction preserves numeric types and literal forms`() {
        val cases = mapOf(
            "Byte" to "fun accept(value: Byte): Unit = Unit\nfun use(): Unit = accept(42)\n",
            "Short" to "fun accept(value: Short): Unit = Unit\nfun use(): Unit = accept(42)\n",
            "Int" to "fun use(): Int = 4_200\n",
            "Long" to "fun use(): Long = 42L\n",
            "Float" to "fun use(): Float = 42.5F\n",
            "Double" to "fun use(): Double = 42.5\n",
            "Hex" to "fun use(): Int = 0x2A\n",
            "Binary" to "fun use(): Int = 0b101010\n",
        )

        cases.forEach { (name, source) ->
            val projectDirectory = Files.createTempDirectory("minekot-number-${name.lowercase()}")
            val sourceFile = projectDirectory.resolve("src/main/kotlin/Example.kt")
            Files.createDirectories(sourceFile.parent)
            sourceFile.toFile().writeText(source)
            val initial = MineKotAssistedFixEngine(projectDirectory).preview(MineKotAssistRequestDocument())
            val candidate = initial.report.candidates.single()
            val requested = MineKotAssistRequestDocument(
                requests = listOf(
                    MineKotAssistRequest(
                        findingId = candidate.findingId,
                        action = candidate.action,
                        options = MineKotMagicNumberOptions(constantName = "${name.uppercase()}_VALUE"),
                    ),
                ),
            )

            val corrected = MineKotAssistedFixEngine(projectDirectory).preview(requested).replacements.values.single()
            val expectedType = when (name) {
                "Hex", "Binary" -> "Int"
                else -> name
            }
            assertTrue(corrected.contains("private const val ${name.uppercase()}_VALUE: ${expectedType}"), corrected)
        }
    }

    @Test
    fun `File to Path preview updates declarations and consumers project wide`() {
        val projectDirectory = Files.createTempDirectory("minekot-file-to-path")
        val apiFile = projectDirectory.resolve("src/main/kotlin/Api.kt")
        val consumerFile = projectDirectory.resolve("src/test/kotlin/Consumer.kt")
        Files.createDirectories(apiFile.parent)
        Files.createDirectories(consumerFile.parent)
        apiFile.toFile().writeText("import java.io.File\n\nfun load(input: File): File = input\n")
        consumerFile.toFile().writeText("import java.io.File\n\nfun use(): File = load(File(\"input.txt\"))\n")
        val initial = MineKotAssistedFixEngine(projectDirectory).preview(MineKotAssistRequestDocument())
        val requests = initial.report.candidates.map { candidate ->
            MineKotAssistRequest(
                findingId = candidate.findingId,
                action = candidate.action,
                options = MineKotFileToPathOptions(symbol = "load"),
            )
        }

        val preview = MineKotAssistedFixEngine(projectDirectory).preview(
            MineKotAssistRequestDocument(requests = requests),
        )

        assertEquals(setOf(apiFile, consumerFile), preview.replacements.keys)
        preview.replacements.values.forEach { source ->
            assertTrue(source.contains("import java.nio.file.Path"), source)
            assertFalse(source.contains("java.io.File"), source)
        }
        assertTrue(preview.replacements.getValue(consumerFile).contains("load(Path.of(\"input.txt\"))"))
    }

    @Test
    fun `File to Path preview preserves comments strings and unrelated symbols`() {
        val projectDirectory = Files.createTempDirectory("minekot-file-to-path-safety")
        val sourceFile = projectDirectory.resolve("src/main/kotlin/Api.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText(
            """
            import java.io.File

            fun load(input: File): File = input // File remains documentation
            fun unrelated(input: File): String = "File"
            """.trimIndent() + "\n",
        )
        val initial = MineKotAssistedFixEngine(projectDirectory).preview(MineKotAssistRequestDocument())
        val candidate = initial.report.candidates.single { item -> item.action == "file-to-path" }
        val requested = MineKotAssistRequestDocument(
            requests = listOf(
                MineKotAssistRequest(
                    findingId = candidate.findingId,
                    action = candidate.action,
                    options = MineKotFileToPathOptions(symbol = "load"),
                ),
            ),
        )

        val corrected = MineKotAssistedFixEngine(projectDirectory).preview(requested).replacements.getValue(sourceFile)

        assertTrue(corrected.contains("fun load(input: Path): Path"), corrected)
        assertTrue(corrected.contains("// File remains documentation"), corrected)
        assertTrue(corrected.contains("fun unrelated(input: File): String = \"File\""), corrected)
    }

    @Test
    fun `assisted preview requests semantic choices and rejects unsafe exception and lifecycle shapes`() {
        val projectDirectory = Files.createTempDirectory("minekot-assisted-choices")
        val sourceFile = projectDirectory.resolve("src/main/kotlin/Example.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText(
            """
            fun work(name: String): Unit {
                runCatching { println(name) }
                Thread.sleep(1)
                println("§aHello ${'$'}name")
                Thread { println(name) }
                try { println(name) } catch (error: Exception) { println(error) } finally { println("cleanup") }
            }
            """.trimIndent(),
        )

        val report = MineKotAssistedFixEngine(projectDirectory)
            .preview(MineKotAssistRequestDocument())
            .report
        val coroutine = report.candidates.first { candidate ->
            candidate.action == "coroutine-preference" && candidate.status == "needs-input"
        }
        val miniMessage = report.candidates.single { candidate -> candidate.action == "minimessage-text" }
        val unsupported = report.candidates.filter { candidate -> candidate.status == "unsupported" }

        assertEquals(
            setOf("scope-property", "dispatcher", "cancellation-point", "cleanup-method"),
            coroutine.requiredOptions.toSet(),
        )
        assertTrue("placeholders-expression" in miniMessage.requiredOptions)
        assertTrue(unsupported.any { candidate -> candidate.action == "coroutine-preference" })
        assertTrue(unsupported.any { candidate -> candidate.action == "forbidden-try-catch" })
    }

    @Test
    fun `mineKotFormat corrects source and proves second pass idempotence`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "format.gradle.kts")
        writeGradleProperties(
            projectDirectory,
            "org.gradle.caching=true",
            "org.gradle.parallel=true",
            "org.gradle.configureondemand=true",
        )
        val sourceFile = projectDirectory.resolve("src/main/kotlin/Example.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText(
            """
            /**
             * Creates a greeting.
             *
             * @param name greeting recipient.
             * @return formatted greeting.
             */
            fun greeting(name: String): String = "Hello ${'$'}name"
            """.trimIndent() + "\n",
        )
        runGradle(projectDirectory, "writeMineKotCodestyle")

        val result = runGradle(projectDirectory, "mineKotFormat")

        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormat")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatFirstPass")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatSnapshot")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatSecondPass")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatIdempotence")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatVerifyStagedSources")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStaged")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedMain")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedTest")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatApply")?.outcome)
        assertFalse(result.output.contains("mineKotFormatConvergencePass"))
        assertFalse(result.output.contains("mineKotFormatVerificationPass"))
        assertTrue(sourceFile.toFile().readText().contains("\"Hello ${'$'}{name}\""))
        assertTrue(result.output.contains("src/main/kotlin/Example.kt"), result.output)
        assertFalse(
            result.output.contains("build/tmp/minekot/format-sources/src/main/kotlin/Example.kt"),
            result.output,
        )

        val repeatedResult = runGradle(projectDirectory, "mineKotFormat")

        assertEquals(TaskOutcome.SUCCESS, repeatedResult.task(":mineKotFormatCompileStaged")?.outcome)
    }

    @Test
    fun `mineKotFormat leaves sources unchanged when staged validation fails`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "format.gradle.kts")
        writeGradleProperties(
            projectDirectory,
            "org.gradle.caching=true",
            "org.gradle.parallel=true",
            "org.gradle.configureondemand=true",
        )
        val sourceFile = projectDirectory.resolve("src/main/kotlin/Example.kt")
        Files.createDirectories(sourceFile.parent)
        val original = "fun greeting(name: String): String = \"Hello ${'$'}name\"\nfun broken(\n"
        sourceFile.toFile().writeText(original)
        runGradle(projectDirectory, "writeMineKotCodestyle")

        runGradleAndFail(projectDirectory, "mineKotFormat")

        assertEquals(original, sourceFile.toFile().readText())
    }

    @Test
    fun `mineKotFormat rejects concurrent repository source changes`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "format.gradle.kts")
        projectDirectory.resolve("build.gradle.kts").toFile().appendText(
            """

            val mutateAfterFormatStage by tasks.registering {
                dependsOn("mineKotFormatIdempotence")
                doLast { file("src/main/kotlin/Concurrent.kt").appendText("// concurrent change\\n") }
            }
            tasks.matching { it.name == "mineKotFormatApply" }.configureEach {
                dependsOn(mutateAfterFormatStage)
            }
            """.trimIndent() + "\n",
        )
        val sourceFile = projectDirectory.resolve("src/main/kotlin/Concurrent.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText("internal fun concurrent(name: String): String = \"Hello ${'$'}name\"\n")
        runGradle(projectDirectory, "writeMineKotCodestyle")

        val result = runGradleAndFail(projectDirectory, "mineKotFormat")

        assertTrue(result.output.contains("Repository sources changed during mineKotFormat"), result.output)
        assertTrue(sourceFile.toFile().readText().contains("Hello ${'$'}name"))
        assertTrue(sourceFile.toFile().readText().contains("concurrent change"))
    }

    @Test
    fun `staged formatter keeps main and test compilation classpaths isolated`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "format.gradle.kts")
        projectDirectory.resolve("build.gradle.kts").toFile().appendText(
            "\ndependencies { testImplementation(\"org.junit.jupiter:junit-jupiter-api:5.11.4\") }\n",
        )
        val sourceFile = projectDirectory.resolve("src/main/kotlin/InvalidMain.kt")
        Files.createDirectories(sourceFile.parent)
        val original = "import org.junit.jupiter.api.Test\n\n@Test\nfun invalidMain(): Unit = Unit\n"
        sourceFile.toFile().writeText(original)
        runGradle(projectDirectory, "writeMineKotCodestyle")

        val result = runGradleAndFail(projectDirectory, "mineKotFormat")

        assertEquals(TaskOutcome.FAILED, result.task(":mineKotFormatCompileStagedMain")?.outcome)
        assertNull(result.task(":compileKotlin"))
        assertTrue(result.output.contains("unresolved reference 'junit'", ignoreCase = true), result.output)
        assertEquals(original, sourceFile.toFile().readText())
    }

    @Test
    fun `staged formatter compiles test sources against staged main and test dependencies`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "format.gradle.kts")
        projectDirectory.resolve("build.gradle.kts").toFile().appendText(
            "\ndependencies { testImplementation(\"org.junit.jupiter:junit-jupiter-api:5.11.4\") }\n",
        )
        val mainSource = projectDirectory.resolve("src/main/kotlin/InternalValue.kt")
        val testSource = projectDirectory.resolve("src/test/kotlin/InternalValueTest.kt")
        Files.createDirectories(mainSource.parent)
        Files.createDirectories(testSource.parent)
        mainSource.toFile().writeText("internal fun internalValue(): String = \"value\"\n")
        testSource.toFile().writeText(
            "import org.junit.jupiter.api.Test\n\n@Test\nfun readsInternalValue(): Unit { internalValue() }\n",
        )
        runGradle(projectDirectory, "writeMineKotCodestyle")

        val result = runGradle(projectDirectory, "mineKotFormat")

        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedMain")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedTest")?.outcome)
    }

    @Test
    fun `staged formatter retains project dependency producer wiring`() {
        val projectDirectory = createProject()
        projectDirectory.resolve("settings.gradle.kts").toFile().appendText("\ninclude(\":dependency\")\n")
        writeBuildFixture(projectDirectory, "format.gradle.kts")
        projectDirectory.resolve("build.gradle.kts").toFile().appendText(
            "\ndependencies { implementation(project(\":dependency\")) }\n",
        )
        val dependencyDirectory = projectDirectory.resolve("dependency")
        Files.createDirectories(dependencyDirectory)
        dependencyDirectory.resolve("build.gradle.kts").toFile().writeText(
            "plugins { kotlin(\"jvm\") }\n",
        )
        val dependencySource = dependencyDirectory.resolve("src/main/kotlin/DependencyValue.kt")
        Files.createDirectories(dependencySource.parent)
        dependencySource.toFile()
            .writeText("package dependency\n\nobject DependencyValue { val value: String = \"value\" }\n")
        val sourceFile = projectDirectory.resolve("src/main/kotlin/Consumer.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText(
            "import dependency.DependencyValue\n\ninternal fun consumeDependency(): String = DependencyValue.value\n",
        )
        runGradle(projectDirectory, "writeMineKotCodestyle")

        val result = runGradle(projectDirectory, "mineKotFormat")

        assertNotNull(result.task(":dependency:compileKotlin"))
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedMain")?.outcome)
    }

    @Test
    fun `staged formatter discovers custom Kotlin source roots`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "format.gradle.kts")
        projectDirectory.resolve("build.gradle.kts").toFile().appendText(
            "\nkotlin { sourceSets.main { kotlin.srcDir(\"custom-kotlin\") } }\n",
        )
        val sourceFile = projectDirectory.resolve("custom-kotlin/Custom.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText("fun custom(name: String): String = \"Hello ${'$'}name\"\n")
        runGradle(projectDirectory, "writeMineKotCodestyle")

        val result = runGradle(projectDirectory, "mineKotFormat")

        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedMain")?.outcome)
        assertTrue(sourceFile.toFile().readText().contains("${'$'}{name}"))
    }

    @Test
    fun `staged formatter omits absent optional classpath outputs under warnings as errors`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "format.gradle.kts")
        projectDirectory.resolve("build.gradle.kts").toFile().appendText(
            """

            dependencies {
                add("implementation", files(layout.buildDirectory.dir("missing-optional-classes")))
            }
            minekotToolchain { build { allWarningsAsErrors.set(true) } }
            """.trimIndent() + "\n",
        )
        val sourceFile = projectDirectory.resolve("src/main/kotlin/Example.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText("internal fun example(): Unit = Unit\n")
        runGradle(projectDirectory, "writeMineKotCodestyle")

        val result = runGradle(projectDirectory, "mineKotFormat")

        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedMain")?.outcome)
        assertFalse(result.output.contains("classpath entry points to a non-existent location"), result.output)
    }

    @Test
    fun `staged formatter models custom JVM compilations`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "format.gradle.kts")
        projectDirectory.resolve("build.gradle.kts").toFile().appendText(
            """

            kotlin.target.compilations.create("integrationTest") {
                associateWith(kotlin.target.compilations.getByName("main"))
            }
            """.trimIndent() + "\n",
        )
        val mainSource = projectDirectory.resolve("src/main/kotlin/InternalMain.kt")
        val integrationSource = projectDirectory.resolve("src/integrationTest/kotlin/IntegrationValue.kt")
        Files.createDirectories(mainSource.parent)
        Files.createDirectories(integrationSource.parent)
        mainSource.toFile().writeText("internal fun internalMain(): String = \"main\"\n")
        integrationSource.toFile().writeText("internal fun integrationValue(): String = internalMain()\n")
        runGradle(projectDirectory, "writeMineKotCodestyle")

        val result = runGradle(projectDirectory, "mineKotFormat")

        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedIntegrationTest")?.outcome)
    }

    @Test
    fun `staged formatter rejects unregistered source-derived generators`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "format.gradle.kts")
        projectDirectory.resolve("build.gradle.kts").toFile().appendText(
            """

            val generatedDirectory = layout.buildDirectory.dir("generated/source-derived")
            val generateSourceDerived by tasks.registering {
                inputs.file("src/main/kotlin/GeneratorInput.kt")
                outputs.dir(generatedDirectory)
                doLast {
                    generatedDirectory.get().file("Generated.kt").asFile.apply {
                        parentFile.mkdirs()
                        writeText("internal fun generatedFromSource(): String = \\\"generated\\\"\\n")
                    }
                }
            }
            kotlin { sourceSets.main { kotlin.srcDir(generateSourceDerived) } }
            """.trimIndent() + "\n",
        )
        val sourceFile = projectDirectory.resolve("src/main/kotlin/GeneratorInput.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText("internal val generatedFromInput: String = generatedFromSource()\n")
        runGradle(projectDirectory, "writeMineKotCodestyle")

        val result = runGradleAndFail(projectDirectory, "mineKotFormat")

        assertTrue(result.output.contains("Unsupported staged Kotlin generators"), result.output)
        assertTrue(result.output.contains(":generateSourceDerived reads repository Kotlin"), result.output)
    }

    @Test
    fun `staged formatter runs registered generators against staged sources`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "format.gradle.kts")
        projectDirectory.resolve("build.gradle.kts").toFile().appendText(
            """

            val originalGenerated = layout.buildDirectory.dir("generated/registered")
            val formatGenerated = layout.buildDirectory.dir("tmp/minekot/custom-format-generated")
            val assistGenerated = layout.buildDirectory.dir("tmp/minekot/custom-assist-generated")
            val formatInput = layout.buildDirectory.file(
                "tmp/minekot/format-sources/src/main/kotlin/GeneratorInput.kt",
            )
            val assistInput = layout.buildDirectory.file(
                "reports/minekot/staged/src/main/kotlin/GeneratorInput.kt",
            )
            val generateOriginal by tasks.registering {
                inputs.file("src/main/kotlin/GeneratorInput.kt")
                outputs.dir(originalGenerated)
                doLast {
                    originalGenerated.get().file("Generated.kt").asFile.apply {
                        parentFile.mkdirs()
                        writeText("internal fun generatedFromSource(): Int = 1\n")
                    }
                }
            }
            val generateFormat by tasks.registering {
                dependsOn("mineKotFormatIdempotence")
                inputs.file(formatInput)
                outputs.dir(formatGenerated)
                doLast {
                    val formatted = formatInput.get().asFile.readText().contains("${'\\'}${'$'}{name}")
                    val declaration = if (formatted) {
                        "internal fun generatedFromSource(): String = String()"
                    } else {
                        "internal fun generatedFromSource(): Int = 1"
                    }
                    formatGenerated.get().file("Generated.kt").asFile.apply {
                        parentFile.mkdirs()
                        writeText(declaration + "\n")
                    }
                }
            }
            val generateAssist by tasks.registering {
                dependsOn("mineKotAssistStage")
                inputs.file(assistInput)
                outputs.dir(assistGenerated)
                doLast {
                    val formatted = assistInput.get().asFile.readText().contains("${'\\'}${'$'}{name}")
                    val declaration = if (formatted) {
                        "internal fun generatedFromSource(): String = String()"
                    } else {
                        "internal fun generatedFromSource(): Int = 1"
                    }
                    assistGenerated.get().file("Generated.kt").asFile.apply {
                        parentFile.mkdirs()
                        writeText(declaration + "\n")
                    }
                }
            }
            kotlin { sourceSets.main { kotlin.srcDir(generateOriginal) } }
            minekotToolchain {
                stagedCompilation {
                    generators.register("sourceDerived") {
                        compilationName.set("main")
                        originalKotlinOutput.set(originalGenerated)
                        formatKotlinOutput.set(generateFormat.map { formatGenerated.get() })
                        assistKotlinOutput.set(generateAssist.map { assistGenerated.get() })
                    }
                }
            }
            """.trimIndent() + "\n",
        )
        val sourceFile = projectDirectory.resolve("src/main/kotlin/GeneratorInput.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText(
            "internal fun generatorInput(name: String): String = \"Hello ${'$'}name\"\n\n" +
                    "internal val generatedFromInput: String = generatedFromSource()\n\n" +
                    "internal fun generatedAnswer(): Int = 42\n",
        )
        runGradle(projectDirectory, "writeMineKotCodestyle")

        val result = runGradle(projectDirectory, "mineKotFormat")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateFormat")?.outcome)
        assertNull(result.task(":generateOriginal"))
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedMain")?.outcome)
        assertTrue(sourceFile.toFile().readText().contains("${'$'}{name}"))

        runGradle(projectDirectory, "mineKotAssistPreview")
        val reportFile = projectDirectory.resolve("build/reports/minekot/assisted-fixes.json")
        val candidate = assistJson.decodeFromString<MineKotAssistReport>(reportFile.toFile().readText())
            .candidates.single { candidate ->
                candidate.action == "magic-number" && candidate.path.endsWith("GeneratorInput.kt")
            }
        val request = MineKotAssistRequestDocument(
            requests = listOf(
                MineKotAssistRequest(
                    findingId = candidate.findingId,
                    action = candidate.action,
                    options = MineKotMagicNumberOptions(constantName = "GENERATED_ANSWER"),
                ),
            ),
        )
        projectDirectory.resolve("minekot-fixes.json").toFile().writeText(assistJson.encodeToString(request) + "\n")
        runGradle(projectDirectory, "mineKotAssistPreview")
        val requested = assistJson.decodeFromString<MineKotAssistReport>(reportFile.toFile().readText())
        projectDirectory.resolve("minekot-fixes.json").toFile().writeText(
            assistJson.encodeToString(
                request.copy(confirmation = MineKotAssistConfirmation(requested.planId, confirmed = true)),
            ) + "\n",
        )

        val assisted = runGradle(projectDirectory, "mineKotAssistApply")

        assertEquals(TaskOutcome.SUCCESS, assisted.task(":generateAssist")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, assisted.task(":mineKotAssistCompileStagedMain")?.outcome)
    }

    @Test
    fun `staged formatter reruns KSP against formatted sources`() {
        val projectDirectory = createProject()
        projectDirectory.resolve("settings.gradle.kts").toFile().appendText("\ninclude(\":processor\")\n")
        projectDirectory.resolve("build.gradle.kts").toFile().writeText(
            """
            plugins {
                id("org.minekot.toolchain") version "+"
                id("com.google.devtools.ksp")
            }

            dependencies {
                ksp(project(":processor"))
                kspTest(project(":processor"))
            }
            """.trimIndent() + "\n",
        )
        val processorDirectory = projectDirectory.resolve("processor")
        Files.createDirectories(processorDirectory)
        processorDirectory.resolve("build.gradle.kts").toFile().writeText(
            """
            plugins { kotlin("jvm") }

            dependencies {
                implementation("com.google.devtools.ksp:symbol-processing-api:2.3.10")
            }
            """.trimIndent() + "\n",
        )
        val processorSource = processorDirectory.resolve("src/main/kotlin/FormattingProcessor.kt")
        Files.createDirectories(processorSource.parent)
        processorSource.toFile().writeText(
            """
            package processor

            import com.google.devtools.ksp.processing.CodeGenerator
            import com.google.devtools.ksp.processing.Dependencies
            import com.google.devtools.ksp.processing.KSPLogger
            import com.google.devtools.ksp.processing.Resolver
            import com.google.devtools.ksp.processing.SymbolProcessor
            import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
            import com.google.devtools.ksp.processing.SymbolProcessorProvider
            import com.google.devtools.ksp.symbol.KSAnnotated
            import java.io.File

            class FormattingProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
                private var generated: Boolean = false

                override fun process(resolver: Resolver): List<KSAnnotated> {
                    if (generated) return emptyList()
                    val source = resolver.getAllFiles().firstOrNull { file -> file.fileName == "Source.kt" }
                        ?: return emptyList()
                    val formatted = File(source.filePath).readText().contains("${'\\'}${'$'}{name}")
                    val returnType = if (formatted) "String" else "Int"
                    val returnValue = if (formatted) "\"generated\"" else "1"
                    codeGenerator.createNewFile(Dependencies(false, source), "", "Generated").writer().use { writer ->
                        writer.write("internal fun generatedValue(): ${'$'}returnType = ${'$'}returnValue\n")
                    }
                    generated = true
                    return emptyList()
                }
            }

            class FormattingProcessorProvider : SymbolProcessorProvider {
                override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
                    FormattingProcessor(environment.codeGenerator)
            }
            """.trimIndent() + "\n",
        )
        val serviceFile = processorDirectory.resolve(
            "src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider",
        )
        Files.createDirectories(serviceFile.parent)
        serviceFile.toFile().writeText("processor.FormattingProcessorProvider\n")
        val sourceFile = projectDirectory.resolve("src/main/kotlin/Source.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText(
            "internal fun greeting(name: String): String = \"Hello ${'$'}name\"\n\n" +
                    "internal val generated: String = generatedValue()\n\n" +
                    "internal fun kspAnswer(): Int = 42\n",
        )
        val testSource = projectDirectory.resolve("src/test/kotlin/KspStagedTest.kt")
        Files.createDirectories(testSource.parent)
        testSource.toFile().writeText(
            "internal fun readsStagedKspMain(): String = generatedValue() + greeting(\"test\")\n",
        )
        runGradle(projectDirectory, "writeMineKotCodestyle")

        val result = runGradle(projectDirectory, "mineKotFormat")

        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedKspMain")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedMain")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedKspTest")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedTest")?.outcome)
        assertNull(result.task(":kspKotlin"))
        assertNull(result.task(":kspTestKotlin"))
        assertTrue(sourceFile.toFile().readText().contains("${'$'}{name}"))
        assertTrue(
            projectDirectory.resolve("build/tmp/minekot/format-classes-ksp/main/kotlin/Generated.kt")
                .toFile()
                .readText()
                .contains("String"),
        )
        runGradle(projectDirectory, "mineKotFormat", "--configuration-cache")
        val cachedFormat = runGradle(projectDirectory, "mineKotFormat", "--configuration-cache")
        assertTrue(cachedFormat.output.contains("Reusing configuration cache"), cachedFormat.output)

        runGradle(projectDirectory, "mineKotAssistPreview")
        val reportFile = projectDirectory.resolve("build/reports/minekot/assisted-fixes.json")
        val candidate = assistJson.decodeFromString<MineKotAssistReport>(reportFile.toFile().readText())
            .candidates.single { candidate -> candidate.action == "magic-number" && candidate.path.endsWith("Source.kt") }
        val request = MineKotAssistRequestDocument(
            requests = listOf(
                MineKotAssistRequest(
                    findingId = candidate.findingId,
                    action = candidate.action,
                    options = MineKotMagicNumberOptions(constantName = "KSP_ANSWER"),
                ),
            ),
        )
        projectDirectory.resolve("minekot-fixes.json").toFile().writeText(assistJson.encodeToString(request) + "\n")
        runGradle(projectDirectory, "mineKotAssistPreview")
        val requested = assistJson.decodeFromString<MineKotAssistReport>(reportFile.toFile().readText())
        projectDirectory.resolve("minekot-fixes.json").toFile().writeText(
            assistJson.encodeToString(
                request.copy(confirmation = MineKotAssistConfirmation(requested.planId, confirmed = true)),
            ) + "\n",
        )

        val assisted = runGradle(projectDirectory, "mineKotAssistApply")

        assertEquals(TaskOutcome.SUCCESS, assisted.task(":mineKotAssistCompileStagedKspMain")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, assisted.task(":mineKotAssistCompileStagedMain")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, assisted.task(":mineKotAssistCompileStagedKspTest")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, assisted.task(":mineKotAssistCompileStagedTest")?.outcome)
        assertNull(assisted.task(":kspKotlin"))
        assertNull(assisted.task(":kspTestKotlin"))
    }

    @Test
    fun `staged formatter ignores KSP compilations without processors`() {
        val projectDirectory = createProject()
        projectDirectory.resolve("build.gradle.kts").toFile().writeText(
            """
            plugins {
                id("org.minekot.toolchain") version "+"
                id("com.google.devtools.ksp")
            }
            """.trimIndent() + "\n",
        )
        val sourceFile = projectDirectory.resolve("src/main/kotlin/Source.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText("internal fun answer(): Int = 42\n")
        runGradle(projectDirectory, "writeMineKotCodestyle")

        val result = runGradle(projectDirectory, "mineKotFormat")

        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedMain")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedTest")?.outcome)
        assertNull(result.task(":kspKotlin"))
        assertNull(result.task(":kspTestKotlin"))
    }

    @Test
    fun `staged formatter preserves Kotlin compiler plugins`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "format.gradle.kts")
        val sourceFile = projectDirectory.resolve("src/main/kotlin/SerializableValue.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText(
            """
            import kotlinx.serialization.Serializable

            @Serializable
            internal data class SerializableValue(val name: String)

            internal val valueSerializer = SerializableValue.serializer()
            """.trimIndent() + "\n",
        )
        runGradle(projectDirectory, "writeMineKotCodestyle")

        val result = runGradle(projectDirectory, "mineKotFormat")

        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedMain")?.outcome)
    }

    @Test
    fun `staged formatter preserves compilation warning policy`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "format.gradle.kts")
        projectDirectory.resolve("build.gradle.kts").toFile().appendText(
            "\nminekotToolchain { build { allWarningsAsErrors.set(true) } }\n",
        )
        val sourceFile = projectDirectory.resolve("src/main/kotlin/DeprecatedValue.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText(
            "@Deprecated(\"old\")\ninternal fun oldValue(): Unit = Unit\n\ninternal fun useOldValue(): Unit = oldValue()\n",
        )
        runGradle(projectDirectory, "writeMineKotCodestyle")

        val result = runGradleAndFail(projectDirectory, "mineKotFormat")

        assertTrue(result.output.contains("warnings found and -Werror specified"), result.output)
    }

    @Test
    fun `staged formatter preserves generated Kotlin source dependencies`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "format.gradle.kts")
        projectDirectory.resolve("build.gradle.kts").toFile().appendText(
            """

            val generatedKotlin = layout.buildDirectory.dir("generated/minekot/main")
            val generateKotlin by tasks.registering {
                outputs.dir(generatedKotlin)
                doLast {
                    generatedKotlin.get().file("Generated.kt").asFile.apply {
                        parentFile.mkdirs()
                        writeText("internal fun generatedValue(): String = \"generated\"\n")
                    }
                }
            }
            kotlin { sourceSets.main { kotlin.srcDir(generateKotlin) } }
            """.trimIndent() + "\n",
        )
        val sourceFile = projectDirectory.resolve("src/main/kotlin/GeneratedConsumer.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText("internal fun consumeGenerated(): String = generatedValue()\n")
        runGradle(projectDirectory, "writeMineKotCodestyle")

        val result = runGradle(projectDirectory, "mineKotFormat")

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateKotlin")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":mineKotFormatCompileStagedMain")?.outcome)
    }

    @Test
    fun `staged formatter reuses configuration cache`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "format.gradle.kts")
        val sourceFile = projectDirectory.resolve("src/main/kotlin/Value.kt")
        Files.createDirectories(sourceFile.parent)
        sourceFile.toFile().writeText("val value: String = \"value\"\n")
        runGradle(projectDirectory, "writeMineKotCodestyle")

        runGradle(projectDirectory, "mineKotFormat", "--configuration-cache")
        runGradle(projectDirectory, "mineKotFormat", "--configuration-cache")
        val result = runGradle(projectDirectory, "mineKotFormat", "--configuration-cache")

        assertTrue(result.output.contains("Reusing configuration cache"), result.output)
    }

    @Test
    fun `published plugin classpath does not require KSP`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "default-disabled-lint.gradle.kts")

        val result = runGradleWithoutKsp(projectDirectory, "tasks")

        assertTrue(result.output.contains("mineKotAssistCompileStaged"))
        assertFalse(result.output.contains("NoClassDefFoundError"))
    }

    @Test
    fun `toolchain rejects Kotlin Multiplatform explicitly`() {
        val projectDirectory = createProject()
        projectDirectory.resolve("build.gradle.kts").toFile().writeText(
            """
            plugins {
                kotlin("multiplatform") version "2.4.10"
                id("org.minekot.toolchain") version "+"
            }
            """.trimIndent() + "\n",
        )

        val result = runGradleAndFail(projectDirectory, "tasks")

        assertTrue(result.output.contains("Kotlin Multiplatform staged compilation is not yet supported"))
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

    @Test
    fun `transactional publication restores every moved source after failure`() {
        val projectDirectory = Files.createTempDirectory("minekot-transaction-test")
        val first = projectDirectory.resolve("First.kt")
        val second = projectDirectory.resolve("Second.kt")
        first.toFile().writeText("first-original\n")
        second.toFile().writeText("second-original\n")

        val failure = assertThrows(org.gradle.api.GradleException::class.java) {
            MineKotTransactionalPublisher.publish(
                root = projectDirectory,
                replacements = mapOf(
                    first to "first-new\n".toByteArray(),
                    second to "second-new\n".toByteArray(),
                ),
                transactionDirectory = projectDirectory.resolve("transaction"),
                failureAfterMoves = 1,
            )
        }

        assertTrue(failure.message.orEmpty().contains("original sources were restored"))
        assertEquals("first-original\n", first.toFile().readText())
        assertEquals("second-original\n", second.toFile().readText())
    }

    @Test
    fun `CI CD remains disabled by default`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "default-disabled-lint.gradle.kts")

        val result = runGradle(projectDirectory, "tasks", "--all")

        assertFalse(result.output.contains("writeMineKotCiMetadata"))
        assertFalse(result.output.contains("publishMineKotPublication"))
    }

    @Test
    fun `CI CD writes metadata and validates changelog fragments`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "ci-cd.gradle.kts")
        val fragment = projectDirectory.resolve(".changes/feature/typed-delivery.md")
        Files.createDirectories(fragment.parent)
        fragment.toFile().writeText("Add typed CI/CD delivery tasks.\n")

        val result = runGradle(projectDirectory, "writeMineKotCiMetadata", "verifyMineKotChanges")

        assertEquals(TaskOutcome.SUCCESS, result.task(":writeMineKotCiMetadata")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":verifyMineKotChanges")?.outcome)
        val metadata = projectDirectory.resolve("build/reports/minekot/cicd/metadata.json").toFile().readText()
        assertTrue(metadata.contains("1.21.8"))
        assertTrue(metadata.contains("1.21.11"))
    }

    @Test
    fun `CI CD prepares and verifies stable release evidence`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "ci-cd.gradle.kts")
        val fragment = projectDirectory.resolve(".changes/feature/typed-delivery.md")
        Files.createDirectories(fragment.parent)
        fragment.toFile().writeText("Add typed CI/CD delivery tasks.\n")

        val prepared = runGradle(
            projectDirectory,
            "prepareMineKotRelease",
            "-PreleaseVersion=1.2.0",
            "-PreleaseDate=2026-07-22",
        )

        assertEquals(TaskOutcome.SUCCESS, prepared.task(":prepareMineKotRelease")?.outcome)
        assertFalse(fragment.toFile().exists())
        assertTrue(projectDirectory.resolve("CHANGELOG.md").toFile().readText().contains("## 1.2.0 - 2026-07-22"))

        val commit = "0123456789abcdef0123456789abcdef01234567"
        val evidence = projectDirectory.resolve("docs/releases/v1.2.0.md")
        Files.createDirectories(evidence.parent)
        evidence.toFile().writeText(
            """
            # MineKot v1.2.0 release evidence

            Commit: `${commit}`

            - [x] fabric-1.21.8
            - [x] neoforge-1.21.8
            """.trimIndent() + "\n",
        )

        val verified = runGradle(
            projectDirectory,
            "verifyMineKotRelease",
            "-PreleaseTag=v1.2.0",
            "-PreleaseVersion=1.2.0",
            "-PreleaseCommit=${commit}",
            "-PprotectedBranchCommit=${commit}",
        )

        assertEquals(TaskOutcome.SUCCESS, verified.task(":verifyMineKotRelease")?.outcome)
    }

    @Test
    fun `CI CD assembles exact artifact bundle with checksums`() {
        val projectDirectory = createProject()
        writeBuildFixture(projectDirectory, "ci-cd.gradle.kts")
        val artifacts = projectDirectory.resolve("build/ci/artifacts")
        Files.createDirectories(artifacts.resolve("paper"))
        Files.createDirectories(artifacts.resolve("velocity"))
        artifacts.resolve("paper/paper.jar").toFile().writeText("paper")
        artifacts.resolve("velocity/velocity.jar").toFile().writeText("velocity")

        val result = runGradle(projectDirectory, "assembleMineKotRelease")

        assertEquals(TaskOutcome.SUCCESS, result.task(":assembleMineKotRelease")?.outcome)
        val bundle = projectDirectory.resolve("build/ci/release")
        assertTrue(bundle.resolve("paper.jar").toFile().isFile)
        assertTrue(bundle.resolve("velocity.jar").toFile().isFile)
        assertTrue(bundle.resolve("SHA256SUMS").toFile().readText().contains("paper.jar"))
        assertTrue(bundle.resolve("manifest.json").toFile().readText().contains("velocity.jar"))
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
            .withArguments(*arguments, "--stacktrace", "--no-build-cache")
            .build()

    private fun runGradleWithoutKsp(projectDirectory: Path, vararg arguments: String): BuildResult {
        val metadataFile = listOf(
            Path.of("build/pluginUnderTestMetadata/plugin-under-test-metadata.properties"),
            Path.of("plugin/toolchain/build/pluginUnderTestMetadata/plugin-under-test-metadata.properties"),
        ).first { candidate -> candidate.toFile().isFile }
        val metadata = Properties().apply {
            metadataFile.toFile().inputStream().use(::load)
        }
        val pluginClasspath = metadata.getProperty("implementation-classpath")
            .split(File.pathSeparator)
            .map(::File)
            .filterNot { file -> "symbol-processing" in file.name || "ksp" in file.name.lowercase() }
        return GradleRunner.create()
            .withProjectDir(projectDirectory.toFile())
            .withPluginClasspath(pluginClasspath)
            .withArguments(*arguments, "--stacktrace", "--no-build-cache")
            .build()
    }

    private fun runGradleAndFail(projectDirectory: Path, vararg arguments: String) =
        GradleRunner.create()
            .withProjectDir(projectDirectory.toFile())
            .withPluginClasspath()
            .withArguments(*arguments, "--stacktrace", "--no-build-cache")
            .buildAndFail()

    private fun readFixture(path: String): String {
        return requireNotNull(javaClass.classLoader.getResource("fixtures/${path}")) {
            "Missing test fixture ${path}."
        }.readText()
    }

    private companion object {
        val assistJson: Json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
    }
}
