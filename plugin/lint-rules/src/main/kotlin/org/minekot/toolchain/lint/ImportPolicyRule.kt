package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.internal.AutoCorrectable
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * Enforces MineKot wildcard thresholds and nested-class import policy.
 */
@AutoCorrectable(since = "2.0.0")
class ImportPolicyRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "ImportPolicy",
        severity = Severity.Style,
        description = "MineKot imports use data-driven wildcard thresholds and qualified nested classes.",
        debt = Debt.FIVE_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)
    private val edits = MineKotTextEdits()

    override fun preVisit(root: KtFile) {
        edits.clear()
    }

    override fun postVisit(root: KtFile) {
        edits.applyTo(root, autoCorrect)
    }

    override fun visit(root: KtFile) {
        super.visit(root)
        root.reportMissingWildcardImports()
    }

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        val segments = importDirective.importPath?.pathStr?.split('.') ?: return
        if (
            !importDirective.isAllUnder &&
            segments.size >= 2 &&
            segments.takeLast(2).all { segment -> segment.firstOrNull()?.isUpperCase() == true }
        ) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(importDirective),
                    message = "Do not import nested classes directly; qualify them with their outer class.",
                ),
            )
        }
    }

    private fun KtFile.reportMissingWildcardImports() {
        importDirectives
            .filterNot { directive -> directive.isAllUnder || directive.aliasName != null }
            .groupBy { directive -> directive.importedFqName?.parent()?.asString().orEmpty() }
            .forEach { (scope, imports) ->
                // Detekt runs without reliable symbol resolution here. An uppercase final segment may
                // be an object, and Kotlin rejects star imports from objects. Never report or correct
                // these scopes unless their declaration kind can be proven.
                if (scope.substringAfterLast('.').firstOrNull()?.isUpperCase() == true) {
                    return@forEach
                }
                val threshold = when {
                    scope == javaUtilPackage || recursiveOnDemandPackages.any { packageName ->
                        scope == packageName || scope.startsWith("${packageName}.")
                    } -> onDemandWildcardThreshold

                    else -> packageWildcardThreshold
                }
                if (scope.isBlank() || imports.size < threshold) {
                    return@forEach
                }
                report(
                    CodeSmell(
                        issue = issue,
                        entity = Entity.from(imports.first()),
                        message = "Replace ${imports.size} imports from ${scope} with ${scope}.*.",
                    ),
                )
                scheduleWildcardImport(scope, imports)
            }
    }

    private fun scheduleWildcardImport(scope: String, imports: List<KtImportDirective>) {
        if (imports.any { directive -> directive.text.contains("//") || directive.text.contains("/*") }) {
            return
        }
        val firstImport = imports.minByOrNull { directive -> directive.textRange.startOffset } ?: return
        val wildcardImport = "import ${scope}.*"
        edits.replace(firstImport.textRange.startOffset, firstImport.textRange.endOffset, wildcardImport)
        imports
            .filterNot { directive -> directive == firstImport }
            .forEach { directive ->
                val source = directive.containingFile.text
                val endOffset = if (source.getOrNull(directive.textRange.endOffset) == '\n') {
                    directive.textRange.endOffset + 1
                } else {
                    directive.textRange.endOffset
                }
                edits.replace(directive.textRange.startOffset, endOffset, "")
            }
    }

    private companion object {
        private const val packageWildcardThreshold: Int = 5
        private const val onDemandWildcardThreshold: Int = 1
        private const val javaUtilPackage: String = "java.util"
        private val recursiveOnDemandPackages: Set<String> = setOf("io.ktor", "kotlinx.android.synthetic")
    }
}
