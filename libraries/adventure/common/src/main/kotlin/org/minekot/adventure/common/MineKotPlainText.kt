package org.minekot.adventure.common

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

/**
 * Shared plain text serializer for MineKot Adventure utilities.
 */
val MineKotPlainText: PlainTextComponentSerializer = PlainTextComponentSerializer.plainText()

/**
 * Serializes a component to plain text.
 *
 * @return Plain text representation.
 */
fun Component.toMineKotPlainText(): String =
    MineKotPlainText.serialize(this)

/**
 * Serializes a component to plain text and captures failures.
 *
 * @return Plain text result.
 */
fun Component.toMineKotPlainTextResult(): Result<String> =
    runCatching {
        toMineKotPlainText()
    }
