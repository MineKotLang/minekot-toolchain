package org.minekot.codegen.ksp

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSValueArgument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class MineKotKspTest {
    @Test
    fun `annotation argument helpers read typed values`() {
        val annotation = annotation("name" to "minekot")

        assertEquals("minekot", annotation.mineKotArgument("name"))
        assertEquals("minekot", annotation.mineKotTypedArgument<String>("name"))
    }

    private fun annotation(vararg arguments: Pair<String, Any?>): KSAnnotation =
        proxy { method ->
            when (method.name) {
                "getArguments" -> arguments.map { (name, value) -> valueArgument(name, value) }
                else -> defaultValue(method)
            }
        }

    private fun valueArgument(name: String, value: Any?): KSValueArgument =
        proxy { method ->
            when (method.name) {
                "getName" -> name(name)
                "getValue" -> value
                else -> defaultValue(method)
            }
        }

    private fun name(value: String): KSName =
        proxy { method ->
            when (method.name) {
                "asString", "getShortName" -> value
                else -> defaultValue(method)
            }
        }

    private inline fun <reified Type> proxy(noinline handler: (Method) -> Any?): Type =
        Proxy.newProxyInstance(
            Type::class.java.classLoader,
            arrayOf(Type::class.java),
        ) { _, method, _ ->
            handler(method)
        } as Type

    private fun defaultValue(method: Method): Any? =
        when (method.name) {
            "toString" -> "MineKotKspTestProxy"
            "hashCode" -> 0
            "equals" -> false
            else -> null
        }
}
