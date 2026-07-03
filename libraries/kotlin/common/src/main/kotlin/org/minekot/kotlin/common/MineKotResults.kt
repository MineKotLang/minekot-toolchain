package org.minekot.kotlin.common

/**
 * Standard MineKot failure with optional cause.
 *
 * @param message Failure message.
 * @param cause Failure cause.
 */
class MineKotFailure(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

/**
 * Converts a nullable value into a [Result].
 *
 * @param message Message used when this value is null.
 * @return Successful result containing this value, or a failed result.
 */
fun <Value : Any> Value?.toResult(message: String): Result<Value> =
    if (this == null) {
        Result.failure(IllegalStateException(message))
    } else {
        Result.success(this)
    }

/**
 * Returns this result value or throws an [IllegalStateException] with context.
 *
 * @param message Message used when this result failed.
 * @return Successful result value.
 */
fun <Value> Result<Value>.getOrThrowWithMessage(message: String): Value =
    getOrElse { cause ->
        throw IllegalStateException(message, cause)
    }

/**
 * Maps this result failure to an [IllegalStateException] with context.
 *
 * @param message Message used when this result failed.
 * @return Result with an identical success value or wrapped failure.
 */
fun <Value> Result<Value>.withMineKotMessage(message: String): Result<Value> =
    fold(
        onSuccess = { value -> Result.success(value) },
        onFailure = { cause -> Result.failure(IllegalStateException(message, cause)) },
    )

/**
 * Converts this result into a value using explicit handlers.
 *
 * @param success Success mapper.
 * @param failure Failure mapper.
 * @return Mapped value.
 */
fun <Value, Output> Result<Value>.mineKotFold(
    success: (Value) -> Output,
    failure: (Throwable) -> Output,
): Output =
    fold(success, failure)

/**
 * Maps a result failure.
 *
 * @param transform Failure transform.
 * @return Result with identical success or transformed failure.
 */
fun <Value> Result<Value>.mapMineKotFailure(transform: (Throwable) -> Throwable): Result<Value> =
    fold(
        onSuccess = { value -> Result.success(value) },
        onFailure = { cause -> Result.failure(transform(cause)) },
    )

/**
 * Converts this result failure into a [MineKotFailure].
 *
 * @param message Failure message.
 * @return Result with MineKot failure metadata.
 */
fun <Value> Result<Value>.asMineKotFailure(message: String): Result<Value> =
    mapMineKotFailure { cause ->
        MineKotFailure(message, cause)
    }

/**
 * Recovers a failed result to null.
 *
 * @return Success value, or null when failed.
 */
fun <Value> Result<Value>.recoverMineKotNull(): Value? =
    getOrNull()

/**
 * Collects successful result values or returns the first failure.
 *
 * @return Successful value list or first failure.
 */
fun <Value> Iterable<Result<Value>>.collectMineKotResults(): Result<List<Value>> {
    val values = mutableListOf<Value>()
    forEach { result ->
        result.fold(
            onSuccess = { value -> values.add(value) },
            onFailure = { cause -> return Result.failure(cause) },
        )
    }
    return Result.success(values)
}

/**
 * Partitions results into successful values and failures.
 *
 * @return Pair of successful values and failures.
 */
fun <Value> Iterable<Result<Value>>.partitionMineKotResults(): Pair<List<Value>, List<Throwable>> {
    val values = mutableListOf<Value>()
    val failures = mutableListOf<Throwable>()
    forEach { result ->
        result.fold(
            onSuccess = { value -> values.add(value) },
            onFailure = { cause -> failures.add(cause) },
        )
    }
    return values to failures
}

/**
 * Creates a failed result lazily.
 *
 * @param message Failure message.
 * @return Failed result.
 */
fun <Value> mineKotFailure(message: () -> String): Result<Value> =
    Result.failure(IllegalStateException(message()))
