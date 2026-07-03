package org.minekot.toolchain.lint

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Flags obvious text patterns that should use MiniMessage.
 */
class MiniMessageTextRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = "MiniMessageText",
        severity = Severity.Style,
        description = "MineKot user-facing text uses MiniMessage instead of legacy color codes or concatenation.",
        debt = Debt.TEN_MINS,
    )

    private val legacyColorPattern = Regex("(?i)(§[0-9a-fk-or]|&[0-9a-fk-or])")

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
        super.visitStringTemplateExpression(expression)
        val text = expression.text
        if (legacyColorPattern.containsMatchIn(text)) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Use MiniMessage tags instead of legacy color codes.",
                ),
            )
        }
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        super.visitBinaryExpression(expression)
        if (expression.operationToken == KtTokens.PLUS && expression.hasStringTemplateOperand()) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Avoid string concatenation for user-facing text; prefer MiniMessage templates.",
                ),
            )
        }
    }

    private fun KtBinaryExpression.hasStringTemplateOperand(): Boolean =
        left.containsStringTemplate() || right.containsStringTemplate()

    private fun KtExpression?.containsStringTemplate(): Boolean =
        when (this) {
            is KtStringTemplateExpression -> true
            is KtBinaryExpression -> hasStringTemplateOperand()
            else -> false
        }
}
