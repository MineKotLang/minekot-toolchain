package org.minekot.toolchain.lint.structure

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.internal.AutoCorrectable
import com.intellij.psi.PsiComment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.minekot.toolchain.lint.core.*

/**
 * Enforces source-file encoding markers and formatter-control pairing.
 */
@AutoCorrectable(since = "2.0.0")
class SourceFilePolicyRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "SourceFilePolicy",
        severity = Severity.Style,
        description = "MineKot source files use UTF-8 without BOM and balanced formatter tags.",
        debt = Debt.FIVE_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)
    private val edits = MineKotTextEdits()

    override fun preVisit(root: KtFile) {
        edits.clear()
    }

    override fun postVisit(root: KtFile) {
        edits.applyTo(root, autoCorrect, allowPartialSyntax = true)
    }

    override fun visit(root: KtFile) {
        super.visit(root)
        val source = root.text
        reportIf(root, source.startsWith('\uFEFF'), "Remove the UTF-8 byte-order mark from this source file.")
        if (source.startsWith('\uFEFF')) {
            edits.replace(0, 1, "")
        }
        reportFormatterTags(root)
    }

    private fun reportFormatterTags(root: KtFile) {
        val disabledRegions = ArrayDeque<PsiComment>()
        root.collectDescendantsOfType<PsiComment>().forEach { comment ->
            formatterTagPattern.findAll(comment.text).forEach { match ->
                when (match.groupValues[1]) {
                    "off" -> disabledRegions.addLast(comment)
                    "on" -> if (disabledRegions.isEmpty()) {
                        reportFinding(root, "Remove this unmatched @formatter:" + "on tag.")
                    } else {
                        disabledRegions.removeLast()
                    }
                }
            }
        }
        if (disabledRegions.isNotEmpty()) {
            reportFinding(root, "Close every @formatter:" + "off region with @formatter:" + "on.")
        }
    }

    private fun reportIf(root: KtFile, condition: Boolean, message: String) {
        if (condition) {
            reportFinding(root, message)
        }
    }

    private fun reportFinding(root: KtFile, message: String) {
        report(CodeSmell(issue, Entity.from(root), message))
    }

    private companion object {
        private val formatterTagPattern: Regex = Regex("@formatter:(off|on)")
    }
}
