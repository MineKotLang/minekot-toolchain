package org.minekot.kotlin.io

import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path

/**
 * Converts this path to a URL while capturing failures.
 *
 * @return URL result.
 */
fun Path.toMineKotUrlResult(): Result<URL> =
    runCatching {
        toUri().toURL()
    }

/**
 * Creates a URL class loader from paths.
 *
 * @param parent Parent class loader.
 * @return URL class loader result.
 */
fun Iterable<Path>.toMineKotUrlClassLoaderResult(parent: ClassLoader? = null): Result<URLClassLoader> =
    runCatching {
        val urls = map { path -> path.toMineKotUrlResult().getOrThrow() }.toTypedArray()
        URLClassLoader(urls, parent)
    }
