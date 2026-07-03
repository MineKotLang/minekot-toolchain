package org.minekot.kotlin.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlin.coroutines.ContinuationInterceptor

/**
 * Runs blocking IO work on [Dispatchers.IO].
 *
 * @param block Blocking operation.
 * @return Operation result.
 */
suspend fun <Value> mineKotIo(block: () -> Value): Value =
    withContext(Dispatchers.IO) {
        block()
    }

/**
 * Runs work on a caller-selected dispatcher.
 *
 * @param dispatcher Dispatcher used for execution.
 * @param block Operation to execute.
 * @return Operation result.
 */
suspend fun <Value> onMineKotDispatcher(
    dispatcher: CoroutineDispatcher,
    block: suspend () -> Value,
): Value =
    withContext(dispatcher) {
        block()
    }

/**
 * Requires that execution is on an expected dispatcher.
 *
 * @param expected Expected dispatcher.
 */
suspend fun requireMineKotDispatcher(expected: CoroutineDispatcher) {
    require(currentCoroutineContext()[ContinuationInterceptor] == expected) {
        "MineKot coroutine is running on the wrong dispatcher."
    }
}
