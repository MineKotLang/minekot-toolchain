package org.minekot.kotlin.atomic

import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic

/**
 * Creates an atomic once gate.
 *
 * @return Atomic boolean initialized to false.
 */
fun mineKotOnce(): AtomicBoolean =
    atomic(false)

/**
 * Opens this once gate.
 *
 * @return true only for the first caller.
 */
fun AtomicBoolean.openMineKotOnce(): Boolean =
    compareAndSet(expect = false, update = true)

/**
 * Resets this once gate.
 */
fun AtomicBoolean.resetMineKotOnce() {
    value = false
}

/**
 * Atomic close gate.
 */
class MineKotCloseGate {
    private val closed = atomic(false)

    /**
     * Closes this gate once.
     *
     * @return True for the caller that closed it.
     */
    fun closeOnce(): Boolean =
        closed.compareAndSet(expect = false, update = true)

    /**
     * Returns true when this gate is closed.
     *
     * @return Closed state.
     */
    fun isClosed(): Boolean =
        closed.value

    /**
     * Requires this gate to be open.
     */
    fun requireOpen() {
        check(!closed.value) {
            "MineKot gate is closed."
        }
    }
}
