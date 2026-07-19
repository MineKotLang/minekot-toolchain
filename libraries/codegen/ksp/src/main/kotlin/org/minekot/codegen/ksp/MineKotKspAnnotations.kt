package org.minekot.codegen.ksp

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.squareup.kotlinpoet.ClassName
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Typed reference to an annotation argument available without loading the annotation class.
 *
 * @property annotationType Annotation class name.
 * @property name Argument name.
 */
data class MineKotKspArgument<Value>(
    val annotationType: ClassName,
    val name: String,
)

/**
 * Creates a typed annotation argument reference.
 *
 * @param name Argument name.
 * @return Typed argument reference.
 */
fun <Value> ClassName.mineKotArgument(name: String): MineKotKspArgument<Value> =
    MineKotKspArgument(this, name)

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

/**
 * Reads an annotation argument identified by its declared property.
 *
 * @param property Annotation property.
 * @return Argument value, or null.
 */
fun <AnnotationType : Annotation> KSAnnotation.mineKotArgument(
    property: KProperty1<AnnotationType, *>,
): Any? = mineKotArgument(property.name)

/**
 * Reads a typed annotation argument identified by its declared property.
 *
 * @param property Annotation property.
 * @return Argument value, or null when absent or wrong type.
 */
inline fun <AnnotationType : Annotation, reified Value> KSAnnotation.mineKotTypedArgument(
    property: KProperty1<AnnotationType, Value>,
): Value? = mineKotTypedArgument(property.name)

/**
 * Finds an annotation by type.
 *
 * @param annotationType Annotation type.
 * @return Matching annotation, or null.
 */
fun KSAnnotated.mineKotAnnotation(annotationType: KClass<out Annotation>): KSAnnotation? =
    mineKotAnnotation(ClassName.bestGuess(annotationType.mineKotKspName()))

/**
 * Finds an annotation by class name.
 *
 * @param annotationType Annotation class name.
 * @return Matching annotation, or null.
 */
fun KSAnnotated.mineKotAnnotation(annotationType: ClassName): KSAnnotation? =
    annotations.firstOrNull { annotation ->
        annotation.isMineKotAnnotation(annotationType)
    }

/**
 * Returns whether this annotation has a class name.
 *
 * @param expectedType Annotation class name.
 * @return True when qualified names match.
 */
fun KSAnnotation.isMineKotAnnotation(expectedType: ClassName): Boolean =
    annotationType.resolve().declaration.qualifiedName?.asString() == expectedType.canonicalName

/**
 * Finds an annotation by type.
 *
 * @return Matching annotation, or null.
 */
inline fun <reified AnnotationType : Annotation> KSAnnotated.mineKotAnnotation(): KSAnnotation? =
    mineKotAnnotation(AnnotationType::class)

/**
 * Returns whether this symbol has an annotation type.
 *
 * @param annotationType Annotation type.
 * @return True when annotation is present.
 */
fun KSAnnotated.hasMineKotAnnotation(annotationType: KClass<out Annotation>): Boolean =
    mineKotAnnotation(annotationType) != null

/**
 * Returns whether this symbol has an annotation class name.
 *
 * @param annotationType Annotation class name.
 * @return True when annotation is present.
 */
fun KSAnnotated.hasMineKotAnnotation(annotationType: ClassName): Boolean =
    mineKotAnnotation(annotationType) != null

/**
 * Returns whether this symbol has an annotation type.
 *
 * @return True when annotation is present.
 */
inline fun <reified AnnotationType : Annotation> KSAnnotated.hasMineKotAnnotation(): Boolean =
    hasMineKotAnnotation(AnnotationType::class)

/**
 * Reads a typed annotation argument from a symbol.
 *
 * @param argument Typed argument reference.
 * @return Argument value, or null when annotation or argument is absent or wrong type.
 */
inline fun <reified Value> KSAnnotated.mineKotTypedArgument(argument: MineKotKspArgument<Value>): Value? =
    mineKotAnnotation(argument.annotationType)?.mineKotTypedArgument(argument.name)

/**
 * Reads a typed referenced argument from this annotation.
 *
 * @param argument Typed argument reference.
 * @return Argument value, or null when absent or wrong type.
 */
inline fun <reified Value> KSAnnotation.mineKotTypedArgument(argument: MineKotKspArgument<Value>): Value? =
    mineKotTypedArgument(argument.name)

/**
 * Reads a required typed annotation argument from a symbol.
 *
 * @param argument Typed argument reference.
 * @return Argument value.
 */
inline fun <reified Value> KSAnnotated.mineKotRequiredArgument(argument: MineKotKspArgument<Value>): Value =
    requireNotNull(mineKotTypedArgument(argument)) {
        "Missing ${argument.annotationType.simpleName}.${argument.name} annotation argument."
    }
