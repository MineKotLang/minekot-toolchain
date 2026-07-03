package org.minekot.toolchain

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Writes MineKot codestyle template files into a project.
 */
@DisableCachingByDefault(because = "Small template writer with project-local output.")
abstract class WriteMineKotCodestyleTask : DefaultTask() {
    /**
     * Directory that receives the codestyle template files.
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /**
     * Writes all bundled codestyle resources.
     */
    @TaskAction
    fun writeCodestyle() {
        mineKotCodestyleDescriptors.forEach { descriptor ->
            writeResource(descriptor.resourcePath, descriptor.outputPath)
        }
    }

    private fun writeResource(resourceName: String, relativePath: String) {
        val text = javaClass.classLoader.getResource(resourceName)?.readText()
            ?: error("Missing bundled resource ${resourceName}.")
        val target = outputDirectory.file(relativePath).get().asFile
        target.parentFile.mkdirs()
        target.writeText(text)
    }
}
