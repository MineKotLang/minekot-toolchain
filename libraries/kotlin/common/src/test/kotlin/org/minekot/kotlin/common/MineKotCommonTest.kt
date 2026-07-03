package org.minekot.kotlin.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MineKotCommonTest {
    enum class Mode {
        ENABLED,
        DISABLED,
    }

    @Test
    fun `blank string validation fails`() {
        val result = runCatching {
            "".requireNonBlank("name")
        }

        assertTrue(result.isFailure)
    }

    @Test
    fun `nullable conversion creates successful result`() {
        assertEquals("minekot", "minekot".toResult("missing").getOrThrow())
    }

    @Test
    fun `trimmed blank validation returns trimmed text`() {
        assertEquals("minekot", " minekot ".requireTrimmedNonBlank("name"))
    }

    @Test
    fun `not null helper returns value`() {
        val value = "minekot"

        assertEquals("minekot", value.requireMineKotNotNull("name"))
    }

    @Test
    fun `result message helper wraps failure`() {
        val result = Result.failure<String>(IllegalArgumentException("bad")).withMineKotMessage("context")

        assertEquals("context", result.exceptionOrNull()?.message)
    }

    @Test
    fun `minekot id validates value`() {
        assertEquals("minekot.core", MineKotId("minekot.core").value)
    }

    @Test
    fun `lazy failure creates failed result`() {
        val result = mineKotFailure<String> { "missing" }

        assertEquals("missing", result.exceptionOrNull()?.message)
    }

    @Test
    fun `minekot key parses namespace and path`() {
        val key = MineKotKey.of("minekot", "items/example")

        assertEquals("minekot:items/example", key.value)
        assertEquals("minekot", key.namespace)
        assertEquals("items/example", key.path)
    }

    @Test
    fun `minekot key parse helpers reject invalid input`() {
        assertEquals(null, MineKotKey.parseOrNull("Bad Key"))
        assertTrue(MineKotKey.parseResult("Bad Key").isFailure)
    }

    @Test
    fun `result collection returns values`() {
        val result = listOf(Result.success("mine"), Result.success("kot")).collectMineKotResults()

        assertEquals(listOf("mine", "kot"), result.getOrThrow())
    }

    @Test
    fun `result collection returns first failure`() {
        val failure = IllegalStateException("broken")
        val result = listOf(Result.success("mine"), Result.failure(failure)).collectMineKotResults()

        assertEquals(failure, result.exceptionOrNull())
    }

    @Test
    fun `failure mapping wraps cause`() {
        val result = Result.failure<String>(IllegalArgumentException("bad")).mapMineKotFailure { cause ->
            IllegalStateException("wrapped", cause)
        }

        assertEquals("wrapped", result.exceptionOrNull()?.message)
    }

    @Test
    fun `failure recovers to null`() {
        val result = Result.failure<String>(IllegalStateException("bad"))

        assertEquals(null, result.recoverMineKotNull())
    }

    @Test
    fun `enum and uuid parsing avoid throwing boilerplate`() {
        val uuid = "123e4567-e89b-12d3-a456-426614174000".toMineKotUuidResult().getOrThrow()

        assertEquals("123e4567-e89b-12d3-a456-426614174000", uuid.toString())
        assertEquals(Mode.ENABLED, "enabled".toMineKotEnumResult<Mode>().getOrThrow())
        assertEquals(null, "missing".toMineKotEnumOrNull<Mode>())
    }

    @Test
    fun `result partition and minekot failure preserve failures`() {
        val result = Result.failure<String>(IllegalArgumentException("bad")).asMineKotFailure("wrapped")
        val (values, failures) = listOf(Result.success("ok"), result).partitionMineKotResults()

        assertEquals(listOf("ok"), values)
        assertEquals("wrapped", failures.single().message)
        assertTrue(failures.single() is MineKotFailure)
    }

    @Test
    fun `non empty collection validation rejects empty values`() {
        assertEquals(listOf("minekot"), listOf("minekot").requireMineKotNonEmpty("items"))
        assertFalse(runCatching { emptyList<String>().requireMineKotNonEmpty("items") }.isSuccess)
    }
}
