package org.minekot.kotlin.io

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class MineKotIoTest {
    @Test
    fun `text helpers round trip utf8`() {
        val path = Files.createTempDirectory("minekot-io").resolve("sample.txt")

        path.writeMineKotText("MineKot")

        assertEquals("MineKot", path.readMineKotText())
    }

    @Test
    fun `line helpers round trip utf8`() {
        val path = Files.createTempDirectory("minekot-io").resolve("sample.txt")

        path.writeMineKotLines(listOf("Mine", "Kot"))

        assertEquals(listOf("Mine", "Kot"), path.readMineKotLines())
    }

    @Test
    fun `missing text helper preserves existing file`() {
        val path = Files.createTempDirectory("minekot-io").resolve("sample.txt")

        assertTrue(path.writeMineKotTextIfMissing("first"))
        assertFalse(path.writeMineKotTextIfMissing("second"))
        assertEquals("first", path.readMineKotText())
    }

    @Test
    fun `walk helper returns files in deterministic order`() {
        val directory = Files.createTempDirectory("minekot-io")
        directory.resolve("b.txt").writeMineKotText("b")
        directory.resolve("a.txt").writeMineKotText("a")

        assertEquals(
            listOf(directory.resolve("a.txt"), directory.resolve("b.txt")),
            directory.walkMineKotFiles(),
        )
    }

    @Test
    fun `atomic text helper writes and creates parent`() {
        val path = Files.createTempDirectory("minekot-io").resolve("nested/sample.txt")

        path.writeMineKotTextAtomic("MineKot")

        assertEquals("MineKot", path.readMineKotText())
    }

    @Test
    fun `byte helpers round trip`() {
        val path = Files.createTempDirectory("minekot-io").resolve("sample.bin")

        path.writeMineKotBytes(byteArrayOf(1, 2, 3))

        assertArrayEquals(byteArrayOf(1, 2, 3), path.readMineKotBytes())
    }

    @Test
    fun `delete result captures deletion`() {
        val path = Files.createTempDirectory("minekot-io").resolve("sample.txt")
        path.writeMineKotText("MineKot")

        assertTrue(path.deleteMineKotIfExistsResult().getOrThrow())
        assertFalse(path.exists())
    }

    @Test
    fun `directory copy preserves existing files`() {
        val root = Files.createTempDirectory("minekot-io")
        val source = root.resolve("source")
        val target = root.resolve("target")
        source.resolve("a.txt").writeMineKotText("source")
        target.resolve("a.txt").writeMineKotText("target")
        source.resolve("b.txt").writeMineKotText("new")

        val copied = source.copyMineKotDirectoryMissingOnly(target)

        assertEquals(listOf(target.resolve("b.txt")), copied)
        assertEquals("target", target.resolve("a.txt").readMineKotText())
        assertEquals("new", target.resolve("b.txt").readMineKotText())
    }

    @Test
    fun `safe resolve rejects traversal`() {
        val root = Files.createTempDirectory("minekot-io")

        assertEquals(
            root.toAbsolutePath().normalize().resolve("nested/file.txt"),
            root.resolveMineKotSafe("nested/file.txt"),
        )
        assertTrue(runCatching { root.resolveMineKotSafe("../outside.txt") }.isFailure)
    }

    @Test
    fun `recursive delete removes nested tree`() {
        val root = Files.createTempDirectory("minekot-io")
        root.resolve("nested/file.txt").writeMineKotText("MineKot")

        assertTrue(root.isDirectory())
        assertTrue(root.deleteMineKotRecursivelyResult().getOrThrow() >= 2)
        assertFalse(root.exists())
    }

    @Test
    fun `path metadata and url classloader helpers work`() {
        val path = Files.createTempDirectory("minekot-io").resolve("sample.test.txt")
        path.writeMineKotText("MineKot")

        assertEquals("txt", path.mineKotExtension())
        assertEquals("sample.test", path.mineKotNameWithoutExtension())
        listOf(path).toMineKotUrlClassLoaderResult().getOrThrow().use { loader ->
            assertTrue(loader.urLs.isNotEmpty())
        }
    }
}
