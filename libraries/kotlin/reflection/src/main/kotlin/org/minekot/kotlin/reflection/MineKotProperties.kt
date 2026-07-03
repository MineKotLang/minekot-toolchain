package org.minekot.kotlin.reflection

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * Returns a member property by name.
 *
 * @param name Property name to find.
 * @return Matching property, or null when absent.
 */
fun KClass<*>.memberPropertyNamed(name: String): KProperty1<out Any, *>? =
    memberProperties.firstOrNull { property ->
        property.name == name
    }

/**
 * Returns true when this class has a member property with the given name.
 *
 * @param name Property name to find.
 * @return True when a matching property exists.
 */
fun KClass<*>.hasMemberProperty(name: String): Boolean =
    memberPropertyNamed(name) != null

/**
 * Reads a member property value.
 *
 * @param instance Instance to read from.
 * @param name Property name.
 * @return Property value, or null when absent.
 */
@Suppress("UNCHECKED_CAST")
fun KClass<*>.memberPropertyValue(instance: Any, name: String): Any? =
    (memberPropertyNamed(name) as? KProperty1<Any, *>)?.get(instance)

/**
 * Extracts public member properties into a map.
 *
 * @param instance Instance to inspect.
 * @return Property values by name.
 */
fun KClass<*>.memberPropertyMap(instance: Any): Map<String, Any?> =
    memberProperties
        .sortedBy { property -> property.name }
        .associate { property ->
            property.name to memberPropertyValue(instance, property.name)
        }
