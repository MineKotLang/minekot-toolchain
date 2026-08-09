package org.minekot.toolchain.lint.practices

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.minekot.toolchain.lint.core.CodeSmell
import org.minekot.toolchain.lint.core.Debt
import org.minekot.toolchain.lint.core.Issue
import org.minekot.toolchain.lint.core.Severity

/**
 * Requires KDoc for public API and syntactically non-obvious internal properties.
 */
class MissingKDocRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "MissingKDoc",
        severity = Severity.Style,
        description = "MineKot API contracts and non-obvious internal state require useful KDoc.",
        debt = Debt.FIVE_MINS,
    )
    override val ruleName: RuleName get() = RuleName(issue.id)

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
        if (declaration.hasKDoc() || !declaration.requiresContractKDoc()) {
            return
        }
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(declaration),
                message = "Add KDoc that explains this ${declarationKind} contract.",
            ),
        )
    }

    private fun KtDeclaration.hasKDoc(): Boolean =
        docComment != null ||
                this is KtParameter && containingClassKDocDocumentsProperty()

    private fun KtParameter.containingClassKDocDocumentsProperty(): Boolean {
        val propertyName = name ?: return false
        val containingClass = generateSequence(parent) { element -> element.parent }
            .filterIsInstance<KtClassOrObject>()
            .firstOrNull() ?: return false
        val classKDoc = containingClass.docComment?.text ?: return false
        return Regex("@property\\s+${Regex.escape(propertyName)}(?:\\s|$)").containsMatchIn(classKDoc)
    }

    private fun KtDeclaration.requiresContractKDoc(): Boolean {
        if (
            this is KtClassOrObject && name == null ||
            hasModifier(KtTokens.PRIVATE_KEYWORD) ||
            hasModifier(KtTokens.OVERRIDE_KEYWORD) ||
            isLocalDeclaration() ||
            hasNonPublicContainer()
        ) {
            return false
        }
        if (hasModifier(KtTokens.INTERNAL_KEYWORD)) {
            return this is KtCallableDeclaration && hasComplicatedType()
        }
        return true
    }

    private fun KtDeclaration.isLocalDeclaration(): Boolean =
        parent is KtBlockExpression ||
                parent is KtNamedFunction ||
                parent is KtProperty ||
                generateSequence(parent) { element -> element.parent }
                    .any { element -> element is KtBlockExpression || element is KtNamedFunction }

    private fun KtDeclaration.hasNonPublicContainer(): Boolean =
        generateSequence(parent) { element -> element.parent }
            .filterIsInstance<KtClassOrObject>()
            .any { container ->
                container.hasModifier(KtTokens.PRIVATE_KEYWORD) ||
                        container.hasModifier(KtTokens.INTERNAL_KEYWORD)
            }

    private fun KtCallableDeclaration.hasComplicatedType(): Boolean {
        val type = typeReference?.text ?: (this as? KtProperty)?.initializer?.text ?: return false
        return inferredComplicatedTypePattern.containsMatchIn(type) || complicatedTypeNames.any { typeName ->
            Regex(
                "(?:^|[<,.? ])${Regex.escape(typeName)}(?:$|[<>,.? (])",
            ).containsMatchIn(type)
        }
    }

    private companion object {
        private val inferredComplicatedTypePattern: Regex = Regex(
            "\\b(?:empty(?:List|Map|Set)|mutable(?:List|Map|Set)Of|(?:array|list|map|sequence|set)Of)\\s*(?:<|\\()",
        )
        private val complicatedTypeNames: Set<String> = setOf(
            "Array",
            "Collection",
            "Flow",
            "List",
            "Map",
            "MutableCollection",
            "MutableList",
            "MutableMap",
            "MutableSet",
            "Sequence",
            "Set",
            "StateFlow",
        )
    }
}
