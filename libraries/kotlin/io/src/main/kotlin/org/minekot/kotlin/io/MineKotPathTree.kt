package org.minekot.kotlin.io

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Ensures this directory exists.
 *
 * @return This path.
 * @throws IOException If creation fails.
 */
@Throws(IOException::class)
fun Path.ensureMineKotDirectory(): Path =
    this@ensureMineKotDirectory.createDirectories()

/**
 * Deletes this path if it exists and captures failures.
 *
 * @return True when a file was deleted.
 */
fun Path.deleteMineKotIfExistsResult(): Result<Boolean> =
    runCatching {
        this@deleteMineKotIfExistsResult.deleteIfExists()
    }

/**
 * Deletes this path recursively when it exists.
 *
 * @return Number of deleted paths.
 * @throws IOException If deletion fails.
 */
@Throws(IOException::class)
fun Path.deleteMineKotRecursivelyIfExists(): Int {
    if (!this@deleteMineKotRecursivelyIfExists.exists()) {
        return 0
    }
    val paths = Files.walk(this).use { stream ->
        stream.sorted(Comparator.reverseOrder()).toList()
    }
    paths.forEach { path ->
        path.deleteIfExists()
    }
    return paths.size
}

/**
 * Deletes this path recursively and captures failures.
 *
 * @return Deleted path count result.
 */
fun Path.deleteMineKotRecursivelyResult(): Result<Int> =
    runCatching {
        deleteMineKotRecursivelyIfExists()
    }

/**
 * Walks regular files under this path in deterministic order.
 *
 * @return Sorted file paths.
 * @throws IOException If walking fails.
 */
@Throws(IOException::class)
fun Path.walkMineKotFiles(): List<Path> =
    if (this@walkMineKotFiles.isDirectory()) {
        this@walkMineKotFiles.walk().filter { path -> !path.isDirectory() }.sortedBy(Path::toString).toList()
    } else {
        emptyList()
    }

/**
 * Copies files from this directory to a target directory without replacing existing files.
 *
 * @param targetDirectory Target directory.
 * @return Copied target paths.
 * @throws IOException If copying fails.
 */
@Throws(IOException::class)
fun Path.copyMineKotDirectoryMissingOnly(targetDirectory: Path): List<Path> {
    val sourceDirectory = this
    return sourceDirectory.walkMineKotFiles().mapNotNull { source ->
        val target = targetDirectory.resolve(source.relativeTo(sourceDirectory).toString())
        if (target.exists()) {
            null
        } else {
            target.createMineKotParentDirectories()
            source.copyTo(target)
            target
        }
    }
}

/**
 * Resolves a child path and rejects traversal outside this directory.
 *
 * @param relative Relative path text.
 * @return Safe resolved child path.
 */
fun Path.resolveMineKotSafe(relative: String): Path {
    val root = this@resolveMineKotSafe.toAbsolutePath().normalize()
    val child = root.resolve(relative).normalize()
    require(child.startsWith(root)) {
        "Path ${relative} escapes ${this}."
    }
    return child
}

/**
 * Returns this file extension without a leading dot.
 *
 * @return File extension, or empty string.
 */
fun Path.mineKotExtension(): String =
    this@mineKotExtension.fileName.toString().substringAfterLast('.', missingDelimiterValue = "")

/**
 * Returns this file name without its final extension.
 *
 * @return File name without extension.
 */
fun Path.mineKotNameWithoutExtension(): String =
    this@mineKotNameWithoutExtension.fileName.toString()
        .substringBeforeLast('.', missingDelimiterValue = this@mineKotNameWithoutExtension.fileName.toString())
