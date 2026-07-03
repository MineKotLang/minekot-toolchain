package org.minekot.kotlin.atomic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MineKotAtomicTest {
    @Test
    fun `atomic helper updates value`() {
        val value = mineKotAtomic(1)

        assertEquals(2, value.updateAndGetMineKot { current -> current + 1 })
    }

    @Test
    fun `atomic helper returns previous value`() {
        val value = mineKotAtomic(1)

        assertEquals(1, value.getAndUpdateMineKot { current -> current + 1 })
        assertEquals(2, value.value)
    }

    @Test
    fun `once gate opens once`() {
        val gate = mineKotOnce()

        assertEquals(true, gate.openMineKotOnce())
        assertEquals(false, gate.openMineKotOnce())
    }

    @Test
    fun `once gate can reset`() {
        val gate = mineKotOnce()

        gate.openMineKotOnce()
        gate.resetMineKotOnce()

        assertEquals(true, gate.openMineKotOnce())
    }

    @Test
    fun `compare update result reports mismatch`() {
        val value = mineKotAtomic(1)

        assertTrue(value.compareAndSetMineKotResult(2, 3).isFailure)
        assertEquals(1, value.value)
    }

    @Test
    fun `atomic state holder updates value`() {
        val state = MineKotAtomicState(1)

        assertEquals(2, state.update { value -> value + 1 })
        assertEquals(2, state.get())
        state.set(3)
        assertEquals(3, state.get())
    }

    @Test
    fun `close gate closes once and rejects later use`() {
        val gate = MineKotCloseGate()

        gate.requireOpen()
        assertTrue(gate.closeOnce())
        assertFalse(gate.closeOnce())
        assertTrue(gate.isClosed())
        assertTrue(runCatching { gate.requireOpen() }.isFailure)
    }

    @Test
    fun `once value stores first value only`() {
        val value = MineKotOnceValue<String>()

        assertTrue(value.setOnce("first"))
        assertFalse(value.setOnce("second"))
        assertEquals("first", value.get())
    }

    @Test
    fun `conditional update returns null when predicate fails`() {
        val value = mineKotAtomic(1)

        assertEquals(2, value.updateMineKotIf({ current -> current == 1 }) { current -> current + 1 })
        assertEquals(null, value.updateMineKotIf({ current -> current == 1 }) { current -> current + 1 })
    }
}
