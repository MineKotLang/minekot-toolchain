package org.minekot.toolchain.lint.core

import com.intellij.psi.PsiErrorElement
import dev.detekt.api.modifiedText
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Accumulates and applies text edits to a [KtFile] for auto-correction, ensuring edits do not overlap.
 *
 * This class manages a list of [MineKotTextEdit] objects. Edits are accumulated via [replace] and
 * then applied to a [KtFile] via [applyTo]. It includes logic to prevent adding duplicate or
 * overlapping edits and ensures that the final application of edits maintains the integrity
 * of the file's text by requiring non-overlapping edits.
 */
internal class MineKotTextEdits {
    private val edits: MutableList<MineKotTextEdit> = mutableListOf()

    fun clear() {
        edits.clear()
    }

    /**
     * Records a text replacement operation, preventing duplicates and overlapping edits.
     *
     * If an identical edit already exists, or if the new edit overlaps with any existing
     * recorded edit, it will not be added. Overlap is defined as any intersection of
     * the [startOffset] and [endOffset] ranges.
     *
     * @param startOffset The starting character offset of the text to be replaced (inclusive).
     * @param endOffset The ending character offset of the text to be replaced (exclusive).
     * @param replacement The new string to insert at the specified range.
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
     * Applies all accumulated text edits to the given [KtFile] if conditions are met.
     *
     * Edits are applied only if auto-correction is [enabled], there are edits to apply,
     * the file has not already been modified, and either [allowPartialSyntax] is true
     * or the file contains no [PsiErrorElement]s.
     *
     * Edits are sorted by descending [MineKotTextEdit.startOffset] to ensure that
     * replacements do not invalidate subsequent offsets. A runtime check ensures
     * that no two edits overlap, which would indicate an internal logic error.
     *
     * @param file The [KtFile] to which the edits should be applied.
     * @param enabled If `false`, no edits will be applied.
     * @param allowPartialSyntax If `true`, edits can be applied even if the file contains syntax errors.
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

/**
 * Checks if this [KtElement] is located within a `// @formatter:off` and `// @formatter:on` block.
 *
 * This is determined by comparing the last occurrence of `@formatter:off` and `@formatter:on`
 * in the text preceding the element's [com.intellij.psi.util.startOffset]. If `@formatter:off` appears
 * more recently than `@formatter:on`, the element is considered to be inside a disabled block.
 *
 * @receiver The [KtElement] to check.
 * @return `true` if the element is inside a formatter control block, `false` otherwise.
 */
internal fun KtElement.isInsideMineKotFormatterControl(): Boolean {
    val prefix = containingKtFile.text.take(textRange.startOffset)
    return prefix.lastIndexOf("@formatter:off") > prefix.lastIndexOf("@formatter:on")
}
