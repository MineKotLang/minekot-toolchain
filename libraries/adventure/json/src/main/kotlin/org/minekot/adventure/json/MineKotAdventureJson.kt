package org.minekot.adventure.json

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

/**
 * Shared Gson serializer for MineKot Adventure components.
 */
val MineKotAdventureJson: GsonComponentSerializer = GsonComponentSerializer.gson()

/**
 * Serializes a component to Adventure JSON.
 *
 * @return JSON representation.
 */
fun Component.toMineKotAdventureJson(): String =
    MineKotAdventureJson.serialize(this)

/**
 * Serializes a component and captures failures.
 *
 * @return JSON result.
 */
fun Component.toMineKotAdventureJsonResult(): Result<String> =
    runCatching {
        toMineKotAdventureJson()
    }

/**
 * Deserializes Adventure JSON to a component.
 *
 * @param source JSON source.
 * @return Deserialized component.
 */
fun mineKotAdventureJson(source: String): Component =
    MineKotAdventureJson.deserialize(source)

/**
 * Deserializes Adventure JSON to a component and captures failures in [Result].
 *
 * @param source JSON source.
 * @return Successful component or captured failure.
 */
fun mineKotAdventureJsonResult(source: String): Result<Component> =
    runCatching {
        mineKotAdventureJson(source)
    }

/**
 * Serializes components to JSON strings.
 *
 * @return JSON string list.
 */
fun Iterable<Component>.toMineKotAdventureJsonList(): List<String> =
    map(Component::toMineKotAdventureJson)

/**
 * Serializes components to JSON strings and captures failures.
 *
 * @return JSON string list result.
 */
fun Iterable<Component>.toMineKotAdventureJsonListResult(): Result<List<String>> =
    runCatching {
        toMineKotAdventureJsonList()
    }

/**
 * Deserializes Adventure JSON strings to components.
 *
 * @return Component list.
 */
fun Iterable<String>.fromMineKotAdventureJsonList(): List<Component> =
    map(::mineKotAdventureJson)

/**
 * Deserializes Adventure JSON strings and captures failures.
 *
 * @return Component list result.
 */
fun Iterable<String>.fromMineKotAdventureJsonListResult(): Result<List<Component>> =
    runCatching {
        fromMineKotAdventureJsonList()
    }
