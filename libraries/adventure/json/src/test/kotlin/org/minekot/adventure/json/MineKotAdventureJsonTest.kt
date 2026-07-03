package org.minekot.adventure.json

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MineKotAdventureJsonTest {
    @Test
    fun `json helper round trips component`() {
        val encoded = Component.text("MineKot").toMineKotAdventureJson()
        val decoded = mineKotAdventureJson(encoded)

        assertEquals("MineKot", PlainTextComponentSerializer.plainText().serialize(decoded))
    }

    @Test
    fun `json result helper captures failure`() {
        assertTrue(mineKotAdventureJsonResult("{").isFailure)
    }

    @Test
    fun `json serialize result captures success`() {
        assertTrue(Component.text("MineKot").toMineKotAdventureJsonResult().getOrThrow().contains("MineKot"))
    }

    @Test
    fun `json list helpers round trip components`() {
        val encoded = listOf(Component.text("Mine"), Component.text("Kot")).toMineKotAdventureJsonList()
        val decoded = encoded.fromMineKotAdventureJsonList()

        assertEquals(
            listOf("Mine", "Kot"),
            decoded.map { component -> PlainTextComponentSerializer.plainText().serialize(component) },
        )
    }

    @Test
    fun `json list result helpers capture success and failure`() {
        val encoded = listOf(Component.text("MineKot")).toMineKotAdventureJsonListResult().getOrThrow()

        assertEquals(
            "MineKot",
            PlainTextComponentSerializer.plainText()
                .serialize(encoded.fromMineKotAdventureJsonListResult().getOrThrow().single()),
        )
        assertTrue(listOf("{").fromMineKotAdventureJsonListResult().isFailure)
    }
}
