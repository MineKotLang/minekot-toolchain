package org.minekot.kotlin.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Runs work on a caller-selected dispatcher and captures failures in [Result].
 *
 * @param dispatcher Dispatcher used for execution.
 * @param block Operation to execute.
 * @return Successful result or captured failure.
 */
suspend fun <Value> runMineKotCatching(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    block: suspend () -> Value,
): Result<Value> =
    runCatching {
        onMineKotDispatcher(dispatcher, block)
    }

/**
 * Captures failures unless the coroutine was cancelled.
 *
 * @param block Operation to execute.
 * @return Successful result or captured failure.
 */
suspend fun <Value> runMineKotCatchingCancellable(block: suspend () -> Value): Result<Value> =
    runCatching {
        block()
    }.onFailure { cause ->
        if (cause is CancellationException) {
            throw cause
        }
    }

/**
 * Runs work with a timeout and returns null when the timeout expires.
 *
 * @param milliseconds Timeout in milliseconds.
 * @param block Operation to execute.
 * @return Operation result or null.
 */
suspend fun <Value> mineKotTimeoutOrNull(milliseconds: Long, block: suspend () -> Value): Value? =
    withTimeoutOrNull(milliseconds.milliseconds) {
        block()
    }

/**
 * Runs work with a timeout and captures failures in [Result].
 *
 * @param milliseconds Timeout in milliseconds.
 * @param block Operation to execute.
 * @return Successful value or captured failure.
 */
suspend fun <Value> mineKotTimeoutResult(milliseconds: Long, block: suspend () -> Value): Result<Value> =
    runCatching {
        withTimeout(milliseconds.milliseconds) {
            block()
        }
    }

/**
 * Retries an operation with a fixed delay between failures.
 *
 * @param attempts Maximum attempts.
 * @param wait Delay between failed attempts.
 * @param block Operation to execute.
 * @return Successful result or last failure.
 */
suspend fun <Value> retryMineKot(
    attempts: Int,
    wait: Duration = Duration.ZERO,
    block: suspend () -> Value,
): Result<Value> {
    require(attempts > 0) {
        "attempts must be positive."
    }
    var failure: Throwable? = null
    repeat(attempts) { index ->
        runMineKotCatchingCancellable(block).fold(
            onSuccess = { value -> return Result.success(value) },
            onFailure = { cause ->
                failure = cause
                if (index < attempts - 1 && wait > Duration.ZERO) {
                    delay(wait)
                }
            },
        )
    }
    return Result.failure(requireNotNull(failure))
}

/**
 * Maps values concurrently with a maximum parallelism bound.
 *
 * @param parallelism Maximum concurrent operations.
 * @param transform Value transform.
 * @return Mapped values in input order.
 */
suspend fun <Input, Output> Iterable<Input>.mapMineKotParallel(
    parallelism: Int,
    transform: suspend (Input) -> Output,
): List<Output> = coroutineScope {
    require(parallelism > 0) {
        "parallelism must be positive."
    }
    val semaphore = Semaphore(parallelism)
    map { input ->
        async {
            semaphore.withPermit {
                transform(input)
            }
        }
    }.awaitAll()
}
