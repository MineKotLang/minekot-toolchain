package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Flags obvious thread and timer APIs where coroutines should be preferred.
 */
class CoroutinePreferenceRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "CoroutinePreference",
        severity = Severity.Style,
        description = "MineKot uses coroutines instead of direct thread and timer APIs.",
        debt = Debt.TEN_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callName = expression.calleeExpression?.text ?: return
        if (
            expression.isBlockedConstructorCall(callName) ||
            expression.isBlockedQualifiedCall() ||
            callName in blockedSchedulerCalls &&
            !expression.bridgesIntoCoroutineScope() &&
            !expression.isPlatformSchedulerBridge()
        ) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Use coroutines instead of direct thread or timer APIs.",
                ),
            )
        }
    }

    private fun KtCallExpression.bridgesIntoCoroutineScope(): Boolean =
        text.contains("scope.launch") || text.contains("scope.async")

    private fun KtCallExpression.isPlatformSchedulerBridge(): Boolean =
        parents.filterIsInstance<KtNamedFunction>().firstOrNull()?.let { function ->
            function.name in platformSchedulerBridgeNames
        } == true

    private fun KtCallExpression.isBlockedQualifiedCall(): Boolean {
        val qualifiedExpression = parent as? KtDotQualifiedExpression ?: return false
        val callee = calleeExpression?.text ?: return false
        val qualifiedCall = "${qualifiedExpression.receiverExpression.text}.${callee}"
        return qualifiedCall in blockedQualifiedCalls
    }

    private fun KtCallExpression.isBlockedConstructorCall(callName: String): Boolean {
        if (callName in blockedConstructors && parent !is KtDotQualifiedExpression) {
            return true
        }
        val qualifiedExpression = parent as? KtDotQualifiedExpression ?: return false
        val qualifiedCall = "${qualifiedExpression.receiverExpression.text}.${callName}"
        return qualifiedCall in blockedQualifiedConstructors
    }

    private companion object {
        private val blockedConstructors: Set<String> = setOf("Thread", "Timer")

        private val blockedQualifiedConstructors: Set<String> = setOf(
            "java.lang.Thread",
            "java.util.Timer",
        )

        private val blockedQualifiedCalls: Set<String> = setOf(
            "Thread.sleep",
            "java.lang.Thread.sleep",
            "CompletableFuture.runAsync",
            "CompletableFuture.supplyAsync",
            "Executors.newCachedThreadPool",
            "Executors.newFixedThreadPool",
            "Executors.newScheduledThreadPool",
            "Executors.newSingleThreadExecutor",
            "java.util.concurrent.CompletableFuture.runAsync",
            "java.util.concurrent.CompletableFuture.supplyAsync",
            "java.util.concurrent.Executors.newCachedThreadPool",
            "java.util.concurrent.Executors.newFixedThreadPool",
            "java.util.concurrent.Executors.newScheduledThreadPool",
            "java.util.concurrent.Executors.newSingleThreadExecutor",
        )
        private val blockedSchedulerCalls: Set<String> = setOf(
            "runTask",
            "runTaskAsynchronously",
            "runTaskLater",
            "runTaskLaterAsynchronously",
            "runTaskTimer",
            "runTaskTimerAsynchronously",
            "scheduleAsyncDelayedTask",
            "scheduleSyncDelayedTask",
        )
        private val platformSchedulerBridgeNames: Set<String> = setOf(
            "scheduleDelayedTask",
            "scheduleRepeatingTask",
        )
    }
}
