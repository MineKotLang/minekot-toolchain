package org.minekot.toolchain.lint

import dev.detekt.api.*
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.psi.KtCallExpression

/** Resolves forbidden Java and non-kotlinx calls using Kotlin Analysis API. */
class ResolvedApiPreferenceRule(config: Config) :
    Rule(config, "MineKot codestyle rule."),
    RequiresAnalysisApi {
    private val issue: Issue = Issue(
        id = "ResolvedApiPreference",
        severity = Severity.Style,
        description = "MineKot resolves Java and non-kotlinx APIs before reporting replacements.",
        debt = Debt.TEN_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val resolvedName = analyze(expression) {
            val symbol = expression.resolveToCall()?.singleFunctionCallOrNull()?.symbol ?: return@analyze null
            if (symbol is KaConstructorSymbol) {
                symbol.containingClassId?.asSingleFqName()?.asString()?.plus(".<init>")
            } else {
                symbol.callableId?.asSingleFqName()?.asString()
            }
        } ?: return
        val replacement = forbiddenResolvedCalls.entries.firstOrNull { (call, _) ->
            resolvedName == call || resolvedName.startsWith("${call}.")
        }?.value ?: return
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "Prefer ${replacement} over resolved call ${resolvedName}.",
            ),
        )
    }

    private companion object {
        private val forbiddenResolvedCalls: Map<String, String> = mapOf(
            "java.io.File.<init>" to "java.nio.file.Path and kotlinx-io",
            "java.lang.Thread.<init>" to "kotlinx-coroutines",
            "java.lang.Thread.sleep" to "kotlinx.coroutines.delay",
            "java.nio.file.Files" to "kotlin.io.path or kotlinx-io",
            "java.util.Timer.<init>" to "kotlinx-coroutines",
            "java.util.concurrent.CompletableFuture" to "kotlinx-coroutines Deferred",
            "java.util.concurrent.Executors" to "a lifecycle-owned CoroutineDispatcher",
        )
    }
}
