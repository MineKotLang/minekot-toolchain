package org.minekot.kotlin.testing

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class MineKotTestingTest {
    @Test
    fun `success assertion returns value`() {
        assertEquals("minekot", Result.success("minekot").assertMineKotSuccess())
    }

    @Test
    fun `failure assertion returns cause`() {
        val cause = IllegalStateException("boom")

        assertEquals(cause, Result.failure<String>(cause).assertMineKotFailure())
    }

    @Test
    fun `temp project helper creates directory`() {
        assertTrue(Files.isDirectory(createMineKotTestProject()))
    }

    @Test
    fun `path assertions return path`() {
        val directory = createMineKotTestProject()

        assertEquals(directory, directory.assertMineKotExists().assertMineKotDirectory())
    }

    @Test
    fun `temp project use helper deletes directory`() {
        lateinit var directory: java.nio.file.Path

        useMineKotTestProject { project ->
            directory = project
            project.assertMineKotDirectory()
        }

        assertFalse(Files.exists(directory))
    }

    @Test
    fun `gradle fixture helper writes files`() {
        val directory = createMineKotGradleProject("""plugins { id("java") }""")

        assertTrue(Files.exists(directory.resolve("settings.gradle.kts")))
        assertTrue(Files.exists(directory.resolve("build.gradle.kts")))
    }
}
