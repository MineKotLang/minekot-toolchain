package org.minekot.adventure.minimessage

/**
 * Escapes MiniMessage tags in this text.
 *
 * @return Escaped text.
 */
fun String.escapeMineKotMiniMessage(): String =
    MineKotMiniMessage.escapeTags(this)

/**
 * Requires that this text does not contain legacy color markers.
 *
 * @return This string when valid.
 */
fun String.requireMineKotMiniMessageText(): String {
    require(!mineKotLegacyColorPattern.containsMatchIn(this)) {
        "MiniMessage text must not contain legacy color codes."
    }
    return this
}

/**
 * Requires that tags in this text are in the allowed set.
 *
 * @param allowedTags Allowed tag names.
 * @return This string when valid.
 */
fun String.requireMineKotMiniMessageTags(allowedTags: Set<String>): String {
    mineKotTagPattern.findAll(this).forEach { match ->
        val tag = match.groupValues[1].substringBefore(":")
        require(tag in allowedTags) {
            "MiniMessage tag ${tag} is not allowed."
        }
    }
    return this
}

private val mineKotLegacyColorPattern = Regex("(?i)(§[0-9a-fk-or]|&[0-9a-fk-or])")
private val mineKotTagPattern = Regex("</?([a-zA-Z][a-zA-Z0-9_-]*(?::[^>]*)?)>")
