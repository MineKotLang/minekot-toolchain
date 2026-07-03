package org.minekot.kotlin.reflection

import java.util.*
import kotlin.reflect.KClass

/**
 * Loads services for this class.
 *
 * @param classLoader Class loader used by [ServiceLoader].
 * @return Loaded services result.
 */
fun <Value : Any> KClass<Value>.loadMineKotServicesResult(classLoader: ClassLoader): Result<List<Value>> =
    runCatching {
        ServiceLoader.load(java, classLoader).toList()
    }

/**
 * Finds a sealed subclass by simple name.
 *
 * @param simpleName Subclass simple name.
 * @return Matching subclass, or null.
 */
fun KClass<*>.sealedSubclassNamed(simpleName: String): KClass<*>? =
    sealedSubclasses.firstOrNull { subclass ->
        subclass.simpleName == simpleName
    }
