package org.minekot.kotlin.atomic

import kotlinx.atomicfu.atomic

/**
 * Stores a value once.
 */
class MineKotOnceValue<Value> {
    private val reference = atomic<Value?>(null)

    /**
     * Stores the value when empty.
     *
     * @param value Value to store.
     * @return True when stored.
     */
    fun setOnce(value: Value): Boolean =
        reference.compareAndSet(expect = null, update = value)

    /**
     * Returns stored value.
     *
     * @return Stored value, or null.
     */
    fun get(): Value? =
        reference.value
}

/**
 * Simple atomic state holder.
 *
 * @param initial Initial state.
 */
class MineKotAtomicState<Value>(initial: Value) {
    private val reference = atomic(initial)

    /**
     * Returns current state.
     *
     * @return Current state.
     */
    fun get(): Value =
        reference.value

    /**
     * Stores new state.
     *
     * @param value New state.
     */
    fun set(value: Value) {
        reference.value = value
    }

    /**
     * Updates state and returns new state.
     *
     * @param transform State transform.
     * @return New state.
     */
    fun update(transform: (Value) -> Value): Value =
        reference.updateAndGetMineKot(transform)

    /**
     * Returns an immutable state snapshot.
     *
     * @return Current state.
     */
    fun snapshot(): Value =
        get()
}
