package org.minekot.kotlin.io

import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.*

/**
 * Reads UTF-8 text from this path.
 *
 * @return File contents.
 */
fun Path.readMineKotText(): String =
    this@readMineKotText.readText(Charsets.UTF_8)

/**
 * Writes UTF-8 text to this path after creating the parent directory.
 *
 * @param text Text to write.
 */
fun Path.writeMineKotText(text: String) {
    this@writeMineKotText.createMineKotParentDirectories()
    this@writeMineKotText.writeText(text, Charsets.UTF_8)
}

/**
 * Writes UTF-8 text through a temporary sibling file.
 *
 * @param text Text to write.
 */
fun Path.writeMineKotTextAtomic(text: String) {
    this@writeMineKotTextAtomic.createMineKotParentDirectories()
    val temporary = this@writeMineKotTextAtomic.resolveSibling("${this@writeMineKotTextAtomic.fileName}.tmp")
    temporary.writeText(text, Charsets.UTF_8)
    moveMineKotReplacing(temporary, this)
}

/**
 * Reads UTF-8 lines from this path.
 *
 * @return File lines.
 */
fun Path.readMineKotLines(): List<String> =
    this@readMineKotLines.readLines(Charsets.UTF_8)

/**
 * Writes UTF-8 lines to this path after creating the parent directory.
 *
 * @param lines Lines to write.
 */
fun Path.writeMineKotLines(lines: Iterable<String>) {
    this@writeMineKotLines.createMineKotParentDirectories()
    this@writeMineKotLines.writeLines(lines, Charsets.UTF_8)
}

/**
 * Reads all bytes from this path.
 *
 * @return File bytes.
 */
fun Path.readMineKotBytes(): ByteArray =
    this@readMineKotBytes.readBytes()

/**
 * Writes bytes to this path after creating the parent directory.
 *
 * @param bytes Bytes to write.
 */
fun Path.writeMineKotBytes(bytes: ByteArray) {
    this@writeMineKotBytes.createMineKotParentDirectories()
    this@writeMineKotBytes.writeBytes(bytes)
}

/**
 * Writes UTF-8 text only when this path does not already exist.
 *
 * @param text Text to write.
 * @return True when the text was written.
 */
fun Path.writeMineKotTextIfMissing(text: String): Boolean =
    if (this@writeMineKotTextIfMissing.exists()) {
        false
    } else {
        this@writeMineKotTextIfMissing.writeMineKotText(text)
        true
    }

/**
 * Creates parent directories for this path.
 *
 * @return This path.
 */
fun Path.createMineKotParentDirectories(): Path {
    this@createMineKotParentDirectories.parent?.createDirectories()
    return this
}

/**
 * Moves [source] to [target], replacing the target and preferring an atomic move.
 *
 * @param source Source path.
 * @param target Target path.
 */
fun moveMineKotReplacing(source: Path, target: Path) {
    runCatching {
        source.moveTo(target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }.getOrElse {
        source.moveTo(target, StandardCopyOption.REPLACE_EXISTING)
    }
}
