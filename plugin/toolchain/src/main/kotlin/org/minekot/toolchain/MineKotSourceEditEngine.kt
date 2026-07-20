package org.minekot.toolchain

/** One fingerprintable source replacement. */
internal data class MineKotSourceEdit(
    val startOffset: Int,
    val endOffset: Int,
    val expectedText: String,
    val replacement: String,
)

/** Applies validated, non-overlapping edits from highest offset to lowest. */
internal object MineKotSourceEditEngine {
    /** Returns corrected text or rejects stale, overlapping, formatter-controlled, or partial input. */
    fun apply(
        source: String,
        edits: List<MineKotSourceEdit>,
    ): String {
        if (edits.isEmpty()) {
            return source
        }
        require(!source.hasUnbalancedDelimiters()) { "Assisted edits require complete Kotlin delimiter syntax." }
        val ordered = edits.distinct().sortedByDescending(MineKotSourceEdit::startOffset)
        require(
            ordered.zipWithNext().all { (later, earlier) -> earlier.endOffset <= later.startOffset },
        ) { "MineKot source edits overlap." }
        ordered.forEach { edit ->
            require(edit.startOffset in 0..source.length && edit.endOffset in edit.startOffset..source.length) {
                "MineKot source edit range is outside source bounds."
            }
            require(source.substring(edit.startOffset, edit.endOffset) == edit.expectedText) {
                "MineKot source edit expected text is stale."
            }
            require(!source.isFormatterExcluded(edit.startOffset)) {
                "MineKot source edit intersects a formatter-controlled region."
            }
        }
        return ordered.fold(source) { corrected, edit ->
            corrected.replaceRange(edit.startOffset, edit.endOffset, edit.replacement)
        }
    }

    private fun String.isFormatterExcluded(offset: Int): Boolean {
        val prefix = take(offset)
        return prefix.lastIndexOf("@formatter:off") > prefix.lastIndexOf("@formatter:on")
    }

    private fun String.hasUnbalancedDelimiters(): Boolean {
        val stack = ArrayDeque<Char>()
        var quote: Char? = null
        var escaped = false
        forEach { character ->
            if (quote != null) {
                if (escaped) {
                    escaped = false
                } else if (character == '\\') {
                    escaped = true
                } else if (character == quote) {
                    quote = null
                }
                return@forEach
            }
            if (character == '"' || character == '\'') {
                quote = character
                return@forEach
            }
            when (character) {
                '(', '[', '{' -> stack.addLast(character)
                ')' -> if (stack.removeLastOrNull() != '(') return true
                ']' -> if (stack.removeLastOrNull() != '[') return true
                '}' -> if (stack.removeLastOrNull() != '{') return true
            }
        }
        return quote != null || stack.isNotEmpty()
    }
}
