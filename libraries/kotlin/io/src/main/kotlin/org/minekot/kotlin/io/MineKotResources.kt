package org.minekot.kotlin.io

import java.io.IOException
import java.net.JarURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.exists
import kotlin.io.path.relativeTo

/**
 * Copies a classpath resource to this path only when missing.
 *
 * @param resourceName Classpath resource name.
 * @param classLoader Class loader used to find the resource.
 * @return True when resource was copied.
 * @throws IOException If copying fails.
 */
@Throws(IOException::class)
fun Path.copyMineKotResourceIfMissing(
    resourceName: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
): Boolean {
    if (this@copyMineKotResourceIfMissing.exists()) {
        return false
    }
    this@copyMineKotResourceIfMissing.createMineKotParentDirectories()
    val resource = classLoader.getResource(resourceName) ?: error("Missing resource ${resourceName}.")
    resource.openStream().use { input ->
        java.nio.file.Files.copy(input, this@copyMineKotResourceIfMissing)
    }
    return true
}

/**
 * Installs a classpath resource as a default file.
 *
 * @param resourceName Classpath resource name.
 * @param classLoader Class loader used to find the resource.
 * @return True when resource was copied.
 * @throws IOException If copying fails.
 */
@Throws(IOException::class)
fun Path.installMineKotDefaultResource(
    resourceName: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
): Boolean =
    copyMineKotResourceIfMissing(resourceName, classLoader)

/**
 * Copies a classpath resource tree into this directory without replacing existing files.
 *
 * @param resourceRoot Classpath resource directory.
 * @param classLoader Class loader used to find the resource tree.
 * @return Copied target paths.
 * @throws IOException If copying fails.
 * @throws URISyntaxException If the resource URI is invalid.
 */
@Throws(IOException::class, URISyntaxException::class)
fun Path.copyMineKotResourceTreeMissingOnly(
    resourceRoot: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
): List<Path> {
    val root = resourceRoot.trimEnd('/')
    val resource = classLoader.getResource(root) ?: error("Missing resource ${root}.")
    val resources = when (resource.protocol) {
        "file" -> Path.of(resource.toURI()).walkMineKotFiles().map { file ->
            root to file.relativeTo(Path.of(resource.toURI())).toString().replace('\\', '/')
        }

        "jar" -> jarResourceNames(resource.toString(), root).map { name ->
            root to name.removePrefix("${root}/")
        }

        else -> error("Unsupported resource protocol ${resource.protocol}.")
    }
    return resources.mapNotNull { (prefix, relativePath) ->
        val target = this@copyMineKotResourceTreeMissingOnly.resolve(relativePath)
        if (target.exists()) {
            null
        } else {
            target.copyMineKotResourceIfMissing("${prefix}/${relativePath}", classLoader)
            target
        }
    }
}

@Throws(IOException::class, URISyntaxException::class)
private fun jarResourceNames(resourceUrl: String, root: String): List<String> {
    val connection = URI(resourceUrl).toURL().openConnection() as JarURLConnection
    return JarFile(Path.of(connection.jarFileURL.toURI()).toFile()).use { jar ->
        jar.entries().asSequence()
            .filter { entry -> !entry.isDirectory && entry.name.startsWith("${root}/") }
            .map { entry -> entry.name }
            .sorted()
            .toList()
    }
}
