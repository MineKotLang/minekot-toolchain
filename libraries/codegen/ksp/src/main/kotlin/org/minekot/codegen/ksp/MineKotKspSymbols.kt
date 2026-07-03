package org.minekot.codegen.ksp

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.validate

/**
 * Returns symbols that should be deferred to a later KSP round.
 *
 * @return Invalid symbols for later processing.
 */
fun Iterable<KSAnnotated>.mineKotDeferredSymbols(): List<KSAnnotated> =
    filterNot(KSAnnotated::validate)

/**
 * Validates this symbol and returns it as a [Result].
 *
 * @return Successful symbol or validation failure.
 */
fun <Symbol : KSAnnotated> Symbol.mineKotValidationResult(): Result<Symbol> =
    if (validate()) {
        Result.success(this)
    } else {
        Result.failure(IllegalStateException("KSP symbol failed validation."))
    }

/**
 * Returns true when this type is nullable.
 *
 * @return True when nullable.
 */
fun KSType.isMineKotNullable(): Boolean =
    nullability == Nullability.NULLABLE

/**
 * Returns true when this type is not nullable.
 *
 * @return True when not nullable.
 */
fun KSType.isMineKotNonNullable(): Boolean =
    nullability == Nullability.NOT_NULL
