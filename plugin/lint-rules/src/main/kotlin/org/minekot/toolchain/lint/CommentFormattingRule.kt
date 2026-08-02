package org.minekot.toolchain.lint

import com.intellij.psi.PsiComment
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.internal.AutoCorrectable
import org.jetbrains.kotlin.psi.KtFile

/**
 * Enforces MineKot comment indentation and marker spacing.
 */
@AutoCorrectable(since = "2.0.0")
class CommentFormattingRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "CommentFormatting",
        severity = Severity.Style,
        description = "Comments must be indented properly and have a space after the marker.",
        debt = Debt.FIVE_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)
    private val edits = MineKotTextEdits()

    override fun preVisit(root: KtFile) {
        edits.clear()
    }

    override fun postVisit(root: KtFile) {
        edits.applyTo(root, autoCorrect)
    }

    override fun visitComment(comment: PsiComment) {
        super.visitComment(comment)
        val text = comment.text
        if (text.startsWith("/**") || text.contains("@formatter:")) return

        val startOffset = comment.textRange.startOffset

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

    private fun reportFinding(comment: PsiComment, message: String) {
        report(CodeSmell(issue, Entity.from(comment), message))
    }
}
