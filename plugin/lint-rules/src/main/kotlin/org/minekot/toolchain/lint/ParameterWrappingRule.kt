package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.internal.AutoCorrectable
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtValueArgumentList

/**
 * Requires every item and delimiter in multiline parameter and argument lists to use separate lines.
 */
@AutoCorrectable(since = "2.0.0")
class ParameterWrappingRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "ParameterWrapping",
        severity = Severity.Style,
        description = "MineKot wraps every item in multiline parameter and argument lists.",
        debt = Debt.FIVE_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)
    private val edits = MineKotTextEdits()

    override fun preVisit(root: KtFile) {
        edits.clear()
    }

    override fun postVisit(root: KtFile) {
        edits.applyTo(root, autoCorrect)
    }

    override fun visitParameterList(list: KtParameterList) {
        super.visitParameterList(list)
        list.reportInvalidWrapping(list.parameters)
    }

    override fun visitValueArgumentList(list: KtValueArgumentList) {
        super.visitValueArgumentList(list)
        list.reportInvalidWrapping(list.arguments)
    }

    private fun KtElement.reportInvalidWrapping(elements: List<KtElement>) {
        if (elements.isEmpty()) {
            return
        }
        if (!text.contains('\n')) {
            return
        }
        val listStart = textRange.startOffset
        val source = containingKtFile.text
        val hasNewlineAfterOpening = source
            .substring(listStart + 1, elements.first().textRange.startOffset)
            .contains('\n')
        val everyElementStartsOnNewLine = elements.all { element ->
            val previousDelimiter = if (element == elements.first()) {
                listStart + 1
            } else {
                elements[elements.indexOf(element) - 1].textRange.endOffset
            }
            source.substring(previousDelimiter, element.textRange.startOffset).contains('\n')
        }
        val hasNewlineBeforeClosing = source.substring(elements.last().textRange.endOffset, textRange.endOffset - 1)
            .contains('\n')
        if (hasNewlineAfterOpening && everyElementStartsOnNewLine && hasNewlineBeforeClosing) {
            return
        }
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(this),
                message = "Put every item and the closing delimiter of this multiline list on separate lines.",
            ),
        )
        scheduleSimpleWrapping(elements)
    }

    private fun KtElement.scheduleSimpleWrapping(elements: List<KtElement>) {
        if (text.contains("//") || text.contains("/*") || elements.any { element -> '\n' in element.text }) {
            return
        }
        val source = containingKtFile.text
        val lineStart = source.lastIndexOf('\n', textRange.startOffset - 1) + 1
        val baseIndent = source.substring(lineStart, textRange.startOffset)
            .takeWhile { character -> character == ' ' || character == '\t' }
        val itemIndent = baseIndent + continuationIndent
        val replacement = buildString {
            append("(\n")
            append(itemIndent)
            append(elements.joinToString(separator = ",\n${itemIndent}") { element -> element.text })
            append(',')
            append('\n')
            append(baseIndent)
            append(')')
        }
        edits.replace(textRange.startOffset, textRange.endOffset, replacement)
    }

    private companion object {
        private const val continuationIndent: String = "    "
    }
}
