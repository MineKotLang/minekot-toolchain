package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Prefers forEach for collection iteration when loop control and indexed iteration are unnecessary.
 */
class ForEachPreferenceRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "ForEachPreference",
        severity = Severity.Style,
        description = "MineKot prefers forEach for straightforward collection iteration.",
        debt = Debt.FIVE_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

    override fun visitForExpression(expression: KtForExpression) {
        super.visitForExpression(expression)
        val loopRange = expression.loopRange?.text ?: return
        val body = expression.body ?: return
        if (
            loopRange.isIndexedIteration() ||
            body.hasLoopControlFor(expression)
        ) {
            return
        }
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "Use forEach for collection iteration that does not require loop control.",
            ),
        )
    }

    private fun String.isIndexedIteration(): Boolean =
        endsWith(".indices") ||
                endsWith(".withIndex()") ||
                contains("..") ||
                contains(" until ") ||
                contains(" downTo ")

    private fun org.jetbrains.kotlin.psi.KtExpression.hasLoopControlFor(loop: KtForExpression): Boolean =
        collectDescendantsOfType<KtBreakExpression>().any { jump -> jump.targets(loop) } ||
                collectDescendantsOfType<KtContinueExpression>().any { jump -> jump.targets(loop) }

    private fun org.jetbrains.kotlin.psi.KtExpression.targets(loop: KtLoopExpression): Boolean =
        parents.filterIsInstance<KtLoopExpression>().firstOrNull() == loop
}
