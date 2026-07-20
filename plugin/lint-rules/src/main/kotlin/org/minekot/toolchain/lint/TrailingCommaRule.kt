package org.minekot.toolchain.lint

import com.intellij.psi.PsiComment
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.internal.AutoCorrectable
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Requires a trailing comma in every supported multiline comma-separated construct.
 */
@AutoCorrectable(since = "2.0.0")
class TrailingCommaRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "TrailingComma",
        severity = Severity.Style,
        description = "MineKot requires trailing commas in multiline lists.",
        debt = Debt.FIVE_MINS,
    )
    private val edits = MineKotTextEdits()

    override val ruleName: RuleName get() = RuleName(issue.id)

    override fun preVisit(root: KtFile) {
        edits.clear()
    }

    override fun postVisit(root: KtFile) {
        edits.applyTo(root, autoCorrect)
    }

    override fun visitParameterList(list: KtParameterList) {
        super.visitParameterList(list)
        if (list.ownerFunction is KtFunctionLiteral) {
            return
        }
        list.reportMissingTrailingComma(list.parameters.lastOrNull())
    }

    override fun visitValueArgumentList(list: KtValueArgumentList) {
        super.visitValueArgumentList(list)
        list.reportMissingTrailingComma(list.arguments.lastOrNull())
    }

    override fun visitTypeArgumentList(list: KtTypeArgumentList) {
        super.visitTypeArgumentList(list)
        list.reportMissingTrailingComma(list.arguments.lastOrNull())
    }

    override fun visitTypeParameterList(list: KtTypeParameterList) {
        super.visitTypeParameterList(list)
        list.reportMissingTrailingComma(list.parameters.lastOrNull())
    }

    override fun visitCollectionLiteralExpression(expression: KtCollectionLiteralExpression) {
        super.visitCollectionLiteralExpression(expression)
        expression.reportMissingTrailingComma(expression.innerExpressions.lastOrNull())
    }

    override fun visitDestructuringDeclaration(multiDeclaration: KtDestructuringDeclaration) {
        super.visitDestructuringDeclaration(multiDeclaration)
        if ('\n' !in multiDeclaration.text.substringBefore('=')) {
            return
        }
        multiDeclaration.reportMissingTrailingComma(multiDeclaration.entries.lastOrNull())
    }

    override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
        super.visitArrayAccessExpression(expression)
        val leftBracket = expression.leftBracket ?: return
        val rightBracket = expression.rightBracket ?: return
        val source = expression.containingKtFile.text
        if ('\n' !in source.substring(leftBracket.textRange.startOffset, rightBracket.textRange.endOffset)) {
            return
        }
        expression.reportMissingTrailingComma(expression.indexExpressions.lastOrNull())
    }

    override fun visitContextReceiverList(contextReceiverList: KtContextReceiverList) {
        super.visitContextReceiverList(contextReceiverList)
        val lastReceiver = contextReceiverList.contextParameters.lastOrNull()
            ?: contextReceiverList.contextReceivers().lastOrNull()
        contextReceiverList.reportMissingTrailingComma(lastReceiver)
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        super.visitLambdaExpression(lambdaExpression)
        val functionLiteral = lambdaExpression.functionLiteral
        val lastParameter = functionLiteral.valueParameters.lastOrNull() ?: return
        val arrow = functionLiteral.arrow ?: return
        lambdaExpression.reportMissingTrailingCommaBefore(
            lastParameter,
            lambdaExpression.textRange.startOffset,
            arrow.textRange.startOffset,
        )
    }

    override fun visitWhenEntry(jetWhenEntry: KtWhenEntry) {
        super.visitWhenEntry(jetWhenEntry)
        val lastCondition = jetWhenEntry.conditions.lastOrNull() ?: return
        val arrow = jetWhenEntry.arrow ?: return
        jetWhenEntry.reportMissingTrailingCommaBefore(
            lastCondition,
            jetWhenEntry.textRange.startOffset,
            arrow.textRange.startOffset,
        )
    }

    private fun KtElement.reportMissingTrailingComma(lastElement: KtElement?) {
        if (lastElement == null || !text.contains('\n')) {
            return
        }
        if (hasTrailingCommaIgnoringComments()) {
            return
        }
        val source = containingKtFile.text
        val firstCommentOffset = lastElement.collectDescendantsOfType<PsiComment>()
            .minOfOrNull { comment -> comment.textRange.startOffset }
        val insertionBoundary = firstCommentOffset ?: lastElement.textRange.endOffset
        val insertionOffset = source.substring(0, insertionBoundary).trimEnd().length
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(this),
                message = "Add a trailing comma to this multiline list.",
            ),
        )
        edits.replace(insertionOffset, insertionOffset, ",")
    }

    private fun KtElement.hasTrailingCommaIgnoringComments(): Boolean {
        val listStartOffset = textRange.startOffset
        val sourceWithoutComments = text.toCharArray()
        collectDescendantsOfType<PsiComment>().forEach { comment ->
            val startOffset = comment.textRange.startOffset - listStartOffset
            val endOffset = comment.textRange.endOffset - listStartOffset
            (startOffset until endOffset).forEach { offset ->
                sourceWithoutComments[offset] = ' '
            }
        }
        return sourceWithoutComments.concatToString()
            .dropLast(1)
            .trimEnd()
            .endsWith(',')
    }

    private fun KtElement.reportMissingTrailingCommaBefore(
        lastElement: KtElement,
        headerStartOffset: Int,
        boundaryOffset: Int,
    ) {
        val source = containingKtFile.text
        if ('\n' !in source.substring(headerStartOffset, boundaryOffset)) {
            return
        }
        val between = source.substring(lastElement.textRange.endOffset, boundaryOffset)
        val withoutComments = between
            .replace(Regex("//[^\n]*"), "")
            .replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
        if (withoutComments.trim().startsWith(',')) {
            return
        }
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(this),
                message = "Add a trailing comma before this multiline delimiter.",
            ),
        )
        edits.replace(lastElement.textRange.endOffset, lastElement.textRange.endOffset, ",")
    }
}
