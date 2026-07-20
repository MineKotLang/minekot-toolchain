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
 * Requires labeled outer-receiver access for known enclosing-class members used inside lambdas.
 */
@AutoCorrectable(since = "2.0.0")
class ExplicitScopeInNestedScopeRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val edits: MineKotTextEdits = MineKotTextEdits()
    private val issue: Issue = Issue(
        id = "ExplicitScopeInNestedScope",
        severity = Severity.Style,
        description = "MineKot nested scopes explicitly qualify outer receiver members.",
        debt = Debt.FIVE_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

    override fun preVisit(root: KtFile) {
        edits.clear()
    }

    override fun postVisit(root: KtFile) {
        edits.applyTo(root, autoCorrect)
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        super.visitLambdaExpression(lambdaExpression)
        if (lambdaExpression.isInsideMineKotFormatterControl()) {
            return
        }
        val containingClasses = lambdaExpression.parents.filterIsInstance<KtClassOrObject>().toList()
        if (containingClasses.isEmpty()) {
            return
        }
        val shadowedNames = buildSet {
            lambdaExpression.valueParameters.mapNotNullTo(this) { parameter -> parameter.name }
            lambdaExpression.collectDescendantsOfType<KtNamedDeclaration>()
                .mapNotNullTo(this) { declaration -> declaration.name }
            lambdaExpression.parents.filterIsInstance<KtNamedFunction>().firstOrNull()?.let { function ->
                function.valueParameters.mapNotNullTo(this) { parameter -> parameter.name }
                function.bodyExpression
                    ?.collectDescendantsOfType<KtNamedDeclaration> { declaration ->
                        declaration.textRange.startOffset < lambdaExpression.textRange.startOffset
                    }
                    ?.mapNotNullTo(this) { declaration -> declaration.name }
            }
        }
        lambdaExpression.bodyExpression
            ?.collectDescendantsOfType<KtNameReferenceExpression> { reference ->
                reference.parents
                    .takeWhile { parent -> parent != lambdaExpression }
                    .none { parent -> parent is KtLambdaExpression }
            }
            ?.forEach { reference ->
                val name = reference.getReferencedName()
                val receivers = containingClasses.filter { containingClass ->
                    containingClass.declarations.any { declaration ->
                        declaration.name == name && (declaration is KtProperty || declaration is KtNamedFunction)
                    }
                }
                val receiverName = receivers.singleOrNull()?.name
                if (receiverName != null && name !in shadowedNames && reference.isUnqualified()) {
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(reference),
                            "Qualify outer member ${name} with this@${receiverName}.",
                        ),
                    )
                    edits.replace(
                        reference.textRange.startOffset,
                        reference.textRange.endOffset,
                        "this@${receiverName}.${name}",
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
