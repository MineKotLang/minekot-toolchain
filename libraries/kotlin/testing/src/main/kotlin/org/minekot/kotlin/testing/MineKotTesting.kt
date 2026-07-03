package org.minekot.kotlin.testing

import org.junit.jupiter.api.Assertions.assertTrue
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * Asserts that this result succeeded.
 *
 * @return Successful value.
 */
fun <Value> Result<Value>.assertMineKotSuccess(): Value {
    assertTrue(isSuccess) {
        exceptionOrNull()?.message ?: "Expected successful Result."
    }
    return getOrNull() ?: error("Expected successful Result value.")
}

/**
 * Asserts that this result failed.
 *
 * @return Failure cause.
 */
fun Result<*>.assertMineKotFailure(): Throwable {
    assertTrue(isFailure) {
        "Expected failed Result."
    }
    return exceptionOrNull() ?: error("Expected failed Result.")
}

/**
 * Creates a temporary MineKot test project directory.
 *
 * @param name Directory name prefix.
 * @return Temporary project directory.
 * @throws IOException If directory creation fails.
 */
@Throws(IOException::class)
fun createMineKotTestProject(name: String = "minekot-test"): Path =
    Files.createTempDirectory(name)

/**
 * Creates a temporary project and deletes it after use.
 *
 * @param name Directory name prefix.
 * @param block Operation using the project directory.
 * @return Operation result.
 * @throws Throwable If the block execution or directory creation fails.
 */
@Throws(Throwable::class)
fun <Value> useMineKotTestProject(name: String = "minekot-test", block: (Path) -> Value): Value {
    val directory = createMineKotTestProject(name)
    return runCatching {
        block(directory)
    }.also {
        directory.deleteMineKotRecursively()
    }.getOrElse { cause ->
        throw cause
    }
}

/**
 * Asserts that this path exists.
 *
 * @return This path.
 */
fun Path.assertMineKotExists(): Path {
    assertTrue(exists()) {
        "Expected path to exist: ${this}."
    }
    return this
}

/**
 * Asserts that this path is a directory.
 *
 * @return This path.
 */
fun Path.assertMineKotDirectory(): Path {
    assertTrue(isDirectory()) {
        "Expected directory: ${this}."
    }
    return this
}

/**
 * Creates a small Gradle project fixture.
 *
 * @param buildFile Build file text.
 * @param settingsFile Settings file text.
 * @return Project directory.
 * @throws IOException If directory or file creation fails.
 */
@Throws(IOException::class)
fun createMineKotGradleProject(
    buildFile: String,
    settingsFile: String = """pluginManagement { repositories { gradlePluginPortal(); mavenCentral() } }""",
): Path {
    val directory = createMineKotTestProject()
    Files.writeString(directory.resolve("settings.gradle.kts"), settingsFile)
    Files.writeString(directory.resolve("build.gradle.kts"), buildFile)
    return directory
}

private fun Path.deleteMineKotRecursively() {
    if (!exists()) {
        return
    }
    runCatching {
        Files.walk(this).use { paths ->
            paths.sorted(Comparator.reverseOrder())
                .forEach { path -> Files.deleteIfExists(path) }
        }
    }
}
