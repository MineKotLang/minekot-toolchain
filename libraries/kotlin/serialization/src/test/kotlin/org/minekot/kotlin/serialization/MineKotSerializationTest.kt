package org.minekot.kotlin.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.writeText

class MineKotSerializationTest {
    @Serializable
    data class Sample(val name: String)

    @Test
    fun `json round trip works`() {
        val encoded = encodeMineKotJson(Sample.serializer(), Sample("minekot"))
        val decoded = decodeMineKotJson(Sample.serializer(), encoded)

        assertEquals(Sample("minekot"), decoded)
    }

    @Test
    fun `reified json round trip works`() {
        val encoded = encodeMineKotJson(Sample("minekot"))
        val decoded = decodeMineKotJson<Sample>(encoded)

        assertEquals(Sample("minekot"), decoded)
    }

    @Test
    fun `json result captures decode failure`() {
        val result = decodeMineKotJsonResult(Sample.serializer(), "{")

        assertTrue(result.isFailure)
    }

    @Test
    fun `json path helpers round trip`() {
        val path = Files.createTempDirectory("minekot-json").resolve("sample.json")

        path.writeMineKotJson(Sample.serializer(), Sample("minekot"))

        assertEquals(Sample("minekot"), path.readMineKotJson(Sample.serializer()))
    }

    @Test
    fun `json encode result captures success`() {
        val result = encodeMineKotJsonResult(Sample.serializer(), Sample("minekot"))

        assertTrue(result.getOrThrow().contains("minekot"))
    }

    @Test
    fun `json read result captures failure`() {
        val path = Files.createTempDirectory("minekot-json").resolve("sample.json")
        path.writeText("{")

        assertTrue(path.readMineKotJsonResult(Sample.serializer()).isFailure)
    }

    @Test
    fun `config helper writes default when missing`() {
        val path = Files.createTempDirectory("minekot-json").resolve("sample.json")

        val config = path.readMineKotConfig(Sample.serializer(), Sample("default"))

        assertEquals(Sample("default"), config)
        assertEquals(Sample("default"), path.readMineKotJson(Sample.serializer()))
    }

    @Test
    fun `strict json rejects unknown keys`() {
        val result = runCatching {
            decodeMineKotJson(StrictMineKotJson, Sample.serializer(), """{"name":"minekot","extra":true}""")
        }

        assertTrue(result.isFailure)
    }

    @Test
    fun `json object helper sorts fields`() {
        val json = mapOf("b" to "2", "a" to "1").toMineKotJsonObject()

        assertEquals(JsonPrimitive("1"), json.mineKotField("a"))
        assertEquals(listOf("a", "b"), json.keys.toList())
    }

    @Test
    fun `json list and map result helpers preserve order`() {
        val first = encodeMineKotJson(Sample.serializer(), Sample("a"))
        val second = encodeMineKotJson(Sample.serializer(), Sample("b"))
        val decoded = decodeMineKotJsonListResult(Sample.serializer(), listOf(first, second)).getOrThrow()
        val encoded = encodeMineKotJsonMapResult(Sample.serializer(), mapOf("b" to Sample("b"), "a" to Sample("a")))

        assertEquals(listOf(Sample("a"), Sample("b")), decoded)
        assertEquals(listOf("a", "b"), encoded.getOrThrow().keys.toList())
    }

    @Test
    fun `json typed field readers and migration remove boilerplate`() {
        val source = buildJsonObject {
            put("version", 1)
            put("name", "old")
            put("enabled", true)
        }
        val migrated = source.migrateMineKotJsonConfig(
            currentVersion = source.mineKotInt("version") ?: 0,
            targetVersion = 2,
            migrations = listOf(
                MineKotJsonMigration(1, 2) { current ->
                    buildJsonObject {
                        put("version", 2)
                        put("name", "${current.mineKotString("name")}-new")
                        put("enabled", current.mineKotBoolean("enabled") == true)
                    }
                },
            ),
        ).getOrThrow()

        assertEquals(2, migrated.mineKotInt("version"))
        assertEquals("old-new", migrated.mineKotString("name"))
        assertEquals(true, migrated.mineKotBoolean("enabled"))
    }
}
