package org.minekot.toolchain.lint

import dev.detekt.api.*
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Reports resolved direct JDK APIs when a Kotlin, kotlinx, or MineKot layer owns the capability.
 */
class ResolvedApiPreferenceRule(config: Config) :
    Rule(config, "MineKot codestyle rule."),
    RequiresAnalysisApi {
    private val issue: Issue = Issue(
        id = "ResolvedApiPreference",
        severity = Severity.Style,
        description = "MineKot consumers use Kotlin, kotlinx, and MineKot wrappers before direct JDK APIs.",
        debt = Debt.TEN_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (expression.isInsideMineKotNativeBoundary(config)) {
            return
        }
        val resolvedCall = expression.resolveMineKotCall() ?: return
        val preference = resolvedPreferences.entries.firstOrNull { (callableId, _) ->
            resolvedCall.callableId == callableId || resolvedCall.callableId.startsWith("${callableId}.")
        }?.value ?: return
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "Prefer ${preference} over resolved call ${resolvedCall.callableId} outside a helper or adapter boundary.",
            ),
        )
    }

    private companion object {
        private val resolvedPreferences: Map<String, String> = mapOf(
            "java.io.BufferedInputStream.<init>" to "kotlinx-io or a MineKot IO wrapper",
            "java.io.BufferedOutputStream.<init>" to "kotlinx-io or a MineKot IO wrapper",
            "java.io.File.<init>" to "Path plus a MineKot path wrapper",
            "java.io.FileInputStream.<init>" to "kotlinx-io or a MineKot IO wrapper",
            "java.io.FileOutputStream.<init>" to "kotlinx-io or a MineKot IO wrapper",
            "java.io.FileReader.<init>" to "kotlinx-io or a MineKot IO wrapper",
            "java.io.FileWriter.<init>" to "kotlinx-io or a MineKot IO wrapper",
            "java.nio.file.Files" to "kotlin.io.path, kotlinx-io, or a MineKot IO wrapper",
            "java.util.ArrayList.<init>" to "mutableListOf",
            "java.util.HashMap.<init>" to "mutableMapOf",
            "java.util.HashSet.<init>" to "mutableSetOf",
        )
    }
}
