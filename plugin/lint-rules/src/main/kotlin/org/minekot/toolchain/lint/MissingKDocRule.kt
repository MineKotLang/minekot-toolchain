package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

/**
 * Requires KDoc on public declarations.
 */
class MissingKDocRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "MissingKDoc",
        severity = Severity.Style,
        description = "MineKot public declarations require KDoc.",
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
        reportIfIncompleteKDoc(function)
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
        val requiresKDoc = when (declaration) {
            is KtNamedFunction -> declaration.requiresMethodKDoc()
            is KtProperty -> declaration.requiresVariableKDoc()
            is KtParameter -> declaration.requiresVariableKDoc()
            else -> declaration.requiresKDoc()
        }
        if (declaration.hasKDoc() || !requiresKDoc) {
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

    private fun reportIfIncompleteKDoc(function: KtNamedFunction) {
        if (!function.requiresMethodKDoc()) {
            return
        }
        val kDoc = function.docComment?.text ?: return
        val undocumentedParameters = function.valueParameters.mapNotNull { parameter ->
            parameter.name?.takeUnless { parameterName ->
                Regex("@param\\s+${Regex.escape(parameterName)}(?:\\s|$)").containsMatchIn(kDoc)
            }
        }
        val returnsValue = function.typeReference?.text !in unitReturnTypes ||
                function.typeReference == null && !function.hasBlockBody()
        val requiresReturnTag = returnsValue &&
                !Regex("@return(?:\\s|$)").containsMatchIn(kDoc)
        if (undocumentedParameters.isEmpty() && !requiresReturnTag) {
            return
        }
        val missingTags = buildList {
            undocumentedParameters.forEach { parameterName -> add("@param ${parameterName}") }
            if (requiresReturnTag) {
                add("@return")
            }
        }
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(function),
                message = "Complete this function KDoc with ${missingTags.joinToString()}.",
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

    private fun KtNamedFunction.requiresMethodKDoc(): Boolean =
        parent !is KtNamedFunction &&
                parent !is KtProperty &&
                parent !is KtBlockExpression &&
                !hasModifier(KtTokens.OVERRIDE_KEYWORD)

    private fun KtCallableDeclaration.requiresVariableKDoc(): Boolean =
        hasComplicatedType() &&
                !hasModifier(KtTokens.OVERRIDE_KEYWORD) &&
                parent !is KtNamedFunction &&
                parent !is KtProperty &&
                parent !is KtBlockExpression &&
                generateSequence(parent) { element -> element.parent }.none { element ->
                    element is KtBlockExpression || element is KtNamedFunction
                }

    private fun KtCallableDeclaration.hasComplicatedType(): Boolean {
        val type = typeReference?.text ?: return false
        return complicatedTypeNames.any { typeName ->
            Regex("(?:^|[<,.? ])${Regex.escape(typeName)}(?:$|[<>,.? ])").containsMatchIn(type)
        }
    }

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

    private companion object {
        private val unitReturnTypes: Set<String?> = setOf(null, "Unit", "kotlin.Unit")
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
