package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Flags obvious text patterns that should use MiniMessage.
 */
class MiniMessageTextRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "MiniMessageText",
        severity = Severity.Style,
        description = "MineKot user-facing text uses MiniMessage instead of legacy color codes or concatenation.",
        debt = Debt.TEN_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

    private val legacyColorPattern = Regex("(?i)(§[0-9a-fk-or]|(?<![\\p{Alnum}?=&])&[0-9a-fk-or])")

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
        super.visitStringTemplateExpression(expression)
        val text = expression.text
        if (legacyColorPattern.containsMatchIn(text)) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Use MiniMessage tags instead of legacy color codes.",
                ),
            )
        } else if (expression.isDirectUserFacingArgument() && !expression.isInsideMiniMessageCall()) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Route user-facing text through MiniMessage before calling this API.",
                ),
            )
        }
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        super.visitBinaryExpression(expression)
        if (expression.isInKspDiagnosticSource()) {
            return
        }
        if (
            expression.operationToken == KtTokens.PLUS &&
            expression.hasStringTemplateOperand() &&
            !expression.isNestedStringConcatenation() &&
            expression.isUserFacingText()
        ) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(expression),
                    message = "Avoid string concatenation for user-facing text; prefer MiniMessage templates.",
                ),
            )
        }
    }

    private fun KtBinaryExpression.hasStringTemplateOperand(): Boolean =
        left.containsStringTemplate() || right.containsStringTemplate()

    private fun KtExpression.isInKspDiagnosticSource(): Boolean =
        containingKtFile.importDirectives.any { directive ->
            val path = directive.importPath?.pathStr
            path == "com.google.devtools.ksp.processing.KSPLogger" ||
                    directive.text == "import com.google.devtools.ksp.processing.*"
        }

    private fun KtBinaryExpression.isNestedStringConcatenation(): Boolean {
        var container = parent
        while (container is KtParenthesizedExpression) {
            container = container.parent
        }
        return container is KtBinaryExpression &&
                container.operationToken == KtTokens.PLUS &&
                container.hasStringTemplateOperand()
    }

    private fun KtExpression?.containsStringTemplate(): Boolean =
        when (this) {
            is KtStringTemplateExpression -> true
            is KtBinaryExpression -> hasStringTemplateOperand()
            else -> false
        }

    private fun KtBinaryExpression.isUserFacingText(): Boolean {
        val declarationName = parents.firstNotNullOfOrNull { parent ->
            when (parent) {
                is KtNamedFunction -> parent.name
                is KtProperty -> parent.name
                else -> null
            }
        }
        if (declarationName?.containsUserFacingName() == true) {
            return true
        }
        val valueArgument = parents.filterIsInstance<KtValueArgument>().firstOrNull() ?: return false
        val call = valueArgument.parent?.parent as? KtCallExpression ?: return false
        return call.isUserFacingCall()
    }

    private fun KtStringTemplateExpression.isDirectUserFacingArgument(): Boolean {
        val valueArgument = parent as? KtValueArgument ?: return false
        val call = valueArgument.parent?.parent as? KtCallExpression ?: return false
        return call.isUserFacingCall()
    }

    private fun KtStringTemplateExpression.isInsideMiniMessageCall(): Boolean =
        parents.filterIsInstance<KtCallExpression>().any { call ->
            call.calleeExpression?.text in miniMessageCalls
        }

    private fun KtCallExpression.isUserFacingCall(): Boolean {
        val callName = calleeExpression?.text ?: return false
        if (callName in directUserFacingCalls) {
            return true
        }
        if (callName !in loggerLevelCalls) {
            return false
        }
        val qualifiedCall = parent as? KtDotQualifiedExpression ?: return false
        if (isInKspDiagnosticSource()) {
            return false
        }
        return qualifiedCall.receiverExpression.text.lowercase().contains("log")
    }

    private fun String.containsUserFacingName(): Boolean {
        val lowercaseName = lowercase()
        return userFacingNameParts.any(lowercaseName::contains)
    }

    private companion object {
        private val miniMessageCalls: Set<String> = setOf(
            "deserialize",
            "mineKotMiniMessage",
            "mineKotMiniMessageResult",
            "toMineKotMiniMessageComponent",
        )
        private val directUserFacingCalls: Set<String> = setOf(
            "broadcast",
            "notify",
            "print",
            "println",
            "sendMessage",
        )
        private val loggerLevelCalls: Set<String> = setOf(
            "debug",
            "error",
            "info",
            "warn",
        )
        private val userFacingNameParts: Set<String> = setOf(
            "caption",
            "description",
            "label",
            "log",
            "message",
            "subtitle",
            "text",
            "title",
        )
    }
}
