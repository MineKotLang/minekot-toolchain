package org.minekot.kotlin.common

/**
 * Returns this string after validating that it contains non-whitespace content.
 *
 * @param name Human-readable value name used in the failure message.
 * @return This string when it is not blank.
 */
fun String.requireNonBlank(name: String = "value"): String {
    require(isNotBlank()) {
        "${name} must not be blank."
    }
    return this
}

/**
 * Returns this string trimmed after validating that it contains non-whitespace content.
 *
 * @param name Human-readable value name used in the failure message.
 * @return Trimmed string when it is not blank.
 */
fun String.requireTrimmedNonBlank(name: String = "value"): String =
    trim().requireNonBlank(name)

/**
 * Returns this string after validating it as a MineKot identifier-like name.
 *
 * @param name Human-readable value name used in the failure message.
 * @return This string when it is valid.
 */
fun String.requireMineKotName(name: String = "value"): String {
    require(mineKotNamePattern.matches(this)) {
        "${name} must use lowercase letters, digits, dots, underscores, or hyphens."
    }
    return this
}

/**
 * Returns this string after validating it as a MineKot namespace.
 *
 * @param name Human-readable value name used in the failure message.
 * @return This string when it is valid.
 */
fun String.requireMineKotNamespace(name: String = "namespace"): String {
    require(mineKotNamespacePattern.matches(this)) {
        "${name} must use lowercase letters, digits, underscores, or hyphens."
    }
    return this
}

/**
 * Returns this string after validating it as a MineKot key path.
 *
 * @param name Human-readable value name used in the failure message.
 * @return This string when it is valid.
 */
fun String.requireMineKotPath(name: String = "path"): String {
    require(mineKotPathPattern.matches(this)) {
        "${name} must use lowercase letters, digits, dots, underscores, hyphens, or slashes."
    }
    return this
}

/**
 * Returns this string after validating it as a MineKot key.
 *
 * @param name Human-readable value name used in the failure message.
 * @return This string when it is valid.
 */
fun String.requireMineKotKey(name: String = "key"): String {
    val namespace = substringBefore(":", missingDelimiterValue = "")
    val path = substringAfter(":", missingDelimiterValue = "")
    namespace.requireMineKotNamespace("${name} namespace")
    path.requireMineKotPath("${name} path")
    return this
}

/**
 * Requires a nullable value and returns the non-null value.
 *
 * @param name Human-readable value name used in the failure message.
 * @return Non-null value.
 */
fun <Value : Any> Value?.requireMineKotNotNull(name: String = "value"): Value =
    requireNotNull(this) {
        "${name} must not be null."
    }

/**
 * Requires that this collection is not empty.
 *
 * @param name Human-readable value name.
 * @return This collection when non-empty.
 */
fun <Value, CollectionType : Collection<Value>> CollectionType.requireMineKotNonEmpty(
    name: String = "collection",
): CollectionType {
    require(isNotEmpty()) {
        "${name} must not be empty."
    }
    return this
}

private val mineKotNamePattern: Regex = Regex("[a-z0-9._-]+")
private val mineKotNamespacePattern: Regex = Regex("[a-z0-9_-]+")
private val mineKotPathPattern: Regex = Regex("[a-z0-9._/-]+")
