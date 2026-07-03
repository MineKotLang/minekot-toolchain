package org.minekot.kotlin.coroutines

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class MineKotCoroutinesTest {
    @Test
    fun `io helper returns block value`() = runTest {
        assertEquals("minekot", mineKotIo { "minekot" })
    }

    @Test
    fun `catching helper captures failure`() = runTest {
        val result = runMineKotCatching {
            error("boom")
        }

        assertTrue(result.isFailure)
    }

    @Test
    fun `timeout helper returns value`() = runTest {
        assertEquals("minekot", mineKotTimeoutOrNull(1_000) { "minekot" })
    }

    @Test
    fun `launch catching helper routes failure`() = runTest {
        var captured = false
        val job = launchMineKotCatching(onFailure = { captured = true }) {
            error("boom")
        }

        job.join()

        assertTrue(captured)
    }

    @Test
    fun `timeout result captures timeout`() = runTest {
        val result = mineKotTimeoutResult(1) {
            kotlinx.coroutines.delay(10.milliseconds)
            "minekot"
        }

        assertTrue(result.isFailure)
    }

    @Test
    fun `supervisor helper keeps sibling alive`() = runTest {
        var siblingCompleted = false
        val failing = launchMineKotSupervisor(onFailure = {}) {
            error("boom")
        }
        val sibling = launch {
            siblingCompleted = true
        }

        failing.join()
        sibling.join()

        assertTrue(siblingCompleted)
    }

    @Test
    fun `flow result helper captures emitted values and failure`() = runTest {
        val values = flow {
            emit("minekot")
            error("boom")
        }.asMineKotResultFlow().toList()

        assertEquals("minekot", values.first().getOrThrow())
        assertTrue(values.last().isFailure)
    }

    @Test
    fun `managed scope closes backing job`() = runTest {
        val managed =
            mineKotManagedScope(coroutineContext[kotlin.coroutines.ContinuationInterceptor] as kotlinx.coroutines.CoroutineDispatcher)

        managed.close()

        assertFalse(managed.scope.coroutineContext[kotlinx.coroutines.Job]!!.isActive)
    }

    @Test
    fun `retry helper retries until success`() = runTest {
        var attempts = 0
        val result = retryMineKot(attempts = 3) {
            attempts += 1
            if (attempts < 2) error("again")
            "minekot"
        }

        assertEquals("minekot", result.getOrThrow())
        assertEquals(2, attempts)
    }

    @Test
    fun `parallel map preserves input order`() = runTest {
        val result = listOf(1, 2, 3).mapMineKotParallel(parallelism = 2) { value ->
            value * 2
        }

        assertEquals(listOf(2, 4, 6), result)
    }
}
