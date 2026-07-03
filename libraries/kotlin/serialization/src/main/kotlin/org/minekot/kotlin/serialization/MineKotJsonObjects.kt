package org.minekot.kotlin.serialization

import kotlinx.serialization.json.*

/**
 * Creates a JSON object from string values.
 *
 * @return JSON object.
 */
fun Map<String, String>.toMineKotJsonObject(): JsonObject =
    buildJsonObject {
        this@toMineKotJsonObject.toSortedMap().forEach { (name, value) ->
            put(name, value)
        }
    }

/**
 * Returns a JSON object field or null.
 *
 * @param name Field name.
 * @return Field value, or null.
 */
fun JsonObject.mineKotField(name: String): JsonElement? =
    this[name]

/**
 * Returns a string field.
 *
 * @param name Field name.
 * @return String field value, or null.
 */
fun JsonObject.mineKotString(name: String): String? =
    mineKotField(name)?.jsonPrimitive?.contentOrNull

/**
 * Returns an int field.
 *
 * @param name Field name.
 * @return Int field value, or null.
 */
fun JsonObject.mineKotInt(name: String): Int? =
    mineKotField(name)?.jsonPrimitive?.intOrNull

/**
 * Returns a boolean field.
 *
 * @param name Field name.
 * @return Boolean field value, or null.
 */
fun JsonObject.mineKotBoolean(name: String): Boolean? =
    mineKotField(name)?.jsonPrimitive?.booleanOrNull
