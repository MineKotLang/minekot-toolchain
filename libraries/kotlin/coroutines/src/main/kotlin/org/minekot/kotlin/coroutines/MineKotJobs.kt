package org.minekot.kotlin.coroutines

import kotlinx.coroutines.*

/**
 * Managed coroutine scope that cancels on close.
 *
 * @property scope Backing coroutine scope.
 */
class MineKotManagedScope(val scope: CoroutineScope) : AutoCloseable {
    override fun close() {
        scope.cancel()
    }
}

/**
 * Creates a supervised managed scope.
 *
 * @param dispatcher Dispatcher used by the scope.
 * @return Managed scope.
 */
fun mineKotManagedScope(dispatcher: CoroutineDispatcher = Dispatchers.Default): MineKotManagedScope =
    MineKotManagedScope(CoroutineScope(SupervisorJob() + dispatcher))

/**
 * Launches coroutine work and routes failures to a handler.
 *
 * @param dispatcher Dispatcher used for execution.
 * @param onFailure Failure handler.
 * @param block Operation to execute.
 * @return Launched job.
 */
fun CoroutineScope.launchMineKotCatching(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    onFailure: (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
): Job =
    this@launchMineKotCatching.launch(dispatcher) {
        val scope = this
        runCatching {
            block(scope)
        }.onFailure(onFailure)
    }

/**
 * Launches supervised coroutine work and routes failures to a handler.
 *
 * @param dispatcher Dispatcher used for execution.
 * @param onFailure Failure handler.
 * @param block Operation to execute.
 * @return Launched job.
 */
fun CoroutineScope.launchMineKotSupervisor(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    onFailure: (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
): Job =
    this@launchMineKotSupervisor.launch(dispatcher) {
        supervisorScope {
            val scope = this
            runCatching {
                block(scope)
            }.onFailure(onFailure)
        }
    }

/**
 * Cancels and joins this job while capturing failures.
 *
 * @return Completion result.
 */
suspend fun Job.cancelAndJoinMineKotResult(): Result<Unit> =
    runCatching {
        this@cancelAndJoinMineKotResult.cancelAndJoin()
    }
