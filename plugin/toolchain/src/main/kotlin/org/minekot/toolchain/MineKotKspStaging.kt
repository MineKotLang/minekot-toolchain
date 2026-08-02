package org.minekot.toolchain

import com.google.devtools.ksp.gradle.KSP_VERSION
import com.google.devtools.ksp.gradle.KspAATask
import com.google.devtools.ksp.gradle.KspGradleConfig
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

/** Version-gated KSP staging adapter loaded only after KSP plugin application. */
internal object MineKotKspStaging {
    /** Registers isolated KSP execution for one staged Kotlin/JVM compilation. */
    fun register(
        project: Project,
        compilation: KotlinCompilation<*>,
        compileTask: TaskProvider<KotlinCompile>,
        stagingDirectory: Provider<Directory>,
        prerequisite: TaskProvider<out Task>,
        childTaskPrefix: String,
        outputDirectoryName: String,
        associatedStagedOutputs: Map<String, Provider<Directory>>,
    ): StagedKspCompilation? {
        val originalTaskName = compileTask.name.replaceFirst("compile", "ksp")
        val originalTask = project.tasks.findByName(originalTaskName) ?: return null
        if (originalTask !is KspAATask) {
            throw GradleException(
                "Unsupported KSP task ${originalTask.path}; MineKot staged compilation requires KSP ${supportedVersion}.",
            )
        }
        val hasSources = originalTask.kspConfig.run {
            (sourceRoots.files + commonSourceRoots.files + javaSourceRoots.files).any(File::isFile)
        }
        if (!hasSources || originalTask.kspConfig.processorClasspath.files.isEmpty()) return null
        val pathOptions = (originalTask.kspConfig.processorOptions.get() + originalTask.kspConfig.apOptions.get())
            .filterKeys { key -> pathOptionMarkers.any { marker -> marker in key.lowercase() } }
        if (pathOptions.isNotEmpty()) {
            throw GradleException(
                "KSP staged compilation cannot isolate path-like processor options: ${pathOptions.keys.sorted()}",
            )
        }
        if (KSP_VERSION != supportedVersion) {
            throw GradleException(
                "Unsupported KSP version ${KSP_VERSION}; MineKot staged compilation requires ${supportedVersion}.",
            )
        }
        val original = project.providers.provider { originalTask }
        val taskSuffix = compilation.name.replaceFirstChar(Char::uppercaseChar)
        val outputRoot = project.layout.buildDirectory.dir(
            "tmp/minekot/${outputDirectoryName}-ksp/${compilation.name}",
        )
        val staged = project.tasks.register(
            "${childTaskPrefix}Ksp${taskSuffix}",
            KspAATask::class.java,
        ) { task ->
            task.group = "verification"
            task.description = "Runs KSP ${supportedVersion} against staged ${compilation.name} Kotlin sources."
            task.dependsOn(prerequisite, associatedStagedOutputs.values)
            task.outputs.upToDateWhen { false }
            task.outputs.cacheIf { false }
            task.kspClasspath.from(original.map { originalTask -> originalTask.kspClasspath })
            task.commandLineArgumentProviders.set(
                original.flatMap { originalTask -> originalTask.commandLineArgumentProviders },
            )
            task.crossCompilationMetadata.from(
                original.map { originalTask -> originalTask.crossCompilationMetadata },
            )
            task.isolatedClassLoaderCacheBuildService.set(
                original.flatMap { originalTask -> originalTask.isolatedClassLoaderCacheBuildService },
            )
            configureStagedKsp(
                project = project,
                target = task.kspConfig,
                original = original,
                stagingDirectory = stagingDirectory,
                outputRoot = outputRoot,
                associatedStagedOutputs = associatedStagedOutputs,
            )
        }
        val originalOutputRoots = original.map { task ->
            task.kspConfig.run {
                listOf(kotlinOutputDir, javaOutputDir, classOutputDir, resourceOutputDir)
                    .map { directory -> directory.get().asFile.absolutePath }
            }
        }
        val outputMappings = original.zip(staged) { originalTask, stagedTask ->
            val originalConfig = originalTask.kspConfig
            val stagedConfig = stagedTask.kspConfig
            mapOf(
                originalConfig.kotlinOutputDir.get().asFile.absolutePath to
                        stagedConfig.kotlinOutputDir.get().asFile.absolutePath,
                originalConfig.javaOutputDir.get().asFile.absolutePath to
                        stagedConfig.javaOutputDir.get().asFile.absolutePath,
                originalConfig.classOutputDir.get().asFile.absolutePath to
                        stagedConfig.classOutputDir.get().asFile.absolutePath,
                originalConfig.resourceOutputDir.get().asFile.absolutePath to
                        stagedConfig.resourceOutputDir.get().asFile.absolutePath,
            )
        }
        return StagedKspCompilation(
            task = staged,
            kotlinOutput = staged.flatMap { task -> task.kspConfig.kotlinOutputDir },
            classOutput = staged.flatMap { task -> task.kspConfig.classOutputDir },
            javaOutput = staged.flatMap { task -> task.kspConfig.javaOutputDir },
            originalOutputRoots = originalOutputRoots,
            outputMappings = outputMappings,
        )
    }

    private fun configureStagedKsp(
        project: Project,
        target: KspGradleConfig,
        original: Provider<KspAATask>,
        stagingDirectory: Provider<Directory>,
        outputRoot: Provider<Directory>,
        associatedStagedOutputs: Map<String, Provider<Directory>>,
    ) {
        target.processorClasspath.from(original.map { task -> task.kspConfig.processorClasspath })
        target.moduleName.set(original.flatMap { task -> task.kspConfig.moduleName })
        target.sourceRoots.from(
            original.map { task ->
                stagedSources(project, stagingDirectory, task.kspConfig.sourceRoots.files)
            },
        )
        target.commonSourceRoots.from(
            original.map { task ->
                stagedSources(project, stagingDirectory, task.kspConfig.commonSourceRoots.files)
            },
        )
        target.javaSourceRoots.from(original.map { task -> task.kspConfig.javaSourceRoots })
        target.libraries.from(
            original.map { task ->
                task.kspConfig.libraries.files.filter { dependency ->
                    associatedStagedOutputs.keys.none { originalPath ->
                        dependency.toPath().toAbsolutePath().normalize()
                            .startsWith(File(originalPath).toPath().toAbsolutePath().normalize())
                    }
                }
            },
        )
        target.libraries.from(associatedStagedOutputs.values)
        target.jdkHome.set(original.flatMap { task -> task.kspConfig.jdkHome })
        target.jdkVersion.set(original.flatMap { task -> task.kspConfig.jdkVersion })
        target.projectBaseDir.set(original.flatMap { task -> task.kspConfig.projectBaseDir })
        target.outputBaseDir.set(outputRoot)
        target.cachesDir.set(outputRoot.map { directory -> directory.dir("caches") })
        target.kotlinOutputDir.set(outputRoot.map { directory -> directory.dir("kotlin") })
        target.javaOutputDir.set(outputRoot.map { directory -> directory.dir("java") })
        target.classOutputDir.set(outputRoot.map { directory -> directory.dir("classes") })
        target.resourceOutputDir.set(outputRoot.map { directory -> directory.dir("resources") })
        target.languageVersion.set(original.flatMap { task -> task.kspConfig.languageVersion })
        target.apiVersion.set(original.flatMap { task -> task.kspConfig.apiVersion })
        target.processorOptions.set(original.flatMap { task -> task.kspConfig.processorOptions })
        target.apOptions.set(original.flatMap { task -> task.kspConfig.apOptions })
        target.logLevel.set(original.flatMap { task -> task.kspConfig.logLevel })
        target.allWarningsAsErrors.set(original.flatMap { task -> task.kspConfig.allWarningsAsErrors })
        target.excludedProcessors.set(original.flatMap { task -> task.kspConfig.excludedProcessors })
        target.jvmTarget.set(original.flatMap { task -> task.kspConfig.jvmTarget })
        target.jvmDefaultMode.set(original.flatMap { task -> task.kspConfig.jvmDefaultMode })
        target.incremental.set(false)
        target.incrementalLog.set(false)
        target.incrementalGraphOrigin.set(original.flatMap { task -> task.kspConfig.incrementalGraphOrigin })
        target.experimentalPsiResolution.set(
            original.flatMap { task -> task.kspConfig.experimentalPsiResolution },
        )
        target.classpathStructure.setFrom(emptyList<File>())
        target.nonJvmLibraries.from(original.map { task -> task.kspConfig.nonJvmLibraries })
        target.platformType.set(original.flatMap { task -> task.kspConfig.platformType })
        target.konanTargetName.set(original.flatMap { task -> task.kspConfig.konanTargetName })
        target.konanHome.set(original.flatMap { task -> task.kspConfig.konanHome })
        target.profilingMode.set(original.flatMap { task -> task.kspConfig.profilingMode })
    }

    private fun stagedSources(
        project: Project,
        stagingDirectory: Provider<Directory>,
        sources: Set<File>,
    ): List<File> {
        val projectRoot = project.layout.projectDirectory.asFile.toPath().toAbsolutePath().normalize()
        val buildRoot = project.layout.buildDirectory.get().asFile.toPath().toAbsolutePath().normalize()
        val stagingRoot = stagingDirectory.get().asFile.toPath().toAbsolutePath().normalize()
        return sources.map { source ->
            val path = source.toPath().toAbsolutePath().normalize()
            if (path.startsWith(projectRoot) && !path.startsWith(buildRoot)) {
                stagingRoot.resolve(projectRoot.relativize(path)).toFile()
            } else {
                source
            }
        }
    }

    private const val supportedVersion: String = "2.3.10"
    private val pathOptionMarkers: Set<String> = setOf("dir", "output", "path", "schema")
}

/** Isolated KSP task and path substitutions consumed by staged Kotlin compilation. */
internal data class StagedKspCompilation(
    val task: TaskProvider<out Task>,
    val kotlinOutput: Provider<Directory>,
    val classOutput: Provider<Directory>,
    val javaOutput: Provider<Directory>,
    val originalOutputRoots: Provider<List<String>>,
    val outputMappings: Provider<Map<String, String>>,
)
