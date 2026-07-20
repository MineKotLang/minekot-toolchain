package org.minekot.toolchain

import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.security.MessageDigest
import kotlin.io.path.*

/** Copies Kotlin sources into a disposable formatting workspace. */
@DisableCachingByDefault(because = "Stages current mutable repository sources.")
abstract class MineKotFormatStageTask : DefaultTask() {
    init {
        outputs.upToDateWhen { false }
    }

    /** Repository root containing source files. */
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    /** Disposable source mirror under the build directory. */
    @get:OutputDirectory
    abstract val stagingDirectory: DirectoryProperty

    /** Recreates the source mirror without modifying repository sources. */
    @TaskAction
    fun stage() {
        val root = projectDirectory.get().asFile.toPath()
        val staging = stagingDirectory.get().asFile.toPath()
        project.delete(staging.toFile())
        sourceFiles(root).forEach { source ->
            val target = staging.resolve(source.relativeTo(root))
            target.parent.createDirectories()
            source.toFile().copyTo(target.toFile(), overwrite = true)
        }
    }
}

/** Captures source hashes after first correction pass. */
@DisableCachingByDefault(because = "Captures mutable source state between formatter passes.")
abstract class MineKotFormatSnapshotTask : DefaultTask() {
    /** Repository root containing sources. */
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    /** Snapshot manifest. */
    @get:OutputFile
    abstract val snapshotFile: RegularFileProperty

    /** Writes stable relative-path hashes. */
    @TaskAction
    fun snapshot() {
        val root = projectDirectory.get().asFile.toPath()
        val hashes = sourceHashes(root)
        snapshotFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(formatJson.encodeToString(hashes) + "\n")
        }
    }
}

/** Fails when second formatter pass changes source bytes. */
@DisableCachingByDefault(because = "Compares mutable source state after second formatter pass.")
abstract class MineKotFormatIdempotenceTask : DefaultTask() {
    /** Repository root containing sources. */
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    /** First-pass hash manifest. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val snapshotFile: RegularFileProperty

    /** Verifies byte-identical second pass. */
    @TaskAction
    fun verify() {
        val expected = formatJson.decodeFromString<Map<String, String>>(snapshotFile.get().asFile.readText())
        val actual = sourceHashes(projectDirectory.get().asFile.toPath())
        if (expected != actual) {
            val changed = (expected.keys + actual.keys).filter { path -> expected[path] != actual[path] }.sorted()
            throw GradleException("mineKotFormat second pass changed sources: ${changed.joinToString()}")
        }
    }
}

/** Transactionally publishes a fully validated staged formatting result. */
@DisableCachingByDefault(because = "Publishes validated source changes.")
abstract class MineKotFormatApplyTask : DefaultTask() {
    /** Repository root receiving validated sources. */
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    /** Validated source mirror. */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val stagingDirectory: DirectoryProperty

    /** Applies staged files only after all task dependencies succeed. */
    @TaskAction
    fun apply() {
        val root = projectDirectory.get().asFile.toPath()
        val staging = stagingDirectory.get().asFile.toPath()
        val replacements = sourceFiles(staging).associate { source ->
            root.resolve(source.relativeTo(staging)) to source.toFile().readBytes()
        }
        MineKotTransactionalPublisher.publish(
            root = root,
            replacements = replacements,
            transactionDirectory = staging.parent.resolve("format-transaction"),
        )
    }
}

private fun sourceFiles(root: java.nio.file.Path): List<java.nio.file.Path> =
    root.walk()
        .filter { path ->
            path.isRegularFile() && path.extension in formatSourceExtensions &&
                    path.relativeTo(root).none { segment -> segment.name in formatIgnoredDirectories }
        }
        .toList()

private fun sourceHashes(root: java.nio.file.Path): Map<String, String> =
    sourceFiles(root)
        .associate { path ->
            path.relativeTo(root).invariantSeparatorsPathString to sha256(path.readText())
        }
        .toSortedMap()

private fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }

private val formatJson: Json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}
private val formatSourceExtensions: Set<String> = setOf("kt", "kts")
private val formatIgnoredDirectories: Set<String> = setOf(".git", ".gradle", ".idea", "build", "out")
