package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Flags non-trivial numeric literals that should be named.
 */
class MagicNumberRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "MagicNumber",
        severity = Severity.Style,
        description = "MineKot avoids unnamed numeric literals.",
        debt = Debt.FIVE_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

    override fun visitConstantExpression(expression: KtConstantExpression) {
        super.visitConstantExpression(expression)
        if (!expression.isMagicNumber()) {
            return
        }
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message = "Replace this magic number with a named constant.",
            ),
        )
    }

    private fun KtConstantExpression.isMagicNumber(): Boolean =
        text.toMineKotNumberOrNull() != null &&
                text.toMineKotNumberOrNull() !in allowedNumbers &&
                parents.filterIsInstance<KtAnnotationEntry>().firstOrNull() == null &&
                parents.filterIsInstance<KtPackageDirective>().firstOrNull() == null &&
                !isNamedPropertyInitializer() &&
                parents.filterIsInstance<KtProperty>().firstOrNull()?.isConstant() != true

    private fun KtConstantExpression.isNamedPropertyInitializer(): Boolean =
        (parent as? KtProperty)?.initializer == this

    private fun KtProperty.isConstant(): Boolean =
        hasModifier(KtTokens.CONST_KEYWORD) || name?.all { it.isUpperCase() || it == '_' || it.isDigit() } == true

    private fun String.toMineKotNumberOrNull(): Double? {
        val cleaned = trim()
            .replace("_", "")
            .lowercase()
            .removeSuffix("ul")
            .removeSuffix("u")
            .removeSuffix("l")
            .removeSuffix("f")
        return when {
            cleaned.startsWith("0x") -> cleaned.removePrefix("0x").toLongOrNull(16)?.toDouble()
            cleaned.startsWith("0b") -> cleaned.removePrefix("0b").toLongOrNull(2)?.toDouble()
            else -> cleaned.toDoubleOrNull()
        }
    }

    private companion object {
        private val allowedNumbers: Set<Double> = setOf(0.0, 1.0, 2.0)
    }
}
