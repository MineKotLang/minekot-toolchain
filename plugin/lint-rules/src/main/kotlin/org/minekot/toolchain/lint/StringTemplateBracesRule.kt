package org.minekot.toolchain.lint

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Requires braces for string template entries.
 */
class StringTemplateBracesRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = "StringTemplateBraces",
        severity = Severity.Style,
        description = "MineKot requires braces around string template variables.",
        debt = Debt.FIVE_MINS,
    )

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
        super.visitStringTemplateExpression(expression)
        expression.entries.filterIsInstance<KtSimpleNameStringTemplateEntry>().forEach { entry ->
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(entry),
                    message = "Use braces in string templates: ${'$'}{${entry.expression?.text ?: "value"}}.",
                ),
            )
        }
    }
}
