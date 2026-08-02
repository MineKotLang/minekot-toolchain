package org.minekot.toolchain

import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.io.path.*

/** Copies Kotlin sources into a disposable formatting workspace. */
@DisableCachingByDefault(because = "Stages current mutable repository sources.")
abstract class MineKotFormatStageTask : DefaultTask() {
    /** Filesystem service. */
    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    init {
        outputs.upToDateWhen { false }
    }

    /** Repository root containing source files. */
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    /** Disposable source mirror under the build directory. */
    @get:OutputDirectory
    abstract val stagingDirectory: DirectoryProperty

    /** Repository source hashes captured before staging. */
    @get:OutputFile
    abstract val baselineFile: RegularFileProperty

    /** Recreates the source mirror without modifying repository sources. */
    @TaskAction
    fun stage() {
        val root = projectDirectory.get().asFile.toPath()
        val staging = stagingDirectory.get().asFile.toPath()
        fileSystemOperations.delete { operation -> operation.delete(staging.toFile()) }
        val baseline = sourceHashes(root)
        sourceFiles(root).forEach { source ->
            val target = staging.resolve(source.relativeTo(root))
            target.parent.createDirectories()
            source.toFile().copyTo(target.toFile(), overwrite = true)
        }
        baselineFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(formatJson.encodeToString(baseline) + "\n")
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

/** Compiles one staged Kotlin/JVM compilation with original compiler arguments. */
@DisableCachingByDefault(because = "Validates mutable staged sources with absolute compiler paths.")
abstract class MineKotStagedKotlinCompileTask : DefaultTask() {
    /** Kotlin compiler runtime. */
    @get:Classpath
    abstract val compilerClasspath: ConfigurableFileCollection

    /** Original compilation dependency classpath and producer wiring. */
    @get:Classpath
    abstract val compilationClasspath: ConfigurableFileCollection

    /** Serialized arguments from original Kotlin compilation task. */
    @get:Input
    abstract val compilerArguments: ListProperty<String>

    /** Original repository source paths mapped to staged mirrors. */
    @get:Input
    abstract val sourcePathMappings: MapProperty<String, String>

    /** Associated and generated outputs mapped to staged outputs. */
    @get:Input
    abstract val associatedOutputMappings: MapProperty<String, String>

    /** Unsupported source-derived generator diagnostics. */
    @get:Input
    abstract val unsupportedGeneratorProvenance: ListProperty<String>

    /** Original compilation destination replaced in serialized arguments. */
    @get:Input
    abstract val originalDestinationPath: Property<String>

    /** Original Kotlin source arguments removed from serialized compiler arguments. */
    @get:Input
    abstract val originalKotlinSourcePaths: ListProperty<String>

    /** Effective staged and retained Kotlin sources compiled by this task. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val compilationKotlinSources: ConfigurableFileCollection

    /** Generated Java sources unsupported by Kotlin-only staged compilation. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val unsupportedGeneratedJavaSources: ConfigurableFileCollection

    /** Isolated output for this staged compilation. */
    @get:OutputDirectory
    abstract val destination: DirectoryProperty

    /** Process execution service. */
    @get:Inject
    abstract val execOperations: ExecOperations

    /** Filesystem service. */
    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    init {
        outputs.upToDateWhen { false }
    }

    /** Rewrites mutable paths then invokes Kotlin/JVM compiler. */
    @TaskAction
    fun compile() {
        val unsupportedGenerators = unsupportedGeneratorProvenance.get()
        if (unsupportedGenerators.isNotEmpty()) {
            throw GradleException(
                unsupportedGenerators.joinToString(
                    prefix = "Unsupported staged Kotlin generators:\n- ",
                    separator = "\n- ",
                ),
            )
        }
        val generatedJavaSources = unsupportedGeneratedJavaSources.files.filter(File::isFile)
        if (generatedJavaSources.isNotEmpty()) {
            throw GradleException(
                "Staged KSP Java output is unsupported: ${generatedJavaSources.joinToString()}",
            )
        }
        val output = destination.get().asFile
        fileSystemOperations.delete { operation -> operation.delete(output) }
        output.mkdirs()
        val replacements = buildMap {
            put(originalDestinationPath.get(), output.absolutePath)
            putAll(associatedOutputMappings.get())
            putAll(sourcePathMappings.get())
        }
        val orderedReplacements = replacements.entries.sortedByDescending { (original) -> original.length }
        val originalSources = originalKotlinSourcePaths.get().toSet()
        val arguments = compilerArguments.get()
            .filterNot { argument -> argument in originalSources }
            .map { argument ->
                orderedReplacements.fold(argument) { value, (original, staged) -> value.replace(original, staged) }
            }
            .toMutableList()
        val classpath = compilationClasspath.files
            .map { dependency ->
                orderedReplacements.fold(dependency.absolutePath) { value, (original, staged) ->
                    value.replace(original, staged)
                }
            }
            .distinct()
            .joinToString(File.pathSeparator)
        val classpathIndex = arguments.indexOf("-classpath")
        if (classpathIndex >= 0) {
            arguments[classpathIndex + 1] = classpath
        } else {
            arguments.addAll(0, listOf("-classpath", classpath))
        }
        arguments += compilationKotlinSources.files
            .filter { source -> source.isFile && source.extension in formatSourceExtensions }
            .map(File::getAbsolutePath)
            .sorted()
        val missingSources = sourcePathMappings.get().values
            .map(::File)
            .filterNot(File::isFile)
        if (missingSources.isNotEmpty()) {
            throw GradleException("Missing staged Kotlin sources: ${missingSources.joinToString()}")
        }
        execOperations.javaexec { execution ->
            execution.classpath(compilerClasspath)
            execution.mainClass.set("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
            execution.args(arguments)
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

    /** Repository source hashes captured before staging. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val baselineFile: RegularFileProperty

    /** Applies staged files only after all task dependencies succeed. */
    @TaskAction
    fun apply() {
        val root = projectDirectory.get().asFile.toPath()
        val staging = stagingDirectory.get().asFile.toPath()
        val expectedBaseline = formatJson.decodeFromString<Map<String, String>>(baselineFile.get().asFile.readText())
        val currentBaseline = sourceHashes(root)
        if (currentBaseline != expectedBaseline) {
            val changed = (expectedBaseline.keys + currentBaseline.keys)
                .filter { path -> expectedBaseline[path] != currentBaseline[path] }
                .sorted()
            throw GradleException("Repository sources changed during mineKotFormat: ${changed.joinToString()}")
        }
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
            path.relativeTo(root).invariantSeparatorsPathString to sha256(path.readBytes())
        }
        .toSortedMap()

private fun sha256(value: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value)
        .joinToString("") { byte -> "%02x".format(byte) }

private val formatJson: Json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}
private val formatSourceExtensions: Set<String> = setOf("kt", "kts")
private val formatIgnoredDirectories: Set<String> = setOf(".git", ".gradle", ".idea", "build", "out")
