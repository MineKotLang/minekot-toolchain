package org.minekot.toolchain.lint.formatting

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.internal.AutoCorrectable
import org.jetbrains.kotlin.psi.KtFile
import org.minekot.toolchain.lint.core.*

/**
 * Enforces MineKot-specific comment indentation and marker spacing rules.
 *
 * This rule ensures that comments adhere to a consistent style within the MineKot codebase.
 * It primarily focuses on two aspects:
 *
 * 1.  **Indentation**: Standalone comments (those on their own line) are expected to inherit
 *     their indentation from the nearest declaration or expression sibling. This helps
 *     maintain a clean and aligned code structure. Inline comments, however, retain their
 *     existing placement as altering their indentation could inadvertently change the layout
 *     of the surrounding code.
 * 2.  **Marker Spacing**: Ensures that there is a single space immediately following the
 *     comment marker (e.g., `//` or `/*`) and before the closing block comment marker (`*/`).
 *     This improves readability and consistency.
 *
 * The rule is [AutoCorrectable], meaning it can automatically fix detected formatting issues.
 *
 * @constructor Creates a new instance of [CommentFormattingRule].
 * @param config The Detekt configuration for this rule.
 * @see [MineKotTextEdits]
 */
@AutoCorrectable(since = "2.0.0")
class CommentFormattingRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "CommentFormatting",
        severity = Severity.Style,
        description = "Comments must be indented properly and have a space after the marker.",
        debt = Debt.FIVE_MINS,
    )

    /**
     * The unique identifier for this Detekt rule.
     *
     * This property provides the [RuleName] derived from the [issue]'s ID.
     *
     * @return The [RuleName] for this rule.
     */
    override val ruleName: RuleName get() = RuleName(issue.id)

    /**
     * Manages and applies text edits for autocorrection.
     *
     * This utility collects all necessary modifications during the visit phase and applies them
     * in the [postVisit] phase if autocorrection is enabled.
     */
    private val edits = MineKotTextEdits()

    /**
     * Clears all accumulated text edits before visiting a new [KtFile].
     *
     * This ensures that each file processing starts with a clean slate, preventing edits
     * from previous files from affecting the current one.
     *
     * @param root The [KtFile] that is about to be visited.
     */
    override fun preVisit(root: KtFile) {
        edits.clear()
    }

    /**
     * Applies all accumulated text edits to the [KtFile] after it has been visited.
     *
     * If autocorrection is enabled, this method will modify the file based on the
     * formatting issues detected and recorded by the rule.
     *
     * @param root The [KtFile] that has just been visited.
     */
    override fun postVisit(root: KtFile) {
        edits.applyTo(root, autoCorrect)
    }

    /**
     * Processes each [PsiComment] found in the file to enforce formatting rules.
     *
     * This method checks for several comment formatting issues:
     * - Skips KDoc comments (`/**`) and formatter control comments (`@formatter:`).
     * - Calls [alignStandaloneComment] to correct indentation for standalone comments.
     * - Checks for and adds a missing space after the comment marker (e.g., `//` or `/ *`).
     * - Checks for and adds a missing space before the closing block comment marker (`*/`).
     *
     * Detected issues are reported as [CodeSmell]s, and corresponding text edits are
     * recorded for potential autocorrection.
     *
     * @param comment The [PsiComment] element being visited.
     */
    override fun visitComment(comment: PsiComment) {
        super.visitComment(comment)
        val text = comment.text
        if (text.startsWith("/**") || text.contains("@formatter:")) return

        val startOffset = comment.textRange.startOffset
        alignStandaloneComment(comment, startOffset)

        val needsLeadingSpace = (text.startsWith("//") && !text.startsWith("// ")) ||
                (text.startsWith("/*") && !text.startsWith("/* "))

        if (needsLeadingSpace) {
            reportFinding(comment, "Add a space after the comment marker.")
            edits.replace(startOffset + 2, startOffset + 2, " ")
        }

        if (text.startsWith("/*") && text.endsWith("*/") && !text.endsWith(" */")) {
            reportFinding(comment, "Add a space before the closing block comment marker.")
            edits.replace(comment.textRange.endOffset - 2, comment.textRange.endOffset - 2, " ")
        }
    }

    /**
     * Reports a [CodeSmell] finding for a given comment.
     *
     * This helper function encapsulates the creation and reporting of a [CodeSmell]
     * using the rule's predefined [issue] and the provided descriptive message.
     *
     * @param comment The [PsiComment] where the issue was found.
     * @param message The descriptive message for the reported issue.
     */
    private fun reportFinding(comment: PsiComment, message: String) {
        report(CodeSmell(issue, Entity.from(comment), message))
    }

    /**
     * Aligns the indentation of a standalone comment to match its surrounding code.
     *
     * This function determines the correct indentation by finding a suitable reference
     * [PsiElement] (e.g., a declaration or expression) and applies its indentation.
     * It only acts on comments that are truly "standalone" on their line, meaning
     * the characters preceding them on the same line are all whitespace.
     *
     * @param comment The [PsiComment] to align.
     * @param startOffset The starting offset of the comment in the file's text.
     */
    private fun alignStandaloneComment(comment: PsiComment, startOffset: Int) {
        val source = comment.containingFile.text
        val lineStart = source.lastIndexOf('\n', startOffset - 1) + 1
        val indentation = source.substring(lineStart, startOffset)
        if (indentation.any { character -> !character.isWhitespace() }) return
        val reference = comment.indentationReference() ?: return
        val referenceStart = reference.textRange.startOffset
        val referenceLineStart = source.lastIndexOf('\n', referenceStart - 1) + 1
        val expected = source.substring(referenceLineStart, referenceStart).takeWhile(Char::isWhitespace)
        if (indentation == expected) return

        reportFinding(comment, "Indent this comment to match the surrounding code.")
        edits.replace(lineStart, startOffset, expected)
    }

    /**
     * Finds a suitable [PsiElement] to use as an indentation reference for this comment.
     *
     * This extension function searches for the nearest non-whitespace, non-comment,
     * non-structural token sibling. It first checks previous siblings, then next siblings.
     * This reference element's indentation will be used to align the comment.
     *
     * @receiver The [PsiComment] for which to find an indentation reference.
     * @return The [PsiElement] to use as a reference, or `null` if no suitable reference is found.
     * @see [isCodeIndentReference]
     */
    private fun PsiComment.indentationReference(): PsiElement? =
        generateSequence(prevSibling, PsiElement::getPrevSibling)
            .firstOrNull { sibling -> sibling.isCodeIndentReference() }
            ?: generateSequence(nextSibling, PsiElement::getNextSibling)
                .firstOrNull { sibling -> sibling.isCodeIndentReference() }

    /**
     * Checks if a [PsiElement] can serve as a valid indentation reference.
     *
     * A valid indentation reference is an element that is not whitespace, not a comment,
     * and not one of the predefined structural tokens (like braces, parentheses, etc.).
     * These structural tokens often have their own specific indentation rules or don't
     * represent the main code block's indentation.
     *
     * @receiver The [PsiElement] to check.
     * @return `true` if the element is a valid indentation reference, `false` otherwise.
     */
    private fun PsiElement.isCodeIndentReference(): Boolean =
        this !is PsiWhiteSpace && this !is PsiComment && text !in indentationStructuralTokens

    private companion object {
        /**
         * A set of [String]s representing structural tokens that should not be used as indentation references.
         *
         * These tokens (e.g., `{`, `}`, `(`, `)`) often have their own specific indentation rules
         * or are part of a larger structure, and thus do not accurately reflect the indentation
         * of the main code body.
         */
        private val indentationStructuralTokens: Set<String> = setOf("{", "}", "(", ")", "[", "]", ",")
    }
}
