package org.minekot.adventure.ansi

import net.kyori.adventure.text.Component
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MineKotAnsiTest {
    @Test
    fun `ansi result helper serializes component`() {
        assertTrue(Component.text("MineKot").toMineKotAnsiResult().getOrThrow().contains("MineKot"))
    }

    @Test
    fun `ansi strip helper removes color escapes`() {
        assertEquals("MineKot", "\u001B[32mMineKot\u001B[0m".stripMineKotAnsi())
    }
}
