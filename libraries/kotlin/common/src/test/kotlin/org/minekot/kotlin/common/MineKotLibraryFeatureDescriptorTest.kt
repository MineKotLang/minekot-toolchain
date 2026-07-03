package org.minekot.kotlin.common

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.walk

class MineKotLibraryFeatureDescriptorTest {
    @Test
    fun `feature descriptors map to configured modules and tests`() {
        val root = mineKotRoot()
        val settings = root.resolve("settings.gradle.kts").readText()
        val sourceText = root.resolve("libraries").walk()
            .filter { path -> path.toString().endsWith(".kt") }
            .joinToString("\n") { path -> path.readText() }
        val readme = root.resolve("README.md").readText()

        mineKotLibraryFeatureDescriptors.forEach { descriptor ->
            assertTrue(settings.contains(descriptor.module.substringAfterLast(":"))) {
                "Missing module for ${descriptor.module}."
            }
            assertTrue(descriptor.publicSymbols.isNotEmpty()) {
                "Missing public symbols for ${descriptor.feature}."
            }
            assertTrue(descriptor.testClass.endsWith("Test")) {
                "Missing test class for ${descriptor.feature}."
            }
            descriptor.publicSymbols.forEach { symbol ->
                assertTrue(sourceText.contains(symbol)) {
                    "Missing public symbol ${symbol} for ${descriptor.feature}."
                }
            }
            assertTrue(readme.contains(descriptor.feature)) {
                "README must mention ${descriptor.feature}."
            }
        }
    }

    private fun mineKotRoot(): Path {
        var current = Path.of("").toAbsolutePath()
        while (!current.resolve("settings.gradle.kts").exists()) {
            current = current.parent ?: error("Cannot find repository root.")
        }
        return current
    }
}
