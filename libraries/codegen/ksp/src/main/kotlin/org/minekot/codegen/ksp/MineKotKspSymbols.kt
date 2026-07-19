package org.minekot.codegen.ksp

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import kotlin.reflect.KClass

/**
 * Returns this runtime type's qualified name for KSP lookups.
 *
 * @return Qualified type name.
 */
fun KClass<*>.mineKotKspName(): String =
    requireNotNull(qualifiedName) { "KSP lookups require a named type." }

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

/**
 * Returns whether this KSP type represents a runtime type.
 *
 * @param type Runtime type.
 * @return True when qualified names match.
 */
fun KSType.isMineKotType(type: KClass<*>): Boolean =
    isMineKotType(ClassName.bestGuess(type.mineKotKspName()))

/**
 * Returns whether this KSP type represents a class name.
 *
 * @param type Class name.
 * @return True when qualified names match.
 */
fun KSType.isMineKotType(type: ClassName): Boolean =
    declaration.qualifiedName?.asString() == type.canonicalName

/**
 * Returns whether this KSP type represents a runtime type.
 *
 * @return True when qualified names match.
 */
inline fun <reified Type : Any> KSType.isMineKotType(): Boolean =
    isMineKotType(Type::class)
