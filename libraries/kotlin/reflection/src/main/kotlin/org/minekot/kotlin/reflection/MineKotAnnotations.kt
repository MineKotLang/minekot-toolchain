package org.minekot.kotlin.reflection

import org.minekot.kotlin.common.toResult
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.full.findAnnotation

/**
 * Returns an annotation of this element as a [Result].
 *
 * @return Successful annotation or failure when missing.
 */
inline fun <reified AnnotationType : Annotation> KAnnotatedElement.mineKotAnnotationResult(): Result<AnnotationType> =
    findAnnotation<AnnotationType>().toResult("Missing annotation ${AnnotationType::class.simpleName}.")

/**
 * Returns true when this element has the requested annotation.
 *
 * @return True when annotation exists.
 */
inline fun <reified AnnotationType : Annotation> KAnnotatedElement.hasMineKotAnnotation(): Boolean =
    findAnnotation<AnnotationType>() != null

/**
 * Returns all matching annotations.
 *
 * @return Matching annotations.
 */
inline fun <reified AnnotationType : Annotation> KAnnotatedElement.mineKotAnnotations(): List<AnnotationType> =
    annotations.filterIsInstance<AnnotationType>()
