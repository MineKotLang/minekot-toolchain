package org.minekot.toolchain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import kotlin.io.path.*

/** Versioned semantic style-guide review document. */
@Serializable
internal data class MineKotSemanticReviewDocument(
    val schemaVersion: Int = semanticReviewSchemaVersion,
    val sourceFingerprint: String,
    val reviewer: String = "",
    val reviewedAt: String = "",
    val checks: List<MineKotSemanticReviewCheck> = semanticReviewIds.map { id ->
        MineKotSemanticReviewCheck(id = id, confirmed = false)
    },
)

/** One explicit semantic style-guide confirmation. */
@Serializable
internal data class MineKotSemanticReviewCheck(
    val id: String,
    val confirmed: Boolean,
    val notes: String? = null,
)

/** Writes a non-mutating semantic-review template for current sources. */
@DisableCachingByDefault(because = "Preview depends on dynamic repository source state.")
abstract class MineKotReviewPreviewTask : DefaultTask() {
    /** Repository root fingerprinted by this task. */
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    /** Preview report destination. */
    @get:Internal
    abstract val reportDirectory: DirectoryProperty

    /** Writes current review template without changing source files. */
    @TaskAction
    fun preview() {
        val root = projectDirectory.get().asFile.toPath()
        val output = reportDirectory.get().asFile.toPath().resolve(semanticReviewReportName)
        output.parent.toFile().mkdirs()
        output.writeText(
            semanticReviewJson.encodeToString(
                MineKotSemanticReviewDocument(sourceFingerprint = root.mineKotSemanticFingerprint()),
            ) + "\n",
        )
        logger.lifecycle("Wrote MineKot semantic-review template to ${output}.")
    }
}

/** Requires exact, current semantic style-guide confirmation. */
@DisableCachingByDefault(because = "Validation depends on current repository source state and review age.")
abstract class VerifyMineKotSemanticReviewTask : DefaultTask() {
    /** Repository root fingerprinted by this task. */
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    /** Checked-in semantic-review document. */
    @get:Internal
    abstract val reviewFile: RegularFileProperty

    /** Maximum accepted age for external-link confirmation. */
    @get:Input
    abstract val maximumAgeDays: Property<Int>

    /** Validates confirmation, source fingerprint, required checks, and review age. */
    @TaskAction
    fun verify() {
        val root = projectDirectory.get().asFile.toPath()
        val file = reviewFile.asFile.orNull?.takeIf { candidate -> candidate.isFile } ?: throw GradleException(
            "Missing MineKot semantic review; run mineKotReviewPreview and confirm ${reviewFile.get().asFile}.",
        )
        val document = runCatching {
            semanticReviewJson.decodeFromString<MineKotSemanticReviewDocument>(file.readText())
        }.getOrElse { failure ->
            throw GradleException("Invalid MineKot semantic review ${file}: ${failure.message}", failure)
        }
        val violations = buildList {
            if (document.schemaVersion != semanticReviewSchemaVersion) {
                add("schemaVersion must be ${semanticReviewSchemaVersion}")
            }
            if (document.sourceFingerprint != root.mineKotSemanticFingerprint()) {
                add("sourceFingerprint is stale")
            }
            if (document.reviewer.isBlank()) {
                add("reviewer must be non-blank")
            }
            val reviewTime = runCatching { Instant.parse(document.reviewedAt) }.getOrNull()
            if (reviewTime == null) {
                add("reviewedAt must be an ISO-8601 instant")
            } else if (reviewTime.isAfter(Instant.now())) {
                add("reviewedAt must not be in the future")
            }
            val checksById = document.checks.associateBy(MineKotSemanticReviewCheck::id)
            semanticReviewIds.forEach { id ->
                if (checksById[id]?.confirmed != true) {
                    add("check ${id} must be confirmed")
                }
            }
            val unknownIds = checksById.keys - semanticReviewIds
            if (unknownIds.isNotEmpty()) {
                add("unknown checks: ${unknownIds.sorted().joinToString()}")
            }
            if (
                reviewTime != null &&
                Duration.between(reviewTime, Instant.now()).toDays() > maximumAgeDays.get()
            ) {
                add("external-link and semantic review is older than ${maximumAgeDays.get()} days")
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                violations.joinToString(
                    prefix = "MineKot semantic-review violations:\n- ",
                    separator = "\n- ",
                ),
            )
        }
    }
}

/** Computes stable fingerprint for files covered by semantic review. */
internal fun Path.mineKotSemanticFingerprint(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    mineKotSemanticFiles().forEach { path ->
        digest.update(path.relativeTo(this).invariantSeparatorsPathString.toByteArray())
        digest.update(0)
        digest.update(path.readBytes())
        digest.update(0)
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}

private fun Path.mineKotSemanticFiles(): List<Path> = walk()
    .filter { path ->
        path.isRegularFile() &&
                path.relativeTo(this).none { segment -> segment.name in semanticReviewIgnoredDirectories } &&
                (path.extension in semanticReviewExtensions || path.name in semanticReviewFileNames)
    }
    .sortedBy { path -> path.relativeTo(this).invariantSeparatorsPathString }
    .toList()

internal const val semanticReviewSchemaVersion: Int = 1
internal val semanticReviewIds: List<String> = listOf(
    "language.american-english",
    "versions.latest-stable",
    "kotlin.data-driven-design",
    "kotlin.modular-decoupling",
    "kotlin.internal-extensions",
    "kotlin.comment-necessity",
    "kotlin.minimessage-localization",
    "kotlin.blocking-operations",
    "gradle.dependency-scopes",
    "gradle.convention-extraction",
    "gradle.repository-classification",
    "gradle.resolution-strategy",
    "gradle.test-architecture",
    "markdown.content-quality",
    "markdown.external-links",
)
private const val semanticReviewReportName: String = "semantic-review.json"
private val semanticReviewExtensions: Set<String> = setOf("gradle.kts", "kt", "kts", "md", "toml")
private val semanticReviewFileNames: Set<String> = setOf("gradle.properties")
private val semanticReviewIgnoredDirectories: Set<String> = setOf(".git", ".gradle", ".idea", "build", "out")
private val semanticReviewJson: Json = Json {
    encodeDefaults = true
    prettyPrint = true
    prettyPrintIndent = "  "
}
