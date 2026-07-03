package org.minekot.kotlin.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.minekot.kotlin.io.writeMineKotTextIfMissing
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * JSON object migration between config versions.
 *
 * @property fromVersion Source version.
 * @property toVersion Target version.
 * @property transform Object transform.
 */
data class MineKotJsonMigration(
    val fromVersion: Int,
    val toVersion: Int,
    val transform: (JsonObject) -> JsonObject,
)

/**
 * Reads config or writes and returns a default value when missing.
 *
 * @param serializer Serializer for the value.
 * @param defaultValue Default config value.
 * @param json JSON codec.
 * @return Existing or default config value.
 */
fun <Value> Path.readMineKotConfig(
    serializer: KSerializer<Value>,
    defaultValue: Value,
    json: Json = MineKotJson,
): Value {
    if (!exists()) {
        writeMineKotTextIfMissing(encodeMineKotJson(json, serializer, defaultValue))
        return defaultValue
    }
    return readMineKotJson(json, serializer)
}

/**
 * Reads config or writes a default value while capturing failures.
 *
 * @param serializer Serializer for the value.
 * @param defaultValue Default config value.
 * @param json JSON codec.
 * @return Existing or default config result.
 */
fun <Value> Path.readMineKotConfigResult(
    serializer: KSerializer<Value>,
    defaultValue: Value,
    json: Json = MineKotJson,
): Result<Value> =
    runCatching {
        readMineKotConfig(serializer, defaultValue, json)
    }

/**
 * Reads config strictly and falls back to lenient JSON when requested.
 *
 * @param serializer Serializer for the value.
 * @param defaultValue Default value written when missing.
 * @param strict Whether unknown keys fail reads.
 * @return Config value result.
 */
fun <Value> Path.readMineKotConfigResult(
    serializer: KSerializer<Value>,
    defaultValue: Value,
    strict: Boolean,
): Result<Value> =
    readMineKotConfigResult(
        serializer = serializer,
        defaultValue = defaultValue,
        json = if (strict) StrictMineKotJson else MineKotJson,
    )

/**
 * Runs sequential JSON config migrations.
 *
 * @param currentVersion Current object version.
 * @param targetVersion Desired version.
 * @param migrations Available migrations.
 * @return Migrated object result.
 */
fun JsonObject.migrateMineKotJsonConfig(
    currentVersion: Int,
    targetVersion: Int,
    migrations: Iterable<MineKotJsonMigration>,
): Result<JsonObject> =
    runCatching {
        var version = currentVersion
        var current = this
        val bySource = migrations.associateBy(MineKotJsonMigration::fromVersion)
        while (version < targetVersion) {
            val migration = bySource[version] ?: error("Missing migration from version ${version}.")
            current = migration.transform(current)
            version = migration.toVersion
        }
        current
    }
