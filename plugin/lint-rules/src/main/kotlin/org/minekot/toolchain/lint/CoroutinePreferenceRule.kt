package org.minekot.toolchain.lint

import dev.detekt.api.*
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Reports resolved direct concurrency and platform scheduler APIs outside approved boundaries.
 */
class CoroutinePreferenceRule(config: Config) :
    Rule(config, "MineKot codestyle rule."),
    RequiresAnalysisApi {
    private val issue: Issue = Issue(
        id = "CoroutinePreference",
        severity = Severity.Style,
        description = "MineKot consumers use coroutines and platform dispatch abstractions.",
        debt = Debt.TEN_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (expression.isInsideMineKotNativeBoundary(config)) {
            return
        }
        val resolvedCall = expression.resolveMineKotCall() ?: return
        val replacement = blockedConcurrencyCalls[resolvedCall.callableId] ?: return
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "Prefer ${replacement} over resolved call ${resolvedCall.callableId} outside a helper or adapter boundary.",
            ),
        )
    }

    private companion object {
        private val blockedConcurrencyCalls: Map<String, String> = mapOf(
            "java.lang.Thread.<init>" to "a lifecycle-owned coroutine scope",
            "java.lang.Thread.sleep" to "kotlinx.coroutines.delay",
            "java.util.Timer.<init>" to "a lifecycle-owned coroutine scope",
            "java.util.concurrent.CompletableFuture.runAsync" to "coroutine launch or async",
            "java.util.concurrent.CompletableFuture.supplyAsync" to "coroutine async",
            "java.util.concurrent.Executors.newCachedThreadPool" to "a lifecycle-owned CoroutineDispatcher",
            "java.util.concurrent.Executors.newFixedThreadPool" to "a lifecycle-owned CoroutineDispatcher",
            "java.util.concurrent.Executors.newScheduledThreadPool" to "a lifecycle-owned CoroutineDispatcher",
            "java.util.concurrent.Executors.newSingleThreadExecutor" to "a lifecycle-owned CoroutineDispatcher",
            "org.bukkit.scheduler.BukkitScheduler.runTask" to "MineKot platform dispatch",
            "org.bukkit.scheduler.BukkitScheduler.runTaskAsynchronously" to "MineKot platform dispatch",
            "org.bukkit.scheduler.BukkitScheduler.runTaskLater" to "MineKot platform dispatch",
            "org.bukkit.scheduler.BukkitScheduler.runTaskLaterAsynchronously" to "MineKot platform dispatch",
            "org.bukkit.scheduler.BukkitScheduler.runTaskTimer" to "MineKot platform dispatch",
            "org.bukkit.scheduler.BukkitScheduler.runTaskTimerAsynchronously" to "MineKot platform dispatch",
        )
    }
}
