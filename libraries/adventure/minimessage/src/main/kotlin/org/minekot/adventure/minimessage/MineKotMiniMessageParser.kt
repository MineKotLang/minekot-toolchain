package org.minekot.adventure.minimessage

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * Shared MiniMessage parser for MineKot user-facing text.
 */
val MineKotMiniMessage: MiniMessage = MiniMessage.miniMessage()

/**
 * Parses MiniMessage text to a component.
 *
 * @param source MiniMessage source.
 * @return Parsed component.
 */
fun mineKotMiniMessage(source: String): Component =
    MineKotMiniMessage.deserialize(source)

/**
 * Parses MiniMessage text and captures failures.
 *
 * @param source MiniMessage source.
 * @return Parsed component result.
 */
fun mineKotMiniMessageResult(source: String): Result<Component> =
    runCatching {
        mineKotMiniMessage(source)
    }

/**
 * Parses this MiniMessage text to a component.
 *
 * @return Parsed component.
 */
fun String.toMineKotMiniMessageComponent(): Component =
    mineKotMiniMessage(this)

/**
 * Serializes a component to MiniMessage.
 *
 * @return MiniMessage representation.
 */
fun Component.toMineKotMiniMessage(): String =
    MineKotMiniMessage.serialize(this)
