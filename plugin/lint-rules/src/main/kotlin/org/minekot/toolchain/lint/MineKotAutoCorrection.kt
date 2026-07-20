package org.minekot.toolchain.lint

import com.intellij.psi.PsiErrorElement
import dev.detekt.api.modifiedText
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Collects deterministic source edits for an auto-correctable MineKot rule.
 */
internal class MineKotTextEdits {
    private val edits: MutableList<MineKotTextEdit> = mutableListOf()

    /**
     * Removes edits left over from a previous file visit.
     */
    fun clear() {
        edits.clear()
    }

    /**
     * Replaces a source range when auto-correction is enabled.
     *
     * @param startOffset Inclusive source offset.
     * @param endOffset Exclusive source offset.
     * @param replacement Replacement source text.
     */
    fun replace(
        startOffset: Int,
        endOffset: Int,
        replacement: String,
    ) {
        val edit = MineKotTextEdit(startOffset, endOffset, replacement)
        if (edit in edits) {
            return
        }
        val overlapsExistingEdit = edits.any { existing ->
            startOffset < existing.endOffset && existing.startOffset < endOffset
        }
        if (!overlapsExistingEdit) {
            edits += edit
        }
    }

    /**
     * Applies all non-overlapping edits from the end of the file to the beginning.
     *
     * @param file Visited Kotlin file.
     * @param enabled Whether the owning rule has auto-correction enabled.
     * @param allowPartialSyntax Whether this byte-level edit is safe when PSI contains syntax errors.
     */
    fun applyTo(
        file: KtFile,
        enabled: Boolean,
        allowPartialSyntax: Boolean = false,
    ) {
        if (
            !enabled || edits.isEmpty() || file.modifiedText != null ||
            !allowPartialSyntax && file.collectDescendantsOfType<PsiErrorElement>().isNotEmpty()
        ) {
            return
        }
        val orderedEdits = edits.sortedByDescending { edit -> edit.startOffset }
        require(
            orderedEdits.zipWithNext().all { (later, earlier) ->
                earlier.endOffset <= later.startOffset
            },
        ) {
            "MineKot auto-correction edits must not overlap: ${orderedEdits}"
        }
        file.modifiedText = orderedEdits.fold(file.text) { source, edit ->
            source.replaceRange(edit.startOffset, edit.endOffset, edit.replacement)
        }
    }
}

private data class MineKotTextEdit(
    val startOffset: Int,
    val endOffset: Int,
    val replacement: String,
)

/** Returns whether this element is inside an active IntelliJ formatter-off region. */
internal fun KtElement.isInsideMineKotFormatterControl(): Boolean {
    val prefix = containingKtFile.text.take(textRange.startOffset)
    return prefix.lastIndexOf("@formatter:off") > prefix.lastIndexOf("@formatter:on")
}
