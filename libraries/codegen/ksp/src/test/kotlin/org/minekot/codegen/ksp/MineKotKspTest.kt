package org.minekot.codegen.ksp

import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ClassName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class MineKotKspTest {
    @Test
    fun `annotation argument helpers read typed values`() {
        val annotation = annotation("name" to "minekot")

        assertEquals("minekot", annotation.mineKotArgument("name"))
        assertEquals("minekot", annotation.mineKotTypedArgument<String>("name"))
        assertEquals("minekot", annotation.mineKotArgument(ExampleAnnotation::name))
        assertEquals("minekot", annotation.mineKotTypedArgument(ExampleAnnotation::name))
        assertEquals(ExampleAnnotation::class.qualifiedName, ExampleAnnotation::class.mineKotKspName())

        val argument = ClassName("org.minekot", "ExampleAnnotation").mineKotArgument<String>("name")
        assertEquals("org.minekot.ExampleAnnotation", argument.annotationType.canonicalName)
        assertEquals("name", argument.name)
    }

    @Test
    fun `class name references read annotations without runtime annotation dependencies`() {
        val annotationType = ClassName.bestGuess(requireNotNull(ExampleAnnotation::class.qualifiedName))
        val annotated = annotated(annotation("name" to "minekot"), annotationType)
        val argument = annotationType.mineKotArgument<String>("name")

        assertTrue(annotated.hasMineKotAnnotation(annotationType))
        assertEquals("minekot", annotated.mineKotTypedArgument(argument))
        assertEquals("minekot", annotated.mineKotRequiredArgument(argument))
    }

    private fun annotation(vararg arguments: Pair<String, Any?>): KSAnnotation =
        proxy { method ->
            when (method.name) {
                "getArguments" -> arguments.map { (name, value) -> valueArgument(name, value) }
                else -> defaultValue(method)
            }
        }

    private fun annotated(annotation: KSAnnotation, annotationType: ClassName): KSAnnotated =
        proxy { method ->
            when (method.name) {
                "getAnnotations" -> sequenceOf(
                    proxy<KSAnnotation> { annotationMethod ->
                        when (annotationMethod.name) {
                            "getAnnotationType" -> typeReference(annotationType)
                            "getArguments" -> annotation.arguments
                            else -> defaultValue(annotationMethod)
                        }
                    },
                )

                else -> defaultValue(method)
            }
        }

    private fun typeReference(type: ClassName): KSTypeReference =
        proxy { method ->
            when (method.name) {
                "resolve" -> type(type)
                else -> defaultValue(method)
            }
        }

    private fun type(type: ClassName): KSType =
        proxy { method ->
            when (method.name) {
                "getDeclaration" -> declaration(type)
                else -> defaultValue(method)
            }
        }

    private fun declaration(type: ClassName): KSDeclaration =
        proxy { method ->
            when (method.name) {
                "getQualifiedName" -> name(type.canonicalName)
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

    private annotation class ExampleAnnotation(val name: String)
}
