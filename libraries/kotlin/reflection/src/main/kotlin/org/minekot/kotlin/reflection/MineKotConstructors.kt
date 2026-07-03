package org.minekot.kotlin.reflection

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * Returns a primary constructor parameter name set.
 *
 * @return Constructor parameter names.
 */
fun KClass<*>.primaryConstructorParameterNames(): Set<String> =
    primaryConstructor
        ?.parameters
        ?.mapNotNull { parameter -> parameter.name }
        ?.toSet()
        .orEmpty()

/**
 * Calls the primary constructor with arguments matched by parameter name.
 *
 * @param arguments Constructor arguments by name.
 * @return Created instance.
 */
fun <Value : Any> KClass<Value>.callPrimaryConstructor(arguments: Map<String, Any?>): Value {
    val constructor = requireNotNull(primaryConstructor) {
        "Class ${qualifiedName ?: simpleName} has no primary constructor."
    }
    val parameters = constructor.parameters.associateWith { parameter ->
        arguments[parameter.name]
    }.filterKeys { parameter ->
        parameter.name in arguments
    }
    return constructor.callBy(parameters)
}
