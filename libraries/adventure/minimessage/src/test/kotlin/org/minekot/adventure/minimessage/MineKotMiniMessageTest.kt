package org.minekot.adventure.minimessage

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MineKotMiniMessageTest {
    @Test
    fun `minimessage helper parses text`() {
        val component = mineKotMiniMessage("<green>MineKot</green>")

        assertEquals("MineKot", PlainTextComponentSerializer.plainText().serialize(component))
    }

    @Test
    fun `minimessage helper parses placeholders`() {
        val component = mineKotMiniMessage("Hello <name>", mapOf("name" to "MineKot"))

        assertEquals("Hello MineKot", PlainTextComponentSerializer.plainText().serialize(component))
    }

    @Test
    fun `minimessage extension parses text`() {
        val component = "<green>MineKot</green>".toMineKotMiniMessageComponent()

        assertEquals("MineKot", PlainTextComponentSerializer.plainText().serialize(component))
    }

    @Test
    fun `minimessage escape helper escapes tags`() {
        assertEquals("\\<green>MineKot\\</green>", "<green>MineKot</green>".escapeMineKotMiniMessage())
    }

    @Test
    fun `minimessage component placeholders parse`() {
        val component = mineKotMiniMessage(
            "Hello <name>",
            listOf(MineKotMiniMessagePlaceholder("name", Component.text("MineKot"))),
        )

        assertEquals("Hello MineKot", PlainTextComponentSerializer.plainText().serialize(component))
    }

    @Test
    fun `minimessage result helper captures success`() {
        assertEquals(
            "MineKot",
            PlainTextComponentSerializer.plainText().serialize(mineKotMiniMessageResult("MineKot").getOrThrow()),
        )
    }

    @Test
    fun `legacy color validation rejects legacy text`() {
        val result = runCatching {
            "&aMineKot".requireMineKotMiniMessageText()
        }

        assertTrue(result.isFailure)
    }

    @Test
    fun `placeholder builder and tag allow list validate templates`() {
        val component = mineKotMiniMessage(
            "Hello <name>",
            listOf(mineKotPlaceholder("name", Component.text("MineKot"))),
        )

        assertEquals("Hello MineKot", PlainTextComponentSerializer.plainText().serialize(component))
        assertEquals("<green>MineKot</green>", "<green>MineKot</green>".requireMineKotMiniMessageTags(setOf("green")))
        assertTrue(runCatching { "<red>MineKot</red>".requireMineKotMiniMessageTags(setOf("green")) }.isFailure)
    }
}
