package org.minekot.toolchain.lint

import dev.detekt.api.Config
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement

/** Stable call data copied out of an Analysis API session. */
internal data class ResolvedMineKotCall(
    val callableId: String,
)

/** Resolves one unambiguous function or constructor call. */
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

/** Whether this source location is an approved native implementation boundary. */
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
