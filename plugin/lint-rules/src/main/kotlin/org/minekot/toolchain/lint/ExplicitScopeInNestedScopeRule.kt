package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Requires labeled outer-receiver access for known enclosing-class members used inside lambdas.
 */
class ExplicitScopeInNestedScopeRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "ExplicitScopeInNestedScope",
        severity = Severity.Style,
        description = "MineKot nested scopes explicitly qualify outer receiver members.",
        debt = Debt.FIVE_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)
    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        super.visitLambdaExpression(lambdaExpression)
        val containingClass = lambdaExpression.parents.filterIsInstance<KtClassOrObject>().firstOrNull() ?: return
        val className = containingClass.name ?: return
        val memberNames = containingClass.declarations
            .filter { declaration -> declaration is KtProperty || declaration is KtNamedFunction }
            .mapNotNullTo(mutableSetOf()) { declaration -> declaration.name }
        if (memberNames.isEmpty()) {
            return
        }
        val shadowedNames = buildSet {
            lambdaExpression.valueParameters.mapNotNullTo(this) { parameter -> parameter.name }
            lambdaExpression.collectDescendantsOfType<KtNamedDeclaration>()
                .mapNotNullTo(this) { declaration -> declaration.name }
        }
        lambdaExpression.bodyExpression
            ?.collectDescendantsOfType<KtNameReferenceExpression> { reference ->
                reference.parents
                    .takeWhile { parent -> parent != lambdaExpression }
                    .none { parent -> parent is KtLambdaExpression }
            }
            ?.forEach { reference ->
                val name = reference.getReferencedName()
                if (name in memberNames && name !in shadowedNames && reference.isUnqualified()) {
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(reference),
                            "Qualify outer member ${name} with this@${className}.",
                        ),
                    )
                }
            }
    }

    private fun KtNameReferenceExpression.isUnqualified(): Boolean {
        val selector = (parent as? KtCallExpression) ?: this
        val qualifiedExpression = selector.parent as? KtDotQualifiedExpression ?: return true
        return qualifiedExpression.selectorExpression != selector
    }
}
