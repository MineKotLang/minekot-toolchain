package org.minekot.kotlin.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.minekot.kotlin.io.readMineKotText
import org.minekot.kotlin.io.writeMineKotText
import java.nio.file.Path

/**
 * Shared JSON defaults for MineKot projects.
 */
val MineKotJson: Json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Strict JSON defaults for config validation.
 */
val StrictMineKotJson: Json = Json {
    prettyPrint = true
    ignoreUnknownKeys = false
    encodeDefaults = true
}

/**
 * Encodes a value with [MineKotJson].
 *
 * @param serializer Serializer for the value.
 * @param value Value to encode.
 * @return JSON string.
 */
fun <Value> encodeMineKotJson(serializer: KSerializer<Value>, value: Value): String =
    MineKotJson.encodeToString(serializer, value)

/**
 * Encodes a value with a selected [Json] instance.
 *
 * @param json JSON codec.
 * @param serializer Serializer for the value.
 * @param value Value to encode.
 * @return JSON string.
 */
fun <Value> encodeMineKotJson(json: Json, serializer: KSerializer<Value>, value: Value): String =
    json.encodeToString(serializer, value)

/**
 * Encodes a value with [MineKotJson] using a reified serializer.
 *
 * @param value Value to encode.
 * @return JSON string.
 */
inline fun <reified Value> encodeMineKotJson(value: Value): String =
    MineKotJson.encodeToString(value)

/**
 * Decodes a value with [MineKotJson].
 *
 * @param serializer Serializer for the value.
 * @param source JSON source text.
 * @return Decoded value.
 */
fun <Value> decodeMineKotJson(serializer: KSerializer<Value>, source: String): Value =
    MineKotJson.decodeFromString(serializer, source)

/**
 * Decodes a value with a selected [Json] instance.
 *
 * @param json JSON codec.
 * @param serializer Serializer for the value.
 * @param source JSON source text.
 * @return Decoded value.
 */
fun <Value> decodeMineKotJson(json: Json, serializer: KSerializer<Value>, source: String): Value =
    json.decodeFromString(serializer, source)

/**
 * Decodes a value with [MineKotJson] using a reified serializer.
 *
 * @param source JSON source text.
 * @return Decoded value.
 */
inline fun <reified Value> decodeMineKotJson(source: String): Value =
    MineKotJson.decodeFromString(source)

/**
 * Decodes a value with [MineKotJson] and captures failures in [Result].
 *
 * @param serializer Serializer for the value.
 * @param source JSON source text.
 * @return Successful decoded value or captured failure.
 */
fun <Value> decodeMineKotJsonResult(serializer: KSerializer<Value>, source: String): Result<Value> =
    runCatching {
        decodeMineKotJson(serializer, source)
    }

/**
 * Encodes a value and captures failures in [Result].
 *
 * @param serializer Serializer for the value.
 * @param value Value to encode.
 * @return Successful JSON or captured failure.
 */
fun <Value> encodeMineKotJsonResult(serializer: KSerializer<Value>, value: Value): Result<String> =
    runCatching {
        encodeMineKotJson(serializer, value)
    }

/**
 * Decodes a list of JSON sources.
 *
 * @param serializer Serializer for each value.
 * @param sources JSON source texts.
 * @return Decoded list result.
 */
fun <Value> decodeMineKotJsonListResult(
    serializer: KSerializer<Value>,
    sources: Iterable<String>,
): Result<List<Value>> =
    runCatching {
        sources.map { source -> decodeMineKotJson(serializer, source) }
    }

/**
 * Encodes named values into JSON.
 *
 * @param serializer Serializer for each value.
 * @param values Named values.
 * @return Named JSON result.
 */
fun <Value> encodeMineKotJsonMapResult(
    serializer: KSerializer<Value>,
    values: Map<String, Value>,
): Result<Map<String, String>> =
    runCatching {
        values.toSortedMap().mapValues { (_, value) ->
            encodeMineKotJson(serializer, value)
        }
    }

/**
 * Reads and decodes JSON from this path.
 *
 * @param serializer Serializer for the value.
 * @return Decoded value.
 */
fun <Value> Path.readMineKotJson(serializer: KSerializer<Value>): Value =
    decodeMineKotJson(serializer, readMineKotText())

/**
 * Reads and decodes JSON from this path with a selected [Json] instance.
 *
 * @param json JSON codec.
 * @param serializer Serializer for the value.
 * @return Decoded value.
 */
fun <Value> Path.readMineKotJson(json: Json, serializer: KSerializer<Value>): Value =
    decodeMineKotJson(json, serializer, readMineKotText())

/**
 * Reads and decodes JSON while capturing failures in [Result].
 *
 * @param serializer Serializer for the value.
 * @return Successful decoded value or captured failure.
 */
fun <Value> Path.readMineKotJsonResult(serializer: KSerializer<Value>): Result<Value> =
    runCatching {
        readMineKotJson(serializer)
    }

/**
 * Encodes and writes JSON to this path.
 *
 * @param serializer Serializer for the value.
 * @param value Value to write.
 */
fun <Value> Path.writeMineKotJson(serializer: KSerializer<Value>, value: Value) {
    writeMineKotText(encodeMineKotJson(serializer, value))
}

/**
 * Encodes and writes JSON with a selected [Json] instance.
 *
 * @param json JSON codec.
 * @param serializer Serializer for the value.
 * @param value Value to write.
 */
fun <Value> Path.writeMineKotJson(json: Json, serializer: KSerializer<Value>, value: Value) {
    writeMineKotText(encodeMineKotJson(json, serializer, value))
}

/**
 * Writes JSON while capturing failures in [Result].
 *
 * @param serializer Serializer for the value.
 * @param value Value to write.
 * @return Write result.
 */
fun <Value> Path.writeMineKotJsonResult(serializer: KSerializer<Value>, value: Value): Result<Unit> =
    runCatching {
        writeMineKotJson(serializer, value)
    }
