package org.minekot.codegen.ksp

import com.google.devtools.ksp.symbol.KSAnnotation

/**
 * Reads a named annotation argument.
 *
 * @param name Argument name.
 * @return Argument value, or null.
 */
fun KSAnnotation.mineKotArgument(name: String): Any? =
    arguments.firstOrNull { argument -> argument.name?.asString() == name }?.value

/**
 * Reads a typed named annotation argument.
 *
 * @param name Argument name.
 * @return Argument value, or null when absent or wrong type.
 */
inline fun <reified Value> KSAnnotation.mineKotTypedArgument(name: String): Value? =
    mineKotArgument(name) as? Value
