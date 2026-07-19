package org.minekot.toolchain.lint

import com.intellij.psi.PsiComment
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.internal.AutoCorrectable
import org.jetbrains.kotlin.psi.KtFile

/**
 * Enforces MineKot first-column comments and compact comment markers.
 */
@AutoCorrectable(since = "2.0.0")
class CommentFormattingRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "CommentFormatting",
        severity = Severity.Style,
        description = "MineKot line and block comments start in the first column without marker padding.",
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
        if (comment.text.startsWith("/**") || comment.text.contains("@formatter:")) {
            return
        }
        val source = comment.containingFile.text
        val lineStart = source.lastIndexOf('\n', comment.textRange.startOffset - 1) + 1
        val prefix = source.substring(lineStart, comment.textRange.startOffset)
        if (prefix.isNotEmpty()) {
            reportFinding(comment, "Place this comment in the first column.")
            if (prefix.isBlank()) {
                edits.replace(lineStart, comment.textRange.startOffset, "")
            }
        }
        if (comment.text.startsWith("// ") || comment.text.startsWith("/* ")) {
            reportFinding(comment, "Do not add a space after the comment marker.")
            edits.replace(comment.textRange.startOffset + 2, comment.textRange.startOffset + 3, "")
        }
    }

    private fun reportFinding(comment: PsiComment, message: String) {
        report(CodeSmell(issue, Entity.from(comment), message))
    }
}
