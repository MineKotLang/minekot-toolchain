package org.minekot.toolchain.lint

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.*

/**
 * Flags obvious unsafe Result handling.
 */
class ResultHandlingRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = "ResultHandling",
        severity = Severity.Style,
        description = "MineKot Result values should be handled explicitly.",
        debt = Debt.FIVE_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        when (expression.calleeExpression?.text) {
            "getOrThrow" -> reportFinding(
                expression,
                "Avoid unsafe getOrThrow; use getOrElse, getOrNull, or explicit handling.",
            )

            "runCatching" -> if (expression.isIgnoredRunCatching()) {
                reportFinding(
                    expression,
                    "Handle runCatching Result with onSuccess, onFailure, getOrElse, or getOrNull.",
                )
            }
        }
    }

    private fun KtCallExpression.isIgnoredRunCatching(): Boolean =
        when (val container = parent) {
            is KtDotQualifiedExpression,
            is KtProperty,
            is KtReturnExpression,
            is KtNamedFunction,
                -> false

            is KtBlockExpression -> !container.usesLastStatementAsValue(this)
            else -> true
        }

    private fun KtBlockExpression.usesLastStatementAsValue(expression: KtCallExpression): Boolean {
        if (statements.lastOrNull() != expression) {
            return false
        }
        return when (parent) {
            is KtTryExpression,
            is KtIfExpression,
            is KtFunctionLiteral,
            is KtWhenEntry,
            is KtReturnExpression,
            is KtProperty,
            is KtDotQualifiedExpression,
                -> true

            else -> false
        }
    }

    private fun reportFinding(expression: KtCallExpression, message: String) {
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = message,
            ),
        )
    }
}
