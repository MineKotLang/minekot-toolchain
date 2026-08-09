package org.minekot.toolchain.lint.formatting

import dev.detekt.api.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.minekot.toolchain.lint.core.*
import org.minekot.toolchain.lint.core.Issue
import org.minekot.toolchain.lint.core.Severity

/**
 * Reports raw rich text only after resolving an Adventure or MineKot text flow.
 */
class MiniMessageTextRule(config: Config) :
    Rule(config, "MineKot codestyle rule."),
    RequiresAnalysisApi {
    private val issue: Issue = Issue(
        id = "MiniMessageText",
        severity = Severity.Style,
        description = "MineKot rich user-facing Adventure text uses MiniMessage templates.",
        debt = Debt.TEN_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
        super.visitStringTemplateExpression(expression)
        if (expression.isPartOfStringConcatenation()) {
            return
        }
        val callPath = expression.resolvedCallPath()
        val miniMessageInput = callPath.firstOrNull() in miniMessageInputCalls
        val rawAdventureFlow =
            callPath.firstOrNull() in rawComponentCalls && callPath.any(adventureTextSinkCalls::contains)
        if (!miniMessageInput && !rawAdventureFlow) {
            return
        }
        val message = if (legacyColorPattern.containsMatchIn(expression.text)) {
            "Use MiniMessage tags instead of legacy color codes on Adventure text surfaces."
        } else if (rawAdventureFlow) {
            "Use a MineKot MiniMessage template instead of raw Component text on this Adventure surface."
        } else {
            return
        }
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = message,
            ),
        )
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        super.visitBinaryExpression(expression)
        if (
            expression.operationToken != KtTokens.PLUS ||
            expression.isNestedStringConcatenation() ||
            !expression.hasStringTemplateOperand()
        ) {
            return
        }
        val callPath = expression.resolvedCallPath()
        if (callPath.firstOrNull() !in rawComponentCalls || callPath.none(adventureTextSinkCalls::contains)) {
            return
        }
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "Use MiniMessage placeholders instead of concatenation on Adventure text surfaces.",
            ),
        )
    }

    private fun KtElement.resolvedCallPath(): List<String> =
        parents.filterIsInstance<KtCallExpression>()
            .mapNotNull { call -> call.resolveMineKotCall()?.callableId }
            .toList()

    private fun KtStringTemplateExpression.isPartOfStringConcatenation(): Boolean =
        parents.takeWhile { parent -> parent !is KtCallExpression }.any { parent ->
            parent is KtBinaryExpression && parent.operationToken == KtTokens.PLUS
        }

    private fun KtBinaryExpression.isNestedStringConcatenation(): Boolean {
        var container = parent
        while (container is KtParenthesizedExpression) {
            container = container.parent
        }
        return container is KtBinaryExpression && container.operationToken == KtTokens.PLUS
    }

    private fun KtBinaryExpression.hasStringTemplateOperand(): Boolean =
        left.containsStringTemplate() || right.containsStringTemplate()

    private fun KtExpression?.containsStringTemplate(): Boolean =
        when (this) {
            is KtStringTemplateExpression -> true
            is KtBinaryExpression -> hasStringTemplateOperand()
            else -> false
        }

    private companion object {
        private val legacyColorPattern: Regex = Regex("(?i)(§[0-9a-fk-or]|(?<![\\p{Alnum}?=&])&[0-9a-fk-or])")
        private val miniMessageInputCalls: Set<String> = setOf(
            "net.kyori.adventure.text.minimessage.MiniMessage.deserialize",
            "org.minekot.adventure.minimessage.mineKotMiniMessage",
            "org.minekot.adventure.minimessage.mineKotMiniMessageResult",
            "org.minekot.adventure.minimessage.toMineKotMiniMessageComponent",
        )
        private val rawComponentCalls: Set<String> = setOf(
            "net.kyori.adventure.text.Component.text",
        )
        private val adventureTextSinkCalls: Set<String> = setOf(
            "net.kyori.adventure.audience.Audience.sendActionBar",
            "net.kyori.adventure.audience.Audience.sendMessage",
            "net.kyori.adventure.audience.Audience.sendPlayerListFooter",
            "net.kyori.adventure.audience.Audience.sendPlayerListHeader",
            "net.kyori.adventure.audience.Audience.sendPlayerListHeaderAndFooter",
            "net.kyori.adventure.audience.Audience.showTitle",
        )
    }
}
