package org.minekot.adventure.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MineKotAdventureTest {
    @Test
    fun `component helper creates plain text`() {
        assertEquals("MineKot", mineKotText("MineKot").toMineKotPlainText())
    }

    @Test
    fun `line join helper joins with newlines`() {
        val component = listOf(mineKotText("Mine"), mineKotText("Kot")).joinMineKotLines()

        assertEquals("Mine\nKot", component.toMineKotPlainText())
    }

    @Test
    fun `word join helper joins with spaces`() {
        val component = listOf(mineKotText("Mine"), mineKotText("Kot")).joinMineKotWords()

        assertEquals("Mine Kot", component.toMineKotPlainText())
    }

    @Test
    fun `line component helper builds from strings`() {
        assertEquals("Mine\nKot", listOf("Mine", "Kot").toMineKotLineComponent().toMineKotPlainText())
    }

    @Test
    fun `plain text result helper returns success`() {
        assertEquals("MineKot", mineKotText("MineKot").toMineKotPlainTextResult().getOrThrow())
    }

    @Test
    fun `empty and space helpers create components`() {
        assertEquals("", mineKotEmptyComponent().toMineKotPlainText())
        assertTrue(mineKotSpace().toMineKotPlainText().isBlank())
    }

    @Test
    fun `row bullet and optional helpers build plain text`() {
        assertEquals("State: Ready", mineKotKeyValueComponent("State", mineKotText("Ready")).toMineKotPlainText())
        assertEquals("- One\n- Two", listOf("One", "Two").toMineKotBulletComponent().toMineKotPlainText())
        assertEquals("", null.orMineKotEmpty().toMineKotPlainText())
    }
}
