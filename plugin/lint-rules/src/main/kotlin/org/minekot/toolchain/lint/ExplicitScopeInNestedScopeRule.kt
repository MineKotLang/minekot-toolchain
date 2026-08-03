package org.minekot.toolchain.lint

import com.intellij.psi.util.PsiTreeUtil
import dev.detekt.api.*
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Reports resolved implicit outer class or object member access inside nested scopes.
 *
 * Explicit receivers and lambda-receiver DSL members remain valid because neither relies on an implicit outer class.
 */
class ExplicitScopeInNestedScopeRule(config: Config) :
    Rule(config, "MineKot codestyle rule."),
    RequiresAnalysisApi {
    private val issue: Issue = Issue(
        id = "ExplicitScopeInNestedScope",
        severity = Severity.Style,
        description = "MineKot nested scopes explicitly qualify outer receiver members.",
        debt = Debt.FIVE_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        super.visitSimpleNameExpression(expression)
        val nameReference = expression as? KtNameReferenceExpression ?: return
        inspectReference(nameReference)
    }

    private fun inspectReference(expression: KtNameReferenceExpression) {
        if (
            expression.isInsideMineKotFormatterControl() ||
            !expression.isUnqualified() ||
            expression.parents.none { parent -> parent is KtLambdaExpression }
        ) {
            return
        }
        val isOuterClassReceiverAccess = analyze(expression) {
            val call = (expression.parent as? KtCallExpression)
                ?.takeIf { callExpression -> callExpression.calleeExpression == expression }
                ?.resolveToCall()
                ?.singleFunctionCallOrNull()
                ?: expression.resolveToCall()?.singleVariableAccessCall()
                ?: return@analyze false
            val receiver = (call.dispatchReceiver ?: call.extensionReceiver).unwrapSmartCast() ?: return@analyze false
            val receiverSymbol = (receiver as? KaImplicitReceiverValue)?.symbol as? KaClassSymbol
                ?: return@analyze false
            val nearestLambda = expression.parents.filterIsInstance<KtLambdaExpression>().firstOrNull()
                ?: return@analyze false
            val receiverPsi = receiverSymbol.psi
            receiverPsi == null || !PsiTreeUtil.isAncestor(nearestLambda, receiverPsi, false)
        }
        if (!isOuterClassReceiverAccess) {
            return
        }
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "Qualify outer receiver member ${expression.getReferencedName()} with an explicit labeled this.",
            ),
        )
    }

    private fun KaReceiverValue?.unwrapSmartCast(): KaReceiverValue? =
        when (this) {
            is KaSmartCastedReceiverValue -> original.unwrapSmartCast()
            else -> this
        }

    private fun KtNameReferenceExpression.isUnqualified(): Boolean {
        val selector = (parent as? KtCallExpression) ?: this
        val qualifiedExpression = selector.parent as? KtQualifiedExpression ?: return true
        return qualifiedExpression.selectorExpression != selector
    }
}
