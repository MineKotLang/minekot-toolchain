package org.minekot.codegen.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import kotlin.reflect.KClass

/**
 * Returns valid symbols with an annotation.
 *
 * @param annotationName Fully qualified annotation name.
 * @return Valid annotated symbols.
 */
fun Resolver.mineKotAnnotatedSymbols(annotationName: String): Sequence<KSAnnotated> =
    getSymbolsWithAnnotation(annotationName).filter(KSAnnotated::validate)

/**
 * Returns valid symbols with an annotation.
 *
 * @param annotationType Annotation type.
 * @return Valid annotated symbols.
 */
fun Resolver.mineKotAnnotatedSymbols(annotationType: KClass<out Annotation>): Sequence<KSAnnotated> =
    mineKotAnnotatedSymbols(ClassName.bestGuess(annotationType.mineKotKspName()))

/**
 * Returns valid symbols with an annotation class name.
 *
 * @param annotationType Annotation class name.
 * @return Valid annotated symbols.
 */
fun Resolver.mineKotAnnotatedSymbols(annotationType: ClassName): Sequence<KSAnnotated> =
    mineKotAnnotatedSymbols(annotationType.canonicalName)

/**
 * Returns valid symbols with an annotation.
 *
 * @return Valid annotated symbols.
 */
inline fun <reified AnnotationType : Annotation> Resolver.mineKotAnnotatedSymbols(): Sequence<KSAnnotated> =
    mineKotAnnotatedSymbols(AnnotationType::class)

/**
 * Returns valid class declarations with an annotation.
 *
 * @param annotationName Fully qualified annotation name.
 * @return Valid annotated class declarations.
 */
fun Resolver.mineKotAnnotatedClasses(annotationName: String): Sequence<KSClassDeclaration> =
    getSymbolsWithAnnotation(annotationName)
        .filterIsInstance<KSClassDeclaration>()
        .filter(KSAnnotated::validate)

/**
 * Returns valid class declarations with an annotation.
 *
 * @param annotationType Annotation type.
 * @return Valid annotated class declarations.
 */
fun Resolver.mineKotAnnotatedClasses(
    annotationType: KClass<out Annotation>,
): Sequence<KSClassDeclaration> = mineKotAnnotatedClasses(ClassName.bestGuess(annotationType.mineKotKspName()))

/**
 * Returns valid class declarations with an annotation class name.
 *
 * @param annotationType Annotation class name.
 * @return Valid annotated class declarations.
 */
fun Resolver.mineKotAnnotatedClasses(
    annotationType: ClassName,
): Sequence<KSClassDeclaration> = mineKotAnnotatedClasses(annotationType.canonicalName)

/**
 * Returns valid class declarations with an annotation.
 *
 * @return Valid annotated class declarations.
 */
inline fun <reified AnnotationType : Annotation> Resolver.mineKotAnnotatedClasses(): Sequence<KSClassDeclaration> =
    mineKotAnnotatedClasses(AnnotationType::class)

/**
 * Returns valid function declarations with an annotation.
 *
 * @param annotationName Fully qualified annotation name.
 * @return Valid annotated function declarations.
 */
fun Resolver.mineKotAnnotatedFunctions(annotationName: String): Sequence<KSFunctionDeclaration> =
    getSymbolsWithAnnotation(annotationName)
        .filterIsInstance<KSFunctionDeclaration>()
        .filter(KSAnnotated::validate)

/**
 * Returns valid function declarations with an annotation.
 *
 * @param annotationType Annotation type.
 * @return Valid annotated function declarations.
 */
fun Resolver.mineKotAnnotatedFunctions(
    annotationType: KClass<out Annotation>,
): Sequence<KSFunctionDeclaration> = mineKotAnnotatedFunctions(ClassName.bestGuess(annotationType.mineKotKspName()))

/**
 * Returns valid function declarations with an annotation class name.
 *
 * @param annotationType Annotation class name.
 * @return Valid annotated function declarations.
 */
fun Resolver.mineKotAnnotatedFunctions(
    annotationType: ClassName,
): Sequence<KSFunctionDeclaration> = mineKotAnnotatedFunctions(annotationType.canonicalName)

/**
 * Returns valid function declarations with an annotation.
 *
 * @return Valid annotated function declarations.
 */
inline fun <reified AnnotationType : Annotation> Resolver.mineKotAnnotatedFunctions(): Sequence<KSFunctionDeclaration> =
    mineKotAnnotatedFunctions(AnnotationType::class)

/**
 * Returns valid property declarations with an annotation.
 *
 * @param annotationName Fully qualified annotation name.
 * @return Valid annotated property declarations.
 */
fun Resolver.mineKotAnnotatedProperties(annotationName: String): Sequence<KSPropertyDeclaration> =
    getSymbolsWithAnnotation(annotationName)
        .filterIsInstance<KSPropertyDeclaration>()
        .filter(KSAnnotated::validate)

/**
 * Returns valid property declarations with an annotation.
 *
 * @param annotationType Annotation type.
 * @return Valid annotated property declarations.
 */
fun Resolver.mineKotAnnotatedProperties(
    annotationType: KClass<out Annotation>,
): Sequence<KSPropertyDeclaration> = mineKotAnnotatedProperties(ClassName.bestGuess(annotationType.mineKotKspName()))

/**
 * Returns valid property declarations with an annotation class name.
 *
 * @param annotationType Annotation class name.
 * @return Valid annotated property declarations.
 */
fun Resolver.mineKotAnnotatedProperties(
    annotationType: ClassName,
): Sequence<KSPropertyDeclaration> = mineKotAnnotatedProperties(annotationType.canonicalName)

/**
 * Returns valid property declarations with an annotation.
 *
 * @return Valid annotated property declarations.
 */
inline fun <reified AnnotationType : Annotation> Resolver.mineKotAnnotatedProperties(): Sequence<KSPropertyDeclaration> =
    mineKotAnnotatedProperties(AnnotationType::class)

/**
 * Finds a class declaration by runtime type.
 *
 * @param type Runtime type.
 * @return Class declaration, or null.
 */
fun Resolver.mineKotClassDeclaration(type: KClass<*>): KSClassDeclaration? =
    mineKotClassDeclaration(ClassName.bestGuess(type.mineKotKspName()))

/**
 * Finds a class declaration by class name.
 *
 * @param type Class name.
 * @return Class declaration, or null.
 */
fun Resolver.mineKotClassDeclaration(type: ClassName): KSClassDeclaration? =
    getClassDeclarationByName(getKSNameFromString(type.canonicalName))

/**
 * Finds a class declaration by runtime type.
 *
 * @return Class declaration, or null.
 */
inline fun <reified Type : Any> Resolver.mineKotClassDeclaration(): KSClassDeclaration? =
    mineKotClassDeclaration(Type::class)

/**
 * Returns whether a runtime type is declared by current source compilation.
 *
 * @param type Runtime type.
 * @return True when type has a containing source file.
 */
fun Resolver.hasMineKotSourceClass(type: KClass<*>): Boolean =
    mineKotClassDeclaration(type)?.containingFile != null

/**
 * Returns whether a class name is declared by current source compilation.
 *
 * @param type Class name.
 * @return True when type has a containing source file.
 */
fun Resolver.hasMineKotSourceClass(type: ClassName): Boolean =
    mineKotClassDeclaration(type)?.containingFile != null

/**
 * Returns whether a runtime type is declared by current source compilation.
 *
 * @return True when type has a containing source file.
 */
inline fun <reified Type : Any> Resolver.hasMineKotSourceClass(): Boolean =
    hasMineKotSourceClass(Type::class)
