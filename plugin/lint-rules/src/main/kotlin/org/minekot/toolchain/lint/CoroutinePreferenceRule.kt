package org.minekot.toolchain.lint

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

/**
 * Flags obvious thread and timer APIs where coroutines should be preferred.
 */
class CoroutinePreferenceRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = "CoroutinePreference",
        severity = Severity.Style,
        description = "MineKot uses coroutines instead of direct thread and timer APIs.",
        debt = Debt.TEN_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callName = expression.calleeExpression?.text ?: return
        if (expression.isBlockedConstructorCall(callName) || expression.isBlockedQualifiedCall()) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Use coroutines instead of direct thread or timer APIs.",
                ),
            )
        }
    }

    private fun KtCallExpression.isBlockedQualifiedCall(): Boolean {
        val qualifiedExpression = parent as? KtDotQualifiedExpression ?: return false
        val callee = calleeExpression?.text ?: return false
        val qualifiedCall = "${qualifiedExpression.receiverExpression.text}.${callee}"
        return blockedQualifiedCalls.any { blockedCall ->
            qualifiedCall == blockedCall || qualifiedCall.endsWith(".${blockedCall}")
        }
    }

    private fun KtCallExpression.isBlockedConstructorCall(callName: String): Boolean {
        if (callName in blockedConstructors) {
            return true
        }
        val qualifiedExpression = parent as? KtDotQualifiedExpression ?: return false
        val qualifiedCall = "${qualifiedExpression.receiverExpression.text}.${callName}"
        return blockedConstructors.any { constructor ->
            qualifiedCall.endsWith(".${constructor}")
        }
    }

    private companion object {
        private val blockedConstructors: Set<String> = setOf("Thread", "Timer")

        private val blockedQualifiedCalls: Set<String> = setOf("Thread.sleep")
    }
}
