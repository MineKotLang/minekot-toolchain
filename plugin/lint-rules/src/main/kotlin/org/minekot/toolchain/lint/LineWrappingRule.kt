package org.minekot.toolchain.lint

import com.intellij.psi.PsiComment
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.internal.AutoCorrectable
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/** Wraps long calls and conditions without changing string contents. */
@AutoCorrectable(since = "2.0.0")
class LineWrappingRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val edits: MineKotTextEdits = MineKotTextEdits()
    private val issue: Issue = Issue(
        id = "LineWrapping",
        severity = Severity.Style,
        description = "MineKot wraps safely structured lines beyond 120 characters.",
        debt = Debt.FIVE_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

    override fun preVisit(root: KtFile) {
        edits.clear()
    }

    override fun postVisit(root: KtFile) {
        edits.applyTo(root, autoCorrect)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val argumentList = expression.valueArgumentList ?: return
        if (
            argumentList.textContains('\n') || expression.valueArguments.size < 2 ||
            expression.collectDescendantsOfType<KtStringTemplateExpression>().isNotEmpty() ||
            expression.collectDescendantsOfType<PsiComment>().isNotEmpty() ||
            expression.isInsideMineKotFormatterControl() || expression.lineLength() <= MAX_LINE_LENGTH
        ) {
            return
        }
        reportFinding(expression, "Wrap this long call with one argument per line.")
        val indent = expression.lineIndent()
        val argumentIndent = " ".repeat(indent.length + CONTINUATION_INDENT)
        val replacement = expression.valueArguments.joinToString(
            prefix = "(\n",
            separator = "\n",
            postfix = "\n${indent})",
        ) { argument -> "${argumentIndent}${argument.text.removeSuffix(",")}," }
        edits.replace(argumentList.textRange.startOffset, argumentList.textRange.endOffset, replacement)
    }

    override fun visitIfExpression(expression: KtIfExpression) {
        super.visitIfExpression(expression)
        val condition = expression.condition ?: return
        if (
            condition.textContains('\n') || condition.isInsideMineKotFormatterControl() ||
            expression.lineLength() <= MAX_LINE_LENGTH ||
            condition.collectDescendantsOfType<KtStringTemplateExpression>().isNotEmpty()
        ) {
            return
        }
        val parts = condition.text.split(logicalOperatorPattern)
        val operators = logicalOperatorPattern.findAll(condition.text).map { match -> match.value.trim() }.toList()
        if (parts.size < 2 || operators.size != parts.size - 1) {
            return
        }
        reportFinding(expression, "Wrap this long condition at logical operators.")
        val continuation = " ".repeat(expression.lineIndent().length + CONTINUATION_INDENT)
        val replacement = buildString {
            append(parts.first().trim())
            operators.forEachIndexed { index, operator ->
                append(" ${operator}\n${continuation}${parts[index + 1].trim()}")
            }
        }
        edits.replace(condition.textRange.startOffset, condition.textRange.endOffset, replacement)
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
        super.visitPropertyAccessor(accessor)
        val body = accessor.bodyExpression ?: return
        val equalsToken = accessor.equalsToken ?: return
        if (
            accessor.isSetter || body.textContains('\n') || body.containsStringLiteral() ||
            accessor.isInsideMineKotFormatterControl() || accessor.lineLength() <= MAX_LINE_LENGTH
        ) {
            return
        }
        reportFinding(body, "Wrap this long property getter expression.")
        val continuation = " ".repeat(accessor.lineIndent().length + CONTINUATION_INDENT)
        edits.replace(equalsToken.textRange.endOffset, body.textRange.startOffset, "\n${continuation}")
    }

    override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS) {
        super.visitBinaryWithTypeRHSExpression(expression)
        val operation = expression.operationReference
        if (
            expression.textContains('\n') || expression.containsStringLiteral() ||
            expression.isInsideMineKotFormatterControl() || expression.lineLength() <= MAX_LINE_LENGTH
        ) {
            return
        }
        reportFinding(expression, "Wrap this long cast before its type operator.")
        val continuation = " ".repeat(expression.lineIndent().length + CONTINUATION_INDENT)
        edits.replace(operation.textRange.startOffset, operation.textRange.startOffset, "\n${continuation}")
    }

    override fun visitWhenEntry(jetWhenEntry: KtWhenEntry) {
        super.visitWhenEntry(jetWhenEntry)
        val conditions = jetWhenEntry.conditions
        if (
            conditions.size < 2 || jetWhenEntry.textContains('\n') ||
            jetWhenEntry.isInsideMineKotFormatterControl() || jetWhenEntry.lineLength() <= MAX_LINE_LENGTH
        ) {
            return
        }
        reportFinding(
            jetWhenEntry.expression ?: conditions.first(),
            "Wrap this long when entry one condition per line.",
        )
        val indent = jetWhenEntry.lineIndent()
        val continuation = " ".repeat(indent.length + CONTINUATION_INDENT)
        val replacement = conditions.joinToString(",\n${continuation}") { condition -> condition.text }
        edits.replace(
            conditions.first().textRange.startOffset,
            conditions.last().textRange.endOffset,
            replacement,
        )
    }

    private fun KtElement.containsStringLiteral(): Boolean =
        collectDescendantsOfType<KtStringTemplateExpression>().isNotEmpty()

    private fun KtElement.lineLength(): Int {
        val source = containingKtFile.text
        val start = source.lastIndexOf('\n', textRange.startOffset - 1) + 1
        val end = source.indexOf('\n', textRange.endOffset).takeIf { offset -> offset >= 0 } ?: source.length
        return source.substring(start, end).lineSequence().maxOf(String::length)
    }

    private fun KtElement.lineIndent(): String {
        val source = containingKtFile.text
        val start = source.lastIndexOf('\n', textRange.startOffset - 1) + 1
        return source.substring(start, textRange.startOffset).takeWhile(Char::isWhitespace)
    }

    private fun reportFinding(expression: KtElement, message: String) {
        report(CodeSmell(issue, Entity.from(expression), message))
    }

    private companion object {
        private const val MAX_LINE_LENGTH: Int = 120
        private const val CONTINUATION_INDENT: Int = 8
        private val logicalOperatorPattern: Regex = Regex("\\s+(&&|\\|\\|)\\s+")
    }
}
