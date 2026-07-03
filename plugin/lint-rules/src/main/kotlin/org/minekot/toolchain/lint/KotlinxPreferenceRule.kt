package org.minekot.toolchain.lint

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * Flags Java APIs where Kotlin standard library or kotlinx APIs are preferred.
 */
class KotlinxPreferenceRule(config: Config) : Rule(config) {
    override val issue: Issue = Issue(
        id = "KotlinxPreference",
        severity = Severity.Style,
        description = "MineKot prefers Kotlin standard library and kotlinx APIs over Java equivalents.",
        debt = Debt.TEN_MINS,
    )

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        val path = importDirective.importedFqName?.asString() ?: return
        val replacement = blockedImports[path] ?: return
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(importDirective),
                message = "Prefer ${replacement} over ${path}.",
            ),
        )
    }

    private companion object {
        private val blockedImports: Map<String, String> = mapOf(
            "java.io.File" to "java.nio.file.Path with kotlin.io.path or kotlinx-io",
            "java.nio.file.Files" to "kotlin.io.path or kotlinx-io helpers",
            "java.util.Timer" to "kotlinx-coroutines",
            "java.lang.Thread" to "kotlinx-coroutines",
            "java.util.ArrayList" to "Kotlin MutableList",
            "java.util.HashMap" to "Kotlin MutableMap",
            "java.util.HashSet" to "Kotlin MutableSet",
        )
    }
}
