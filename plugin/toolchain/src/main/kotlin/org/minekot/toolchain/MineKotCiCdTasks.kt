package org.minekot.toolchain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.work.DisableCachingByDefault
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*

private val releaseVersionPattern = Regex(
    "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-(?:alpha|beta|rc)\\.[1-9]\\d*)?$",
)

private val ciCdJson = Json {
    prettyPrint = true
    prettyPrintIndent = "    "
}

/**
 * Writes stable CI metadata for workflow matrix generation.
 */
@CacheableTask
abstract class WriteMineKotCiMetadataTask : DefaultTask() {
    /** Supported target versions. */
    @get:Input
    abstract val supportedVersions: ListProperty<String>

    /** Project paths included in Maven publication. */
    @get:Input
    abstract val publicationProjects: ListProperty<String>

    /** Release tag prefix. */
    @get:Input
    abstract val tagPrefix: Property<String>

    /** Metadata output file. */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /** Writes configured CI metadata as JSON. */
    @TaskAction
    fun writeMetadata() {
        val document = MineKotCiMetadata(
            publicationProjects = publicationProjects.get().sorted(),
            supportedVersions = supportedVersions.get(),
            tagPrefix = tagPrefix.get(),
        )
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(ciCdJson.encodeToString(document) + "\n")
        }
    }
}

/**
 * Verifies checked-in changelog fragments.
 */
@DisableCachingByDefault(because = "Verification has no outputs")
abstract class VerifyMineKotChangesTask : DefaultTask() {
    /** Changelog fragment directory. */
    @get:InputDirectory
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val fragmentsDirectory: DirectoryProperty

    /** Allowed changelog fragment categories. */
    @get:Input
    abstract val categories: ListProperty<String>

    /** Verifies fragment paths, encoding, and content. */
    @TaskAction
    fun verifyChanges() {
        val root = fragmentsDirectory.orNull?.asFile ?: return
        if (!root.exists()) return
        val allowedCategories = categories.get().toSet()
        val violations = root.walkTopDown()
            .filter { file -> file.isFile && file.name != ".gitkeep" }
            .mapNotNull { file ->
                val relative = file.relativeTo(root).invariantSeparatorsPath
                val category = relative.substringBefore('/')
                when {
                    file.extension != "md" -> "${relative}: fragment must use .md extension"
                    category !in allowedCategories -> "${relative}: unsupported category ${category}"
                    file.readBytes().startsWithUtf8Bom() -> "${relative}: UTF-8 BOM is forbidden"
                    "\r" in file.readText() -> "${relative}: CRLF is forbidden"
                    file.readText().isBlank() -> "${relative}: fragment is empty"
                    !file.readText().endsWith("\n") -> "${relative}: final newline is required"
                    else -> null
                }
            }
            .toList()
        check(violations.isEmpty()) {
            "Invalid MineKot changelog fragments:\n${violations.joinToString(separator = "\n")}"
        }
    }
}

/**
 * Folds typed changelog fragments into a checked-in release section.
 */
@DisableCachingByDefault(because = "Task updates checked-in changelog state")
abstract class PrepareMineKotReleaseTask : DefaultTask() {
    /** Changelog fragment directory. */
    @get:Internal
    abstract val fragmentsDirectory: DirectoryProperty

    /** Allowed changelog fragment categories in output order. */
    @get:Input
    abstract val categories: ListProperty<String>

    /** Release version. */
    @get:Input
    abstract val releaseVersion: Property<String>

    /** ISO-8601 release date. */
    @get:Input
    abstract val releaseDate: Property<String>

    /** Full changelog file. */
    @get:OutputFile
    abstract val changelogFile: RegularFileProperty

    /** Writes release section and removes consumed fragments. */
    @TaskAction
    fun prepareRelease() {
        val version = releaseVersion.get()
        require(releaseVersionPattern.matches(version)) { "Unsupported release version ${version}." }
        val root = fragmentsDirectory.get().asFile
        val fragments = root.walkTopDown()
            .filter { file -> file.isFile && file.extension == "md" }
            .groupBy { file -> file.relativeTo(root).invariantSeparatorsPath.substringBefore('/') }
        require(fragments.isNotEmpty()) { "No MineKot changelog fragments exist." }

        val section = buildString {
            append("## ${version} - ${releaseDate.get()}\n\n")
            categories.get().forEach { category ->
                val entries = fragments[category].orEmpty().sortedBy { file -> file.name }
                if (entries.isNotEmpty()) {
                    append("### ${category.replaceFirstChar(Char::uppercase)}\n\n")
                    entries.forEach { file -> append("- ${file.readText().trim()}\n") }
                    append('\n')
                }
            }
        }
        val output = changelogFile.get().asFile
        val existing = output.takeIf { file -> file.exists() }?.readText().orEmpty()
        val body = existing.removePrefix("# Changelog\n").trimStart()
        output.apply {
            parentFile.mkdirs()
            writeText("# Changelog\n\n${section}${body}".trimEnd() + "\n")
        }
        fragments.values.flatten().forEach { file -> check(file.delete()) { "Cannot remove ${file}." } }
        root.walkBottomUp().filter { file -> file.isDirectory && file != root }.forEach { directory ->
            directory.delete()
        }
    }
}

/**
 * Verifies immutable release context before deployment.
 */
@DisableCachingByDefault(because = "Verification has no outputs")
abstract class VerifyMineKotReleaseTask : DefaultTask() {
    /** Release tag. */
    @get:Input
    abstract val releaseTag: Property<String>

    /** Release version. */
    @get:Input
    abstract val releaseVersion: Property<String>

    /** Tagged commit SHA. */
    @get:Input
    abstract val releaseCommit: Property<String>

    /** Current protected branch head SHA. */
    @get:Input
    abstract val protectedBranchCommit: Property<String>

    /** Release tag prefix. */
    @get:Input
    abstract val tagPrefix: Property<String>

    /** Stable-release evidence directory. */
    @get:InputDirectory
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val evidenceDirectory: DirectoryProperty

    /** Required stable-release evidence checklist identifiers. */
    @get:Input
    abstract val requiredEvidenceItems: ListProperty<String>

    /** Verifies tag, version, branch head, and stable evidence. */
    @TaskAction
    fun verifyRelease() {
        val version = releaseVersion.get()
        val tag = releaseTag.get()
        val commit = releaseCommit.get()
        require(releaseVersionPattern.matches(version)) { "Unsupported release version ${version}." }
        check(tag == "${tagPrefix.get()}${version}") { "Release tag ${tag} does not match version ${version}." }
        check(commit == protectedBranchCommit.get()) { "Release tag must point at current protected branch head." }
        if ('-' in version) return

        val evidence = evidenceDirectory.get().asFile.resolve("${tag}.md")
        check(evidence.isFile) { "Stable release evidence is missing: ${evidence}." }
        val content = evidence.readText()
        check("Commit: `${commit}`" in content) { "Release evidence commit does not match ${commit}." }
        val missingItems = requiredEvidenceItems.get().filterNot { item -> "- [x] ${item}" in content }
        check(missingItems.isEmpty()) { "Release evidence is incomplete: ${missingItems}." }
    }
}

/**
 * Creates deterministic release bundle and checksum manifest.
 */
@CacheableTask
abstract class AssembleMineKotReleaseTask : DefaultTask() {
    /** Directory containing collected CI artifacts. */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val artifactsDirectory: DirectoryProperty

    /** Exact artifact file names required in the release. */
    @get:Input
    abstract val expectedArtifacts: ListProperty<String>

    /** Verified release bundle output directory. */
    @get:OutputDirectory
    abstract val bundleDirectory: DirectoryProperty

    /** Creates verified bundle, SHA-256 file, and JSON manifest. */
    @TaskAction
    fun assembleRelease() {
        val source = artifactsDirectory.get().asFile
        val expected = expectedArtifacts.get()
        check(expected.size == expected.toSet().size) { "Expected release artifact names must be unique." }
        val candidates = source.walkTopDown().filter { file -> file.isFile }.groupBy { file -> file.name }
        val missing = expected.filterNot(candidates::containsKey)
        val duplicates = expected.filter { name -> candidates[name].orEmpty().size > 1 }
        check(missing.isEmpty()) { "Missing release artifacts: ${missing}." }
        check(duplicates.isEmpty()) { "Duplicate release artifacts: ${duplicates}." }

        val output = bundleDirectory.get().asFile
        output.deleteRecursively()
        output.mkdirs()
        val manifest = expected.sorted().map { name ->
            val target = output.resolve(name)
            candidates.getValue(name).single().copyTo(target)
            MineKotReleaseArtifact(name = name, sha256 = target.sha256(), size = target.length())
        }
        output.resolve("SHA256SUMS").writeText(
            manifest.joinToString(separator = "\n", postfix = "\n") { artifact ->
                "${artifact.sha256}  ${artifact.name}"
            },
        )
        output.resolve("manifest.json").writeText(
            ciCdJson.encodeToString(MineKotReleaseManifest(artifacts = manifest)) + "\n",
        )
    }
}

/**
 * Inspects remote Maven paths before or after publication.
 */
@DisableCachingByDefault(because = "Remote repository state is external and mutable")
abstract class VerifyMineKotRemotePublicationTask : DefaultTask() {
    /** Maven repository base URL. */
    @get:Input
    abstract val repositoryUrl: Property<String>

    /** Expected repository-relative artifact paths. */
    @get:Input
    abstract val expectedPaths: ListProperty<String>

    /** Optional repository username. */
    @get:Internal
    abstract val username: Property<String>

    /** Optional repository password. */
    @get:Internal
    abstract val password: Property<String>

    /** Remote-state report file. */
    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    /** Reports empty or complete state and rejects partial publication. */
    @TaskAction
    fun verifyRemotePublication() {
        val paths = expectedPaths.get().sorted()
        require(paths.isNotEmpty()) { "At least one remote Maven path is required." }
        require(username.isPresent == password.isPresent) {
            "Maven repository username and password must be configured together."
        }
        val existing = paths.filter(::existsRemotely)
        val state = when (existing.size) {
            0 -> MineKotRemotePublicationState.EMPTY
            paths.size -> MineKotRemotePublicationState.COMPLETE
            else -> MineKotRemotePublicationState.PARTIAL
        }
        val report = MineKotRemotePublicationReport(
            existing = existing,
            missing = paths - existing.toSet(),
            state = state,
        )
        reportFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(ciCdJson.encodeToString(report) + "\n")
        }
        if (state == MineKotRemotePublicationState.PARTIAL) {
            throw GradleException("Partial Maven publication detected; inspect ${reportFile.get().asFile}.")
        }
    }

    private fun existsRemotely(path: String): Boolean {
        val connection = URI.create(repositoryUrl.get().trimEnd('/') + "/" + path.trimStart('/'))
            .toURL()
            .openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        if (username.isPresent && password.isPresent) {
            val credentials = "${username.get()}:${password.get()}".toByteArray(StandardCharsets.UTF_8)
            connection.setRequestProperty("Authorization", "Basic ${Base64.getEncoder().encodeToString(credentials)}")
        }
        connection.connectTimeout = HTTP_TIMEOUT_MILLISECONDS
        connection.readTimeout = HTTP_TIMEOUT_MILLISECONDS
        return try {
            when (val responseCode = connection.responseCode) {
                in HTTP_SUCCESS_RANGE -> true
                HTTP_NOT_FOUND -> false
                else -> throw GradleException(
                    "Maven repository returned HTTP ${responseCode} for ${path}.",
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val HTTP_TIMEOUT_MILLISECONDS = 10_000
        const val HTTP_NOT_FOUND = 404
        val HTTP_SUCCESS_RANGE = 200..299
    }
}

@Serializable
private data class MineKotCiMetadata(
    val publicationProjects: List<String>,
    val supportedVersions: List<String>,
    val tagPrefix: String,
)

@Serializable
private data class MineKotReleaseManifest(val artifacts: List<MineKotReleaseArtifact>)

@Serializable
private data class MineKotReleaseArtifact(val name: String, val sha256: String, val size: Long)

@Serializable
private data class MineKotRemotePublicationReport(
    val existing: List<String>,
    val missing: List<String>,
    val state: MineKotRemotePublicationState,
)

@Serializable
private enum class MineKotRemotePublicationState {
    COMPLETE,
    EMPTY,
    PARTIAL,
}

private fun ByteArray.startsWithUtf8Bom(): Boolean =
    size >= UTF_8_BOM.size && take(UTF_8_BOM.size) == UTF_8_BOM.toList()

private fun java.io.File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        generateSequence { input.read(buffer).takeIf { count -> count >= 0 } }
            .forEach { count -> digest.update(buffer, 0, count) }
    }
    return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private val UTF_8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
