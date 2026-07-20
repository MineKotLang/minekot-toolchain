package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

/**
 * Requires KDoc on methods and complicated declarations.
 */
class MissingKDocRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "MissingKDoc",
        severity = Severity.Style,
        description = "MineKot methods and complicated declarations require KDoc.",
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
        val returnsValue = function.returnsValue()
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

    private fun KtDeclaration.generatedKDoc(insertionOffset: Int): String {
        val indentation = sourceIndentation()
        val description = when (this) {
            is KtNamedFunction -> "Performs ${name.toDocumentationWords()}."
            is KtProperty -> "Values used by ${name.toDocumentationWords()}."
            is KtObjectDeclaration -> "Provides ${name.toDocumentationWords()}."
            else -> "Represents ${name.toDocumentationWords()}."
        }
        val tags = when (this) {
            is KtNamedFunction -> buildList {
                valueParameters.mapNotNull(KtParameter::getName).forEach { parameterName ->
                    val parameterDescription = parameterName.toDocumentationWords().replaceFirstChar(Char::uppercase)
                    add("@param ${parameterName} ${parameterDescription} value.")
                }
                if (returnsValue()) {
                    add("@return Result produced by ${name.toDocumentationWords()}.")
                }
            }

            else -> emptyList()
        }
        val sourcePrefix = containingKtFile.text.take(insertionOffset)
        val previousToken = sourcePrefix.trimEnd().lastOrNull()
        val paragraphSeparator = if (
            sourcePrefix.isNotEmpty() &&
            !sourcePrefix.endsWith("\n\n") &&
            previousToken != '{'
        ) {
            "\n"
        } else {
            ""
        }
        return buildString {
            append(paragraphSeparator)
            append("${indentation}/**\n")
            append("${indentation} * ${description}\n")
            if (tags.isNotEmpty()) {
                append("${indentation} *\n")
                tags.forEach { tag -> append("${indentation} * ${tag}\n") }
            }
            append("${indentation} */\n${indentation}")
        }
    }

    private fun KtNamedFunction.returnsValue(): Boolean =
        typeReference?.text !in unitReturnTypes || typeReference == null && !hasBlockBody()

    private fun KtDeclaration.sourceIndentation(): String {
        val source = containingKtFile.text
        val anchorOffset = (this as? KtNamedDeclaration)?.nameIdentifier?.textRange?.startOffset
            ?: textRange.startOffset
        val lineStart = source.lastIndexOf('\n', anchorOffset - 1) + 1
        return source.substring(lineStart, anchorOffset).takeWhile(Char::isWhitespace)
    }

    private fun KtDeclaration.documentationInsertion(): DocumentationInsertion {
        val source = containingKtFile.text
        val anchorOffset = (this as? KtNamedDeclaration)?.nameIdentifier?.textRange?.startOffset
            ?: textRange.startOffset
        val lineStartOffset = source.lastIndexOf('\n', anchorOffset - 1) + 1
        val relativeOffset = source.substring(lineStartOffset, anchorOffset)
            .indexOfFirst { character -> !character.isWhitespace() }
        val declarationOffset = if (relativeOffset < 0) lineStartOffset else lineStartOffset + relativeOffset
        return DocumentationInsertion(lineStartOffset, declarationOffset)
    }

    private fun String.withMissingTags(
        indentation: String,
        missingTags: List<String>,
    ): String {
        val content = removeSuffix("*/").trimEnd()
        val separator = if (content.lines().lastOrNull()?.trim() == "*") "" else "\n${indentation} *"
        val tags = missingTags.joinToString(separator = "\n") { tag -> "${indentation} * ${tag}" }
        return "${content}${separator}\n${tags}\n${indentation} */"
    }

    private fun String?.toDocumentationWords(): String =
        this
            ?.replace(Regex("([a-z0-9])([A-Z])"), "\$1 \$2")
            ?.replace('_', ' ')
            ?.lowercase()
            ?: "declaration"

    private fun KtDeclaration.requiresKDoc(): Boolean =
        (this !is KtClassOrObject || name != null) &&
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
                parent !is KtBlockExpression

    private fun KtCallableDeclaration.requiresVariableKDoc(): Boolean =
        hasComplicatedType() &&
                parent !is KtNamedFunction &&
                parent !is KtProperty &&
                parent !is KtBlockExpression &&
                generateSequence(parent) { element -> element.parent }.none { element ->
                    element is KtBlockExpression || element is KtNamedFunction
                }

    private fun KtCallableDeclaration.hasComplicatedType(): Boolean {
        val type = typeReference?.text ?: (this as? KtProperty)?.initializer?.text ?: return false
        return inferredComplicatedTypePattern.containsMatchIn(type) || complicatedTypeNames.any { typeName ->
            Regex(
                "(?:^|[<,.? ])${Regex.escape(typeName)}(?:$|[<>,.? (])",
            ).containsMatchIn(type)
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

private data class DocumentationInsertion(
    val lineStartOffset: Int,
    val declarationOffset: Int,
)
