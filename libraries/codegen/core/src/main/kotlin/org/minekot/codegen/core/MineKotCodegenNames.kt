package org.minekot.codegen.core

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName

/**
 * Creates a safe Kotlin identifier from arbitrary input.
 *
 * @param value Raw identifier value.
 * @return Safe Kotlin identifier.
 */
fun mineKotIdentifier(value: String): String {
    val words = value.split(Regex("[^A-Za-z0-9]+")).filter(String::isNotBlank)
    val joined = words.joinToString("") { word ->
        word.replaceFirstChar { character -> character.uppercaseChar() }
    }
    val identifier = joined.ifBlank { "Generated" }
    return if (identifier.first().isDigit()) {
        "Generated${identifier}"
    } else {
        identifier
    }
}

/**
 * Creates a safe Kotlin member identifier from arbitrary input.
 *
 * @param value Raw identifier value.
 * @return Safe member identifier.
 */
fun mineKotMemberIdentifier(value: String): String =
    mineKotIdentifier(value).replaceFirstChar { character -> character.lowercaseChar() }

/**
 * Creates a safe package name.
 *
 * @param value Raw package name.
 * @return Safe package name.
 */
fun mineKotPackageName(value: String): String =
    value.split('.').joinToString(".") { segment -> mineKotMemberIdentifier(segment).lowercase() }
        .ifBlank { "generated" }

/**
 * Creates a class name from the package and raw class name input.
 *
 * @param packageName Package name.
 * @param rawName Raw class name.
 * @return Safe class name.
 */
fun mineKotClassName(packageName: String, rawName: String): ClassName =
    ClassName(mineKotPackageName(packageName), mineKotIdentifier(rawName))

/**
 * Creates a member name from the package and raw member name input.
 *
 * @param packageName Package name.
 * @param rawName Raw member name.
 * @return Safe member name.
 */
fun mineKotMemberName(packageName: String, rawName: String): MemberName =
    MemberName(mineKotPackageName(packageName), mineKotMemberIdentifier(rawName))

/**
 * Returns this type with requested nullability.
 *
 * @param nullable Whether the result should be nullable.
 * @return Type with requested nullability.
 */
fun TypeName.mineKotNullable(nullable: Boolean = true): TypeName =
    copy(nullable = nullable)

/**
 * Returns this type as non-null.
 *
 * @return Non-null type.
 */
fun TypeName.mineKotNonNullable(): TypeName =
    copy(nullable = false)
