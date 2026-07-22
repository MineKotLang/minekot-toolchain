package org.minekot.adventure.ansi

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer

/**
 * Shared ANSI serializer for MineKot console output.
 */
val MineKotAnsi: ANSIComponentSerializer = ANSIComponentSerializer.ansi()

/**
 * Serializes a component to ANSI text.
 *
 * @return ANSI string.
 */
fun Component.toMineKotAnsi(): String = MineKotAnsi.serialize(this)

/**
 * Serializes a component to ANSI text and captures failures.
 *
 * @return ANSI result.
 */
fun Component.toMineKotAnsiResult(): Result<String> =
    runCatching {
        toMineKotAnsi()
    }

/**
 * Removes common ANSI escape sequences from this string.
 *
 * @return Plain string.
 */
fun String.stripMineKotAnsi(): String =
    replace(mineKotAnsiPattern, "")

private val mineKotAnsiPattern = Regex("\\u001B\\[[;\\d]*m")
