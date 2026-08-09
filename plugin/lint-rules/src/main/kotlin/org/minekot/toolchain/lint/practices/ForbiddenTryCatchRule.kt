package org.minekot.toolchain.lint.practices

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.minekot.toolchain.lint.core.CodeSmell
import org.minekot.toolchain.lint.core.Debt
import org.minekot.toolchain.lint.core.Issue
import org.minekot.toolchain.lint.core.Severity

/**
 * Flags catch clauses that swallow failures or can break coroutine cancellation.
 */
class ForbiddenTryCatchRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "ForbiddenTryCatch",
        severity = Severity.Style,
        description = "MineKot exception boundaries must preserve cancellation and handle failures explicitly.",
        debt = Debt.FIVE_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

    override fun visitTryExpression(expression: KtTryExpression) {
        super.visitTryExpression(expression)
        expression.catchClauses
            .filter { catchClause -> catchClause.isUnsafeCatch() }
            .forEach { catchClause ->
                report(
                    CodeSmell(
                        issue = issue,
                        entity = Entity.from(catchClause),
                        message = "Do not swallow failures or intercept coroutine cancellation without rethrowing it.",
                    ),
                )
            }
    }

    private fun KtCatchClause.isUnsafeCatch(): Boolean {
        val body = catchBody as? KtBlockExpression ?: return true
        if (body.statements.isEmpty() || body.statements.all { statement -> logOnlyPattern.matches(statement.text) }) {
            return true
        }
        val caughtType = catchParameter?.typeReference?.text?.substringAfterLast('.') ?: return false
        if (caughtType == "CancellationException") {
            return !rethrowsCaughtFailure()
        }
        if (caughtType !in cancellationSupertypes || isInsideTypedMineKotBoundary()) {
            return false
        }
        return !rethrowsCaughtFailure() ||
                body.statements.size > 1 && !body.text.contains("CancellationException")
    }

    private fun KtCatchClause.rethrowsCaughtFailure(): Boolean {
        val parameterName = catchParameter?.name ?: return false
        return catchBody
            ?.collectDescendantsOfType<KtThrowExpression>()
            ?.any { expression -> expression.thrownExpression?.text == parameterName } == true
    }

    private fun KtCatchClause.isInsideTypedMineKotBoundary(): Boolean =
        parents.filterIsInstance<KtNamedFunction>().any { function ->
            function.name == "mineKotRunCatching"
        }

    private companion object {
        private val cancellationSupertypes: Set<String> = setOf(
            "Exception",
            "IllegalStateException",
            "RuntimeException",
            "Throwable",
        )
        private val logOnlyPattern: Regex = Regex(
            "(?:[A-Za-z_][A-Za-z0-9_.]*\\.)?(?:debug|error|info|print|println|warn)\\s*\\(.*\\)",
            RegexOption.DOT_MATCHES_ALL,
        )
    }
}
