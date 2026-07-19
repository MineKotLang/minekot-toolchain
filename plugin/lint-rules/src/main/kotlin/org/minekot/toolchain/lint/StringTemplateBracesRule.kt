package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.internal.AutoCorrectable
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Requires braces for string template entries.
 */
@AutoCorrectable(since = "2.0.0")
class StringTemplateBracesRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "StringTemplateBraces",
        severity = Severity.Style,
        description = "MineKot requires braces around string template variables.",
        debt = Debt.FIVE_MINS,
    )
    private val edits = MineKotTextEdits()

    override val ruleName: RuleName get() = RuleName(issue.id)

    override fun preVisit(root: KtFile) {
        edits.clear()
    }

    override fun postVisit(root: KtFile) {
        edits.applyTo(root, autoCorrect)
    }

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
        super.visitStringTemplateExpression(expression)
        expression.entries.filterIsInstance<KtSimpleNameStringTemplateEntry>().forEach { entry ->
            val entryExpression = entry.expression?.text ?: "value"
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(entry),
                    message = "Use braces in string templates: ${'$'}{${entryExpression}}.",
                ),
            )
            edits.replace(
                startOffset = entry.textRange.startOffset,
                endOffset = entry.textRange.endOffset,
                replacement = "${'$'}{${entryExpression}}",
            )
        }
    }
}
