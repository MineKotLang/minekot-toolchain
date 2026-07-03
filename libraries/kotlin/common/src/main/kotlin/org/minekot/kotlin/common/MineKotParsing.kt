package org.minekot.kotlin.common

import java.util.*

/**
 * Parses this value as a UUID or returns null.
 *
 * @return Parsed UUID, or null.
 */
fun String.toMineKotUuidOrNull(): UUID? =
    runCatching {
        UUID.fromString(this)
    }.getOrNull()

/**
 * Parses this value as a UUID.
 *
 * @return Parsed UUID result.
 */
fun String.toMineKotUuidResult(): Result<UUID> =
    runCatching {
        UUID.fromString(this)
    }.withMineKotMessage("Invalid UUID ${this}.")

/**
 * Parses this value as an enum name.
 *
 * @return Matching enum value, or null.
 */
inline fun <reified Value : Enum<Value>> String.toMineKotEnumOrNull(): Value? =
    enumValues<Value>().firstOrNull { value ->
        value.name.equals(this, ignoreCase = true)
    }

/**
 * Parses this value as an enum name.
 *
 * @return Matching enum result.
 */
inline fun <reified Value : Enum<Value>> String.toMineKotEnumResult(): Result<Value> =
    toMineKotEnumOrNull<Value>().toResult("Unknown ${Value::class.simpleName} value ${this}.")
