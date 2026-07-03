package org.minekot.adventure.common

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration

/**
 * Creates an empty component.
 *
 * @return Empty component.
 */
fun mineKotEmptyComponent(): Component =
    Component.empty()

/**
 * Creates a text component.
 *
 * @param text Plain text content.
 * @return Text component.
 */
fun mineKotText(text: String): Component =
    Component.text(text)

/**
 * Creates a newline component.
 *
 * @return Newline component.
 */
fun mineKotNewline(): Component =
    Component.newline()

/**
 * Creates a space component.
 *
 * @return Space component.
 */
fun mineKotSpace(): Component =
    Component.space()

/**
 * Joins components with newline separators.
 *
 * @return Joined component.
 */
fun Iterable<Component>.joinMineKotLines(): Component =
    Component.join(JoinConfiguration.separator(Component.newline()), this)

/**
 * Joins components with space separators.
 *
 * @return Joined component.
 */
fun Iterable<Component>.joinMineKotWords(): Component =
    Component.join(JoinConfiguration.separator(Component.space()), this)

/**
 * Creates a component from text lines.
 *
 * @return Joined line component.
 */
fun Iterable<String>.toMineKotLineComponent(): Component =
    map(::mineKotText).joinMineKotLines()

/**
 * Creates a key-value row component.
 *
 * @param key Row key.
 * @param value Row value.
 * @return Row component.
 */
fun mineKotKeyValueComponent(key: String, value: Component): Component =
    mineKotText("${key}: ").append(value)

/**
 * Creates bullet list components from text.
 *
 * @return Bullet list component.
 */
fun Iterable<String>.toMineKotBulletComponent(): Component =
    map { line -> mineKotText("- ${line}") }.joinMineKotLines()

/**
 * Returns this component or an empty component when null.
 *
 * @return Component value.
 */
fun Component?.orMineKotEmpty(): Component =
    this ?: mineKotEmptyComponent()
