package org.minekot.toolchain

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.minekot.inspections.loader.RulesHostDescriptor
import org.minekot.inspections.loader.RulesHostType
import org.minekot.inspections.loader.RulesHttpRequest
import org.minekot.inspections.loader.RulesHttpResponse
import org.minekot.inspections.loader.RulesHttpTransport
import org.minekot.inspections.loader.RulesReleaseReference
import org.minekot.inspections.loader.runtime.ContentAddressedRulesCache
import org.minekot.inspections.loader.runtime.DefaultRulesResolver
import org.minekot.inspections.loader.runtime.RulesChannelIndexCache
import org.minekot.inspections.loader.runtime.SigstoreRulesSignatureVerifier
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists

/** Coalesces dynamic-rules resolution within one Gradle invocation. */
abstract class MineKotRulesResolutionService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private val lock = ReentrantLock()

    /** Runs one cache or network resolution at a time. */
    fun <T> resolve(action: () -> T): T = lock.withLock(action)

    override fun close() = Unit
}

/** Resolves one exact verified rules lock and materializes a stable Detekt classpath file. */
@CacheableTask
abstract class ResolveMineKotRulesTask : DefaultTask() {
    /** Exact rules release version. */
    @get:Input
    abstract val rulesVersion: Property<String>

    /** Pinned signed-manifest digest. */
    @get:Input
    abstract val manifestSha256: Property<String>

    /** Whether network access is forbidden. */
    @get:Input
    abstract val offline: Property<Boolean>

    /** Host Java feature version. */
    @get:Input
    abstract val javaVersion: Property<Int>

    /** Shared content-addressed cache outside task outputs. */
    @get:LocalState
    abstract val cacheDirectory: DirectoryProperty

    /** Stable verified rules JAR consumed by every Detekt execution. */
    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    /** Invocation-wide resolution lock. */
    @get:Internal
    abstract val resolutionService: Property<MineKotRulesResolutionService>

    /** Resolves, verifies, leases, and materializes the exact requested artifact. */
    @TaskAction
    fun resolveRules() {
        resolutionService.get().resolve {
            val cachePath = cacheDirectory.get().asFile.toPath()
            val version = rulesVersion.get()
            val digest = manifestSha256.get()
            RulesChannelIndexCache(cachePath).load()?.resolved?.index?.releases
                ?.firstOrNull { release -> release.version == version }
                ?.let { release ->
                    require(release.manifestSha256 == digest) {
                        "Rules $version manifest digest differs from the verified stable channel."
                    }
                    require(!release.securityRevoked) {
                        "Rules $version are security-revoked by the verified stable channel."
                    }
                    if (release.withdrawn) logger.warn("Pinned MineKot rules $version were withdrawn from stable.")
                }
            val resolver = DefaultRulesResolver(
                transport = JdkRulesHttpTransport(),
                signatureVerifier = SigstoreRulesSignatureVerifier(),
                cache = ContentAddressedRulesCache(cachePath),
            )
            val reference = RulesReleaseReference(version, digest)
            val host = RulesHostDescriptor(
                hostType = RulesHostType.DETEKT,
                hostVersion = DETEKT_VERSION,
                kotlinVersion = KOTLIN_VERSION,
                coreVersion = CORE_VERSION,
                javaVersion = javaVersion.get(),
                spiMajor = SPI_MAJOR,
            )
            resolver.resolve(reference, host, offline.get()).lease.use { lease ->
                val output = outputJar.get().asFile.toPath()
                output.parent.createDirectories()
                val temporary = createTempFile(output.parent, ".minekot-rules-", ".jar")
                try {
                    lease.jarPath.copyTo(temporary, overwrite = true)
                    FileChannel.open(temporary, WRITE).use { channel -> channel.force(true) }
                    Files.move(temporary, output, ATOMIC_MOVE, REPLACE_EXISTING)
                } finally {
                    temporary.deleteIfExists()
                }
            }
        }
    }

    private companion object {
        const val DETEKT_VERSION = "2.0.0-alpha.6"
        const val KOTLIN_VERSION = "2.4.20"
        const val CORE_VERSION = "1.0.2"
        const val SPI_MAJOR = 1
    }
}

private class JdkRulesHttpTransport : RulesHttpTransport {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    override fun get(request: RulesHttpRequest): RulesHttpResponse {
        val httpRequest = HttpRequest.newBuilder(URI.create(request.url))
            .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "minekot-toolchain")
            .GET()
            .build()
        val response = client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
        val body = response.body().use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size().toLong() + count <= request.maximumBytes) {
                    "Rules response exceeds byte limit."
                }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        return RulesHttpResponse(response.statusCode(), body)
    }

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS = 20L
        const val REQUEST_TIMEOUT_SECONDS = 60L
    }
}
