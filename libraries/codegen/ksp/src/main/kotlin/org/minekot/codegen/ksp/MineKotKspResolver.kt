package org.minekot.codegen.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate

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
 * Returns valid property declarations with an annotation.
 *
 * @param annotationName Fully qualified annotation name.
 * @return Valid annotated property declarations.
 */
fun Resolver.mineKotAnnotatedProperties(annotationName: String): Sequence<KSPropertyDeclaration> =
    getSymbolsWithAnnotation(annotationName)
        .filterIsInstance<KSPropertyDeclaration>()
        .filter(KSAnnotated::validate)
