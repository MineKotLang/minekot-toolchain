package org.minekot.kotlin.common

import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass

/**
 * Captures only declared recoverable exceptions in a [Result].
 *
 * Cancellation, fatal JVM failures, and undeclared exceptions are always rethrown.
 *
 * @param recoverableExceptions Exception types that represent recoverable outcomes.
 * @param block Operation to execute once.
 * @return Successful value or declared recoverable failure.
 */
fun <Value> mineKotRunCatching(
    vararg recoverableExceptions: KClass<out Throwable>,
    block: () -> Value,
): Result<Value> {
    require(recoverableExceptions.isNotEmpty()) {
        "At least one recoverable exception type is required."
    }
    return try {
        Result.success(block())
    } catch (failure: Throwable) {
        if (failure.isMineKotUnrecoverable() || recoverableExceptions.none { exception -> exception.isInstance(failure) }) {
            throw failure
        }
        Result.failure(failure)
    }
}

private fun Throwable.isMineKotUnrecoverable(): Boolean =
    this is CancellationException || this is Error
