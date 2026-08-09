package org.minekot.toolchain.lint.core

import dev.detekt.api.Config
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement

/**
 * Represents a resolved call within the MineKot context, providing its fully qualified callable ID.
 * This is typically used after AST analysis to identify specific function or constructor invocations.
 */
internal data class ResolvedMineKotCall(
    val callableId: String,
)

/**
 * Resolves a [KtCallExpression] to its corresponding [ResolvedMineKotCall] within the MineKot analysis context.
 *
 * This function uses the Kotlin Analysis API to determine the fully qualified callable ID of the
 * invoked function or constructor. It returns `null` if the call cannot be resolved or if it's
 * not a function or constructor call.
 *
 * @receiver The [KtCallExpression] to resolve.
 * @return A [ResolvedMineKotCall] containing the fully qualified callable ID, or `null` if resolution fails.
 */
internal fun KtCallExpression.resolveMineKotCall(): ResolvedMineKotCall? =
    analyze(this) {
        val symbol = resolveToCall()?.singleFunctionCallOrNull()?.symbol ?: return@analyze null
        val callableId = if (symbol is KaConstructorSymbol) {
            symbol.containingClassId?.asSingleFqName()?.asString()?.plus(".<init>")
        } else {
            symbol.callableId?.asSingleFqName()?.asString()
        } ?: return@analyze null
        ResolvedMineKotCall(callableId)
    }

/**
 * Checks if the given [KtElement] is located within a package considered a "MineKot native boundary".
 *
 * Native boundary packages are typically those that interact directly with underlying platform
 * specifics or provide core MineKot runtime functionality, and might have special linting
 * or compilation rules applied to them. The prefixes are configured via the provided [config].
 *
 * @receiver The [KtElement] to check.
 * @param config The Detekt [Config] object containing the `boundaryPackagePrefixes`.
 * @return `true` if the element's package matches or starts with one of the configured prefixes,
 * `false` otherwise.
 */
internal fun KtElement.isInsideMineKotNativeBoundary(config: Config): Boolean {
    val packageName = containingKtFile.packageFqName.asString()
    val prefixes = config.valueOrDefault(
        "boundaryPackagePrefixes",
        defaultMineKotBoundaryPackagePrefixes,
    )
    return prefixes.any { prefix -> packageName == prefix || packageName.startsWith("${prefix}.") }
}

private val defaultMineKotBoundaryPackagePrefixes: List<String> = listOf(
    "org.minekot.kotlin",
    "org.minekot.platform",
)
