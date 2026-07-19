package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.internal.AutoCorrectable
import org.jetbrains.kotlin.psi.KtFile

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
        edits.applyTo(root, autoCorrect)
    }

    override fun visit(root: KtFile) {
        super.visit(root)
        val source = root.text
        reportIf(root, source.startsWith('\uFEFF'), "Remove the UTF-8 byte-order mark from this source file.")
        if (source.startsWith('\uFEFF')) {
            edits.replace(0, 1, "")
        }
        reportFormatterTags(root, source)
    }

    private fun reportFormatterTags(root: KtFile, source: String) {
        var disabledDepth = 0
        formatterTagPattern.findAll(source).forEach { match ->
            when (match.groupValues[1]) {
                "off" -> disabledDepth++
                "on" -> if (disabledDepth == 0) {
                    reportFinding(root, "Remove this unmatched @formatter:on tag.")
                } else {
                    disabledDepth--
                }
            }
        }
        if (disabledDepth > 0) {
            reportFinding(root, "Close every @formatter:off region with @formatter:on.")
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
