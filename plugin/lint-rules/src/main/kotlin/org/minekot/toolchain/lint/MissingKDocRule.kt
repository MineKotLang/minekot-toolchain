package org.minekot.toolchain.lint

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

/**
 * Requires KDoc on public declarations.
 */
class MissingKDocRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = "MissingKDoc",
        severity = Severity.Style,
        description = "MineKot public declarations require KDoc.",
        debt = Debt.FIVE_MINS,
    )

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        super.visitClassOrObject(classOrObject)
        reportIfMissingKDoc(classOrObject, "class or object")
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        reportIfMissingKDoc(function, "function")
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        reportIfMissingKDoc(property, "property")
    }

    override fun visitParameter(parameter: KtParameter) {
        super.visitParameter(parameter)
        if (parameter.hasValOrVar()) {
            reportIfMissingKDoc(parameter, "constructor property")
        }
    }

    private fun reportIfMissingKDoc(declaration: KtDeclaration, declarationKind: String) {
        if (declaration.docComment != null || !declaration.requiresKDoc()) {
            return
        }
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(declaration),
                message = "Add KDoc to this public ${declarationKind}.",
            ),
        )
    }

    private fun KtDeclaration.requiresKDoc(): Boolean =
        parent !is KtNamedFunction &&
                parent !is KtProperty &&
                parent !is KtBlockExpression &&
                !hasModifier(KtTokens.PRIVATE_KEYWORD) &&
                !hasModifier(KtTokens.INTERNAL_KEYWORD) &&
                !hasModifier(KtTokens.OVERRIDE_KEYWORD) &&
                !hasNonPublicContainer()

    private fun KtDeclaration.hasNonPublicContainer(): Boolean =
        generateSequence(parent) { element -> element.parent }
            .any { element ->
                element is KtBlockExpression ||
                        element is KtNamedFunction ||
                        element is KtClassOrObject &&
                        (
                                element.hasModifier(KtTokens.PRIVATE_KEYWORD) ||
                                        element.hasModifier(KtTokens.INTERNAL_KEYWORD)
                                )
            }
}
