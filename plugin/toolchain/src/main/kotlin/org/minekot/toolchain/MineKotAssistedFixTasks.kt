package org.minekot.toolchain

import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.io.path.*

/** Writes assisted-fix candidates and proposed changes without changing source files. */
@DisableCachingByDefault(because = "Preview depends on dynamic repository source state.")
abstract class MineKotAssistPreviewTask : DefaultTask() {
    /** Filesystem service. */
    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    init {
        outputs.upToDateWhen { false }
    }

    /** Repository root scanned by this task. */
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    /** Optional JSON request document. */
    @get:Internal
    abstract val requestFile: RegularFileProperty

    /** Report output directory. */
    @get:OutputDirectory
    abstract val reportDirectory: DirectoryProperty

    /** Generates JSON and unified-diff previews. */
    @TaskAction
    open fun preview() {
        val preview = calculatePreview()
        writePreview(preview)
        logger.lifecycle("MineKot assisted-fix plan ${preview.report.planId}: ${preview.replacements.size} changed files.")
    }

    internal fun calculatePreview(): MineKotAssistPreview {
        val root = projectDirectory.get().asFile.toPath()
        return MineKotAssistedFixEngine(root).preview(readRequestDocument())
    }

    internal fun readRequestDocument(): MineKotAssistRequestDocument {
        val path = requestFile.get().asFile.toPath()
        return if (path.exists()) {
            assistJson.decodeFromString(path.readText())
        } else {
            MineKotAssistRequestDocument()
        }
    }

    internal fun writePreview(preview: MineKotAssistPreview) {
        val directory = reportDirectory.get().asFile.toPath().createDirectories()
        directory.resolve(reportFileName).writeText(assistJson.encodeToString(preview.report) + "\n")
        directory.resolve(diffFileName).writeText(preview.diff)
    }

    internal companion object {
        const val reportFileName: String = "assisted-fixes.json"
        const val diffFileName: String = "assisted-fixes.diff"
        const val baselineFileName: String = "assisted-source-baseline.json"
        val assistJson: Json = Json {
            ignoreUnknownKeys = false
            prettyPrint = true
            prettyPrintIndent = "  "
        }
    }
}

/** Stages one source-fingerprint-gated assisted-fix preview. */
@DisableCachingByDefault(because = "Stages explicitly confirmed source changes.")
abstract class MineKotAssistStageTask : MineKotAssistPreviewTask() {
    /** Validates confirmation, re-previews current sources, then writes staged sources. */
    @TaskAction
    override fun preview() {
        val document = readRequestDocument()
        val currentPreview = confirmedPreview(document)
        val root = projectDirectory.get().asFile.toPath()
        val reportPath = reportDirectory.get().asFile.toPath()
        val stagingDirectory = reportPath.resolve("staged").createDirectories()
        reportPath.resolve(baselineFileName).writeText(
            assistJson.encodeToString(assistSourceHashes(root)) + "\n",
        )
        fileSystemOperations.delete { operation -> operation.delete(stagingDirectory.toFile()) }
        root.walk()
            .filter { path ->
                path.isRegularFile() && path.extension in setOf("kt", "kts") &&
                        path.relativeTo(root)
                            .none { segment -> segment.name in setOf(".git", ".gradle", ".idea", "build", "out") }
            }
            .forEach { path ->
                val staged = stagingDirectory.resolve(path.relativeTo(root))
                staged.parent.createDirectories()
                path.toFile().copyTo(staged.toFile(), overwrite = true)
            }
        currentPreview.replacements.forEach { (path, source) ->
            stagingDirectory.resolve(path.relativeTo(root)).also { file ->
                file.parent.createDirectories()
                file.writeText(source)
            }
        }
        writePreview(currentPreview)
        logger.lifecycle("Staged MineKot assisted-fix plan ${currentPreview.report.planId}.")
    }

    internal fun confirmedPreview(document: MineKotAssistRequestDocument): MineKotAssistPreview {
        val currentPreview = MineKotAssistedFixEngine(projectDirectory.get().asFile.toPath()).preview(document)
        val confirmation = document.confirmation ?: throw GradleException(
            "MineKot assisted fixes require confirmation.plan-id and confirmation.confirmed=true.",
        )
        if (!confirmation.confirmed || confirmation.planId != currentPreview.report.planId) {
            throw GradleException(
                "MineKot assisted-fix confirmation is missing or stale; run mineKotAssistPreview again.",
            )
        }
        val rejected = currentPreview.report.candidates.filter { candidate ->
            document.requests.any { request -> request.findingId == candidate.findingId } && candidate.status != "ready"
        }
        if (rejected.isNotEmpty()) {
            throw GradleException(
                rejected.joinToString(
                    prefix = "MineKot assisted fixes contain rejected requests:\n- ",
                    separator = "\n- ",
                ) { candidate -> "${candidate.path}:${candidate.line}: ${candidate.message}" },
            )
        }
        return currentPreview
    }
}

/** Publishes one confirmed, compiled assisted-fix stage. */
@DisableCachingByDefault(because = "Publishes explicitly confirmed source changes.")
abstract class MineKotAssistApplyTask : MineKotAssistStageTask() {
    /** Revalidates current fingerprints and transactionally publishes compiled staged sources. */
    @TaskAction
    override fun preview() {
        val currentPreview = confirmedPreview(readRequestDocument())
        val root = projectDirectory.get().asFile.toPath()
        val reportPath = reportDirectory.get().asFile.toPath()
        val stagingDirectory = reportPath.resolve("staged")
        val expectedBaseline = assistJson.decodeFromString<Map<String, String>>(
            reportPath.resolve(baselineFileName).readText(),
        )
        val currentBaseline = assistSourceHashes(root)
        if (currentBaseline != expectedBaseline) {
            val changed = (expectedBaseline.keys + currentBaseline.keys)
                .filter { path -> expectedBaseline[path] != currentBaseline[path] }
                .sorted()
            throw GradleException("Repository sources changed during mineKotAssistApply: ${changed.joinToString()}")
        }
        val replacements = currentPreview.replacements.map { (path, source) ->
            val staged = stagingDirectory.resolve(path.relativeTo(root))
            if (!staged.exists() || staged.readText() != source) {
                throw GradleException("Compiled assisted-fix stage is missing or stale for ${path}.")
            }
            path to staged.toFile().readBytes()
        }.toMap()
        MineKotTransactionalPublisher.publish(
            root = root,
            replacements = replacements,
            transactionDirectory = reportDirectory.get().asFile.toPath().resolve("transaction"),
        )
        writePreview(currentPreview)
        logger.lifecycle("Applied MineKot assisted-fix plan ${currentPreview.report.planId}.")
    }
}

private fun assistSourceHashes(root: java.nio.file.Path): Map<String, String> =
    root.walk()
        .filter { path ->
            path.isRegularFile() && path.extension in setOf("kt", "kts") &&
                    path.relativeTo(root).none { segment ->
                        segment.name in setOf(".git", ".gradle", ".idea", "build", "out")
                    }
        }
        .associate { path ->
            path.relativeTo(root).invariantSeparatorsPathString to assistSha256(path.readBytes())
        }
        .toSortedMap()

private fun assistSha256(value: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value)
        .joinToString("") { byte -> "%02x".format(byte) }
