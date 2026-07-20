package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.internal.AutoCorrectable
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Prefers forEach for collection iteration when loop control and indexed iteration are unnecessary.
 */
@AutoCorrectable(since = "2.0.0")
class ForEachPreferenceRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val edits: MineKotTextEdits = MineKotTextEdits()
    private val issue: Issue = Issue(
        id = "ForEachPreference",
        severity = Severity.Style,
        description = "MineKot prefers forEach for straightforward collection iteration.",
        debt = Debt.FIVE_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

    override fun preVisit(root: KtFile) {
        edits.clear()
    }

    override fun postVisit(root: KtFile) {
        edits.applyTo(root, autoCorrect)
    }

    override fun visitForExpression(expression: KtForExpression) {
        super.visitForExpression(expression)
        val loopRangeExpression = expression.loopRange ?: return
        val loopRange = loopRangeExpression.text
        val body = expression.body ?: return
        if (
            loopRange.isIndexedIteration() ||
            expression.isInsideMineKotFormatterControl() ||
            body.hasLoopControlFor(expression) ||
            body.collectDescendantsOfType<KtReturnExpression>().isNotEmpty() ||
            body.text.hasMutationRisk()
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
        val parameter = expression.loopParameter?.text ?: return
        val block = body.text.takeIf { source -> source.startsWith('{') && source.endsWith('}') } ?: return
        val content = block.substring(1, block.length - 1)
        edits.replace(
            expression.textRange.startOffset,
            expression.textRange.endOffset,
            "${loopRangeExpression.forEachReceiver()}.forEach { ${parameter} ->${content}}",
        )
    }

    private fun KtExpression.forEachReceiver(): String =
        if (this is KtNameReferenceExpression || this is KtDotQualifiedExpression || this is KtCallExpression) {
            text
        } else {
            "(${text})"
        }

    private fun String.isIndexedIteration(): Boolean =
        endsWith(".indices") ||
                endsWith(".withIndex()") ||
                contains("..") ||
                contains(" until ") ||
                contains(" downTo ")

    private fun String.hasMutationRisk(): Boolean = mutationPattern.containsMatchIn(this)

    private fun KtExpression.hasLoopControlFor(loop: KtForExpression): Boolean =
        collectDescendantsOfType<KtBreakExpression>().any { jump -> jump.targets(loop) } ||
                collectDescendantsOfType<KtContinueExpression>().any { jump -> jump.targets(loop) }

    private fun KtExpression.targets(loop: KtLoopExpression): Boolean =
        parents.filterIsInstance<KtLoopExpression>().firstOrNull() == loop

    private companion object {
        private val mutationPattern: Regex = Regex(
            "(?:\\[[^]]+]\\s*(?:[+*/%-]=|=(?!=))|" +
                    "\\.(?:add|addAll|clear|put|putAll|remove|removeAll|removeAt|removeIf|replaceAll|retainAll|" +
                    "set|shuffle|sort|sortBy|sortWith)\\s*\\(|" +
                    "\\b[A-Za-z_][A-Za-z0-9_]*\\s*(?:[+*/%-]=|=(?!=)))",
        )
    }
}
