package org.minekot.adventure.minimessage

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * MiniMessage placeholder value.
 *
 * @property name Placeholder name.
 * @property component Placeholder component.
 */
data class MineKotMiniMessagePlaceholder(
    val name: String,
    val component: Component,
)

/**
 * Creates a component placeholder.
 *
 * @param name Placeholder name.
 * @param component Placeholder component.
 * @return Placeholder model.
 */
fun mineKotPlaceholder(name: String, component: Component): MineKotMiniMessagePlaceholder =
    MineKotMiniMessagePlaceholder(name, component)

/**
 * Parses MiniMessage text to a component with named unparsed placeholders.
 *
 * @param source MiniMessage source.
 * @param placeholders Placeholder values by name.
 * @return Parsed component.
 */
fun mineKotMiniMessage(source: String, placeholders: Map<String, String>): Component =
    MineKotMiniMessage.deserialize(
        source,
        TagResolver.resolver(
            placeholders.toSortedMap().map { (name, value) ->
                Placeholder.unparsed(name, value)
            },
        ),
    )

/**
 * Parses MiniMessage text with component placeholders.
 *
 * @param source MiniMessage source.
 * @param placeholders Component placeholders.
 * @return Parsed component.
 */
fun mineKotMiniMessage(
    source: String,
    placeholders: Iterable<MineKotMiniMessagePlaceholder>,
): Component =
    MineKotMiniMessage.deserialize(
        source,
        TagResolver.resolver(
            placeholders.sortedBy(MineKotMiniMessagePlaceholder::name).map { placeholder ->
                Placeholder.component(placeholder.name, placeholder.component)
            },
        ),
    )
