package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.KtTryExpression

/**
 * Flags normal try-catch usage in MineKot code.
 */
class ForbiddenTryCatchRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "ForbiddenTryCatch",
        severity = Severity.Style,
        description = "MineKot uses runCatching and Result handling instead of normal try-catch blocks.",
        debt = Debt.FIVE_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

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
