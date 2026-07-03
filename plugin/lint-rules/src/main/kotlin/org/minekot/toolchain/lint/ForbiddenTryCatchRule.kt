package org.minekot.toolchain.lint

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtTryExpression

/**
 * Flags normal try-catch usage in MineKot code.
 */
class ForbiddenTryCatchRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = "ForbiddenTryCatch",
        severity = Severity.Style,
        description = "MineKot uses runCatching and Result handling instead of normal try-catch blocks.",
        debt = Debt.FIVE_MINS,
    )

    override fun visitTryExpression(expression: KtTryExpression) {
        super.visitTryExpression(expression)
        if (expression.catchClauses.isNotEmpty()) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Use runCatching and Result handling instead of try-catch.",
                ),
            )
        }
    }
}
