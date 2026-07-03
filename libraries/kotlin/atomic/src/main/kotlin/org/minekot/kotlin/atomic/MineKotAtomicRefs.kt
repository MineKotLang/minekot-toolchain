package org.minekot.kotlin.atomic

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic

/**
 * Creates an atomic reference.
 *
 * @param initial Initial value.
 * @return Atomic reference containing [initial].
 */
fun <Value> mineKotAtomic(initial: Value): AtomicRef<Value> =
    atomic(initial)

/**
 * Updates an atomic reference and returns the previous value.
 *
 * @param transform Function that creates the next value.
 * @return Value stored before the update.
 */
fun <Value> AtomicRef<Value>.getAndUpdateMineKot(transform: (Value) -> Value): Value {
    while (true) {
        val current = value
        if (compareAndSet(current, transform(current))) {
            return current
        }
    }
}

/**
 * Updates an atomic reference and returns the new value.
 *
 * @param transform Function that creates the next value.
 * @return New value stored in this reference.
 */
fun <Value> AtomicRef<Value>.updateAndGetMineKot(transform: (Value) -> Value): Value {
    while (true) {
        val current = value
        val updated = transform(current)
        if (compareAndSet(current, updated)) {
            return updated
        }
    }
}

/**
 * Updates this reference only when the expected value matches.
 *
 * @param expected Expected current value.
 * @param update New value.
 * @return Successful updated value or failure.
 */
fun <Value> AtomicRef<Value>.compareAndSetMineKotResult(expected: Value, update: Value): Result<Value> =
    if (compareAndSet(expected, update)) {
        Result.success(update)
    } else {
        Result.failure(IllegalStateException("Atomic value did not match expected value."))
    }

/**
 * Updates this reference when a predicate matches.
 *
 * @param predicate Predicate for current value.
 * @param transform Value transform.
 * @return Updated value, or null when predicate failed.
 */
fun <Value> AtomicRef<Value>.updateMineKotIf(
    predicate: (Value) -> Boolean,
    transform: (Value) -> Value,
): Value? {
    while (true) {
        val current = value
        if (!predicate(current)) {
            return null
        }
        val updated = transform(current)
        if (compareAndSet(current, updated)) {
            return updated
        }
    }
}
