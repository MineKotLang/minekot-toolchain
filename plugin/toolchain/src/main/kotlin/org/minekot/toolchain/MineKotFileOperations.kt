package org.minekot.toolchain

import org.gradle.api.GradleException
import java.io.File

internal fun ClassLoader.readMineKotResourceText(resourcePath: String): String =
    runCatching {
        getResource(resourcePath)?.readText()
            ?: throw GradleException("Missing bundled resource ${resourcePath}.")
    }.getOrElse { failure ->
        throw failure.asGradleException("Failed to read bundled resource ${resourcePath}.")
    }

internal fun File.writeMineKotText(text: String) {
    ensureMineKotParentDirectory()
    runCatching {
        writeText(text)
    }.getOrElse { failure ->
        throw failure.asGradleException("Failed to write ${path}.")
    }
}

internal fun File.ensureMineKotParentDirectory() {
    val parent = parentFile ?: return
    if (!parent.isDirectory && !parent.mkdirs()) {
        throw GradleException("Failed to create directory ${parent.path}.")
    }
}

internal fun Throwable.asGradleException(message: String): GradleException =
    this as? GradleException ?: GradleException(message, this)
