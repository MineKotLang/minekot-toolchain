package org.minekot.kotlin.coroutines

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Converts this flow into a flow of [Result] values.
 *
 * @return Result flow.
 */
fun <Value> Flow<Value>.asMineKotResultFlow(): Flow<Result<Value>> =
    map { value -> Result.success(value) }
        .catch { cause -> emit(Result.failure(cause)) }

/**
 * Collects this flow and captures failures.
 *
 * @param collector Value collector.
 * @return Collection result.
 */
suspend fun <Value> Flow<Value>.collectMineKotResult(collector: suspend (Value) -> Unit): Result<Unit> =
    runMineKotCatchingCancellable {
        collect { value ->
            collector(value)
        }
    }
