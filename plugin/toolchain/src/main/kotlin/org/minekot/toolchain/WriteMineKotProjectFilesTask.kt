package org.minekot.toolchain

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.IOException
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.jar.JarFile

/**
 * Writes missing MineKot standard project files.
 */
@DisableCachingByDefault(because = "Small template writer with project-local output.")
abstract class WriteMineKotProjectFilesTask : DefaultTask() {
    /**
     * Directory that receives the project template files.
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /**
     * Writes bundled standard project files without replacing existing files.
     *
     * @throws IOException If file writing fails.
     */
    @TaskAction
    @Throws(IOException::class)
    fun writeProjectFiles() {
        projectResources().forEach { descriptor ->
            writeResourceIfMissing(descriptor.resourcePath, descriptor.outputPath)
        }
    }

    @Throws(IOException::class)
    private fun writeResourceIfMissing(resourceName: String, relativePath: String) {
        val target = outputDirectory.file(relativePath).get().asFile
        if (target.exists()) {
            return
        }
        val text = javaClass.classLoader.getResource(resourceName)?.readText()
            ?: error("Missing bundled resource ${resourceName}.")
        target.parentFile.mkdirs()
        target.writeText(text)
    }

    @Throws(IOException::class)
    private fun projectResources(): List<MineKotResourceDescriptor> =
        resourcePaths("project").map { resourcePath ->
            MineKotResourceDescriptor(resourcePath, resourcePath.removePrefix("project/").toOutputPath())
        }

    @Throws(IOException::class)
    private fun resourcePaths(root: String): List<String> {
        val resource = javaClass.classLoader.getResource(root) ?: error("Missing bundled resource directory ${root}.")
        return when (resource.protocol) {
            "file" -> fileResourcePaths(root, resource)
            "jar" -> jarResourcePaths(root, resource)
            else -> error("Unsupported resource protocol ${resource.protocol}.")
        }
    }

    private fun fileResourcePaths(root: String, resource: URL): List<String> {
        val directory = File(URLDecoder.decode(resource.path, StandardCharsets.UTF_8))
        return directory.walkTopDown()
            .filter(File::isFile)
            .map { file -> "${root}/${file.relativeTo(directory).invariantSeparatorsPath}" }
            .sorted()
            .toList()
    }

    @Throws(IOException::class)
    private fun jarResourcePaths(root: String, resource: URL): List<String> {
        val jarPath = resource.path.substringBefore("!").removePrefix("file:")
        return JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8)).use { jar ->
            jar.entries().asSequence()
                .map { entry -> entry.name }
                .filter { name -> name.startsWith("${root}/") && !name.endsWith("/") }
                .sorted()
                .toList()
        }
    }

    private fun String.toOutputPath(): String =
        projectOutputNames[this] ?: this

    private companion object {
        private val projectOutputNames: Map<String, String> = mapOf(
            "gitattributes" to ".gitattributes",
            "license" to "LICENSE",
            "notice" to "NOTICE",
            "readme" to "README.md",
            "changelog" to "CHANGELOG.md",
        )
    }
}
