package org.minekot.toolchain

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Verifies repository policies that cannot be observed reliably through parsed Kotlin PSI.
 */
@DisableCachingByDefault(because = "Scans repository files dynamically and has no outputs.")
abstract class VerifyMineKotCodestyleTask : DefaultTask() {
    /** Repository root checked by this task. */
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    /** Verifies raw source files and repository-level Gradle policy. */
    @TaskAction
    fun verify() {
        val root = projectDirectory.get().asFile
        val violations = buildList {
            root.walkTopDown()
                .onEnter { directory -> directory.name !in ignoredDirectories }
                .filter { file -> file.isFile && file.extension in kotlinExtensions }
                .forEach { file -> addAll(file.sourceFileViolations(root)) }
            root.walkTopDown()
                .onEnter { directory -> directory.name !in ignoredDirectories }
                .filter { file -> file.isFile && file.extension in forbiddenGroovyExtensions }
                .forEach { file ->
                    val relativePath = file.relativeTo(root).invariantSeparatorsPath
                    add("${relativePath}: Groovy is forbidden; use Kotlin instead")
                }
            addAll(root.gradlePropertyViolations())
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                violations.joinToString(
                    prefix = "MineKot repository codestyle violations:\n- ",
                    separator = "\n- ",
                ),
            )
        }
    }

    private fun java.io.File.sourceFileViolations(root: java.io.File): List<String> {
        val relativePath = relativeTo(root).invariantSeparatorsPath
        val bytes = readBytes()
        val text = bytes.toString(Charsets.UTF_8)
        return buildList {
            if (bytes.startsWith(utf8Bom)) {
                add("${relativePath}: remove UTF-8 BOM")
            }
            if ('\r' in text) {
                add("${relativePath}: use LF line endings")
            }
            if (!text.endsWith('\n') || text.endsWith("\n\n")) {
                add("${relativePath}: end with exactly one newline")
            }
        }
    }

    private fun java.io.File.gradlePropertyViolations(): List<String> {
        val propertiesFile = resolve("gradle.properties")
        if (!propertiesFile.isFile) {
            return emptyList()
        }
        val properties = java.util.Properties().apply {
            propertiesFile.inputStream().use(::load)
        }
        return requiredGradleProperties.mapNotNull { (name, expectedValue) ->
            if (properties.getProperty(name) == expectedValue) {
                null
            } else {
                "gradle.properties: set ${name}=${expectedValue}"
            }
        }
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { index -> this[index] == prefix[index] }

    private companion object {
        private val utf8Bom: ByteArray = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        private val kotlinExtensions: Set<String> = setOf("kt", "kts")
        private val ignoredDirectories: Set<String> = setOf(".git", ".gradle", ".idea", "build", "out")
        private val forbiddenGroovyExtensions: Set<String> = setOf("gradle", "groovy")
        private val requiredGradleProperties: Map<String, String> = mapOf(
            "org.gradle.caching" to "true",
            "org.gradle.parallel" to "true",
        )
    }
}
