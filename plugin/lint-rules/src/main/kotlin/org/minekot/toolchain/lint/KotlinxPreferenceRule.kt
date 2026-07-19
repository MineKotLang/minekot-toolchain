package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * Flags Java APIs where Kotlin standard library or kotlinx APIs are preferred.
 */
class KotlinxPreferenceRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "KotlinxPreference",
        severity = Severity.Style,
        description = "MineKot prefers Kotlin standard library and kotlinx APIs over Java equivalents.",
        debt = Debt.TEN_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

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
            "java.io.BufferedInputStream" to "kotlinx-io Buffer",
            "java.io.BufferedOutputStream" to "kotlinx-io Buffer",
            "java.io.FileInputStream" to "kotlinx-io Source",
            "java.io.FileOutputStream" to "kotlinx-io Sink",
            "java.io.InputStream" to "kotlinx-io Source",
            "java.io.OutputStream" to "kotlinx-io Sink",
            "java.io.Reader" to "kotlinx-io Source",
            "java.io.Writer" to "kotlinx-io Sink",
            "java.util.concurrent.CompletableFuture" to "kotlinx-coroutines Deferred",
            "java.util.concurrent.Executor" to "kotlinx-coroutines CoroutineDispatcher",
            "java.util.concurrent.ExecutorService" to "kotlinx-coroutines CoroutineDispatcher",
            "java.util.concurrent.Executors" to "kotlinx-coroutines",
            "com.fasterxml.jackson.databind.ObjectMapper" to "kotlinx-serialization",
            "com.google.gson.Gson" to "kotlinx-serialization",
            "org.json.JSONArray" to "kotlinx-serialization",
            "org.json.JSONObject" to "kotlinx-serialization",
        )
    }
}
