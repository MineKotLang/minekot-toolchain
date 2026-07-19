package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.*

/**
 * Flags obvious unsafe Result handling.
 */
class ResultHandlingRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "ResultHandling",
        severity = Severity.Style,
        description = "MineKot Result values should be handled explicitly.",
        debt = Debt.FIVE_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        when (expression.calleeExpression?.text) {
            "getOrThrow" -> reportFinding(
                expression = expression,
                message = "Avoid unsafe getOrThrow; use getOrElse, getOrNull, or explicit handling.",
                enabled = expression.isKnownResultAccess(),
            )

            "runCatching" -> if (expression.isIgnoredRunCatching()) {
                reportFinding(
                    expression,
                    "Handle runCatching Result with onSuccess, onFailure, getOrElse, or getOrNull.",
                )
            }
        }
    }

    private fun KtCallExpression.isKnownResultAccess(): Boolean {
        val qualifiedExpression = parent as? KtDotQualifiedExpression ?: return false
        if (qualifiedExpression.selectorExpression != this) {
            return false
        }
        val receiver = qualifiedExpression.receiverExpression.text
        return knownResultProducerPrefixes.any(receiver::startsWith)
    }

    private fun KtCallExpression.isIgnoredRunCatching(): Boolean = !isResultConsumed()

    private fun KtExpression.isResultConsumed(): Boolean =
        when (val container = parent) {
            is KtDotQualifiedExpression,
            is KtProperty,
            is KtReturnExpression,
            is KtNamedFunction,
            is KtValueArgument,
                -> true

            is KtParenthesizedExpression -> container.isResultConsumed()
            is KtContainerNode -> container.isResultContainerConsumed()
            is KtIfExpression -> container.isResultConsumed()
            is KtTryExpression -> container.isResultConsumed()
            is KtWhenEntry -> (container.parent as? KtWhenExpression)?.isResultConsumed() == true
            is KtBlockExpression -> container.usesLastStatementAsValue(this)
            else -> false
        }

    private fun KtContainerNode.isResultContainerConsumed(): Boolean =
        when (val container = parent) {
            is KtIfExpression -> container.isResultConsumed()
            is KtWhenEntry -> (container.parent as? KtWhenExpression)?.isResultConsumed() == true
            else -> false
        }

    private fun KtBlockExpression.usesLastStatementAsValue(expression: KtExpression): Boolean {
        if (statements.lastOrNull() != expression) {
            return false
        }
        return when (val container = parent) {
            is KtFunctionLiteral -> true
            is KtIfExpression -> container.isResultConsumed()
            is KtTryExpression -> container.isResultConsumed()
            is KtWhenEntry -> (container.parent as? KtWhenExpression)?.isResultConsumed() == true
            else -> false
        }
    }

    private fun reportFinding(
        expression: KtCallExpression,
        message: String,
        enabled: Boolean = true,
    ) {
        if (!enabled) {
            return
        }
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = message,
            ),
        )
    }

    private companion object {
        private val knownResultProducerPrefixes: Set<String> = setOf(
            "runCatching",
            "kotlin.runCatching",
            "Result.success",
            "Result.failure",
            "kotlin.Result.success",
            "kotlin.Result.failure",
        )
    }
}
