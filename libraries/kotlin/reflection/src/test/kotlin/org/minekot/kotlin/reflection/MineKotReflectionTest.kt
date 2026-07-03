package org.minekot.kotlin.reflection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MineKotReflectionTest {
    @Target(AnnotationTarget.CLASS)
    annotation class Marker

    @Marker
    data class Sample(val name: String, val count: Int)

    sealed interface ExampleSealed

    class Child : ExampleSealed

    @Test
    fun `property helper finds property`() {
        assertTrue(Sample::class.hasMemberProperty("name"))
    }

    @Test
    fun `constructor helper returns parameter names`() {
        assertEquals(setOf("name", "count"), Sample::class.primaryConstructorParameterNames())
    }

    @Test
    fun `constructor helper calls by name`() {
        val sample = Sample::class.callPrimaryConstructor(mapOf("name" to "minekot", "count" to 1))

        assertEquals(Sample("minekot", 1), sample)
    }

    @Test
    fun `property value helper reads by name`() {
        val sample = Sample("minekot", 1)

        assertEquals("minekot", Sample::class.memberPropertyValue(sample, "name"))
    }

    @Test
    fun `annotation helpers detect annotation`() {
        assertTrue(Sample::class.hasMineKotAnnotation<Marker>())
        assertEquals(Marker::class, Sample::class.mineKotAnnotationResult<Marker>().getOrThrow().annotationClass)
    }

    @Test
    fun `property map extracts deterministic values`() {
        val sample = Sample("minekot", 1)

        assertEquals(mapOf("count" to 1, "name" to "minekot"), Sample::class.memberPropertyMap(sample))
    }

    @Test
    fun `annotation list and sealed subclass helpers remove lookup boilerplate`() {
        assertEquals(1, Sample::class.mineKotAnnotations<Marker>().size)
        assertEquals(Child::class, ExampleSealed::class.sealedSubclassNamed("Child"))
    }
}
