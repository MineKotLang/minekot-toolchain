package org.minekot.toolchain.lint.core

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.minekot.toolchain.lint.formatting.*
import org.minekot.toolchain.lint.language.CoroutinePreferenceRule
import org.minekot.toolchain.lint.language.ForEachPreferenceRule
import org.minekot.toolchain.lint.language.KotlinxPreferenceRule
import org.minekot.toolchain.lint.language.ResolvedApiPreferenceRule
import org.minekot.toolchain.lint.practices.*
import org.minekot.toolchain.lint.structure.GradleDslConventionsRule
import org.minekot.toolchain.lint.structure.ImportPolicyRule
import org.minekot.toolchain.lint.structure.SourceFilePolicyRule

/**
 * Defines the lifecycle disposition of a MineKot lint rule, indicating its current status
 * and future maintenance plans.
 *
 * This enum helps in managing the evolution of lint rules, providing clear signals
 * about their stability and whether they are recommended for use.
 */
enum class MineKotRuleDisposition {
    /**
     * The rule is actively maintained, stable, and recommended for use.
     *
     * Rules with this disposition are considered part of the core linting experience
     * and are expected to remain stable across versions.
     */
    KEEP,

    /**
     * The rule is currently active but is slated for a potential rewrite or significant
     * improvement in a future release.
     *
     * While functional, its implementation or scope might change, potentially leading
     * to breaking changes in how it's configured or how issues are reported.
     */
    REWRITE,

    /**
     * The rule is deprecated and no longer recommended for use.
     *
     * Deprecated rules might be removed in future versions or have been superseded
     * by a more robust or semantically correct alternative. Users should migrate
     * to the suggested replacement if available.
     */
    DEPRECATED,
}

/**
 * Specifies the behavior and safety level of a lint rule's autocorrection mechanism.
 *
 * This helps users understand the implications of applying automatic fixes suggested
 * by the lint tool.
 */
enum class MineKotCorrectionMode {
    /**
     * Autocorrection for this rule is considered safe and will not alter the
     * semantic meaning or behavior of the code.
     *
     * These corrections typically involve formatting, style, or minor syntactic
     * changes that are guaranteed to be non-breaking.
     */
    SAFE,

    /**
     * Autocorrection is not available or explicitly disabled for this rule.
     *
     * This might be due to the complexity of the fix, the potential for semantic
     * changes, or simply because no automatic fix has been implemented.
     */
    DISABLED,

    /**
     * Autocorrection for this rule might introduce semantic changes or alter
     * the behavior of the code, requiring careful review and "semantic proof"
     * from the developer.
     *
     * Applying such corrections without understanding their implications could
     * lead to subtle bugs.
     */
    REQUIRES_SEMANTIC_PROOF,
}

/**
 * Describes a single MineKot lint rule, providing metadata for its configuration,
 * reporting, and lifecycle management within the Detekt plugin.
 *
 * @property id A unique identifier for the lint rule, used for configuration and reporting.
 * @property defaultActive Whether the rule is enabled by default.
 * @property severity Impact level (e.g., "Style", "Warning", "Error").
 * @property falsePositiveRisk Likelihood of incorrectly flagging valid code ("low", "medium", "high").
 * @property codestyleSection Reference to the relevant MineKot Code Style Guide section(s).
 * @property disposition Lifecycle status: [MineKotRuleDisposition.KEEP], [MineKotRuleDisposition.REWRITE],
 *   or [MineKotRuleDisposition.DEPRECATED]. Defaults to [MineKotRuleDisposition.KEEP].
 * @property reportsByDefault Whether the rule reports issues by default (mirrors [defaultActive] unless overridden).
 * @property correctionMode Safety level of autocorrection: [MineKotCorrectionMode.SAFE],
 *   [MineKotCorrectionMode.DISABLED], or [MineKotCorrectionMode.REQUIRES_SEMANTIC_PROOF].
 *   Defaults to [MineKotCorrectionMode.DISABLED].
 * @property replacementId If [MineKotRuleDisposition.DEPRECATED], the [id] of the superseding rule.
 * @property requiresAnalysisApi Whether the rule requires Detekt's Analysis API (for semantic information, slower).
 * @property factory Factory function to create a [Rule] instance from a [Config].
 */
data class MineKotRuleDescriptor(
    val id: String,
    val defaultActive: Boolean,
    val severity: String,
    val falsePositiveRisk: String,
    val codestyleSection: String,
    val disposition: MineKotRuleDisposition = MineKotRuleDisposition.KEEP,
    val reportsByDefault: Boolean = defaultActive,
    val correctionMode: MineKotCorrectionMode = MineKotCorrectionMode.DISABLED,
    val replacementId: String? = null,
    val requiresAnalysisApi: Boolean = false,
    val factory: (Config) -> Rule,
)

/**
 * A comprehensive list of all custom MineKot lint rules available in this plugin.
 *
 * This list serves as the central registry for the Detekt plugin, allowing it to
 * discover, configure, and apply each defined lint rule. Each entry provides
 * detailed metadata about a specific rule, including its ID, default activation
 * status, severity, and how it handles autocorrection.
 *
 * Rules are categorized and described according to the MineKot Code Style Guide.
 * Developers can refer to this list to understand the full suite of custom checks
 * enforced by the MineKot toolchain.
 */
val mineKotRuleDescriptors: List<MineKotRuleDescriptor> = listOf(
    MineKotRuleDescriptor(
        id = "ForbiddenTryCatch",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "6.1 Error handling",
        disposition = MineKotRuleDisposition.REWRITE,
        factory = ::ForbiddenTryCatchRule,
    ),
    MineKotRuleDescriptor(
        id = "StringTemplateBraces",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "4.5 String templates",
        correctionMode = MineKotCorrectionMode.SAFE,
        factory = ::StringTemplateBracesRule,
    ),
    MineKotRuleDescriptor(
        id = "MiniMessageText",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "7.5 Text and localization",
        disposition = MineKotRuleDisposition.REWRITE,
        requiresAnalysisApi = true,
        factory = ::MiniMessageTextRule,
    ),
    MineKotRuleDescriptor(
        id = "MissingKDoc",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "8.1 API documentation and 8.2 Property documentation",
        disposition = MineKotRuleDisposition.REWRITE,
        factory = ::MissingKDocRule,
    ),
    MineKotRuleDescriptor(
        id = "CoroutinePreference",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "6.3 Concurrency",
        disposition = MineKotRuleDisposition.REWRITE,
        requiresAnalysisApi = true,
        factory = ::CoroutinePreferenceRule,
    ),
    MineKotRuleDescriptor(
        id = "MagicNumber",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "7.2 Intent-based value extraction",
        disposition = MineKotRuleDisposition.REWRITE,
        factory = ::MagicNumberRule,
    ),
    MineKotRuleDescriptor(
        id = "ResultHandling",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "6.1 Error handling",
        disposition = MineKotRuleDisposition.REWRITE,
        factory = ::ResultHandlingRule,
    ),
    MineKotRuleDescriptor(
        id = "KotlinxPreference",
        defaultActive = false,
        severity = "Style",
        falsePositiveRisk = "high",
        codestyleSection = "5.1 Library preference and 5.2 Kotlinx and MineKot APIs",
        disposition = MineKotRuleDisposition.DEPRECATED,
        replacementId = "ResolvedApiPreference",
        factory = ::KotlinxPreferenceRule,
    ),
    MineKotRuleDescriptor(
        id = "ResolvedApiPreference",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "5.1 Library preference and 5.2 Kotlinx and MineKot APIs",
        disposition = MineKotRuleDisposition.REWRITE,
        requiresAnalysisApi = true,
        factory = ::ResolvedApiPreferenceRule,
    ),
    MineKotRuleDescriptor(
        id = "TrailingComma",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "4.4 Trailing commas",
        correctionMode = MineKotCorrectionMode.SAFE,
        factory = ::TrailingCommaRule,
    ),
    MineKotRuleDescriptor(
        id = "LineWrapping",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "4.1 Column limit and 4.3 Indentation",
        correctionMode = MineKotCorrectionMode.SAFE,
        factory = ::LineWrappingRule,
    ),
    MineKotRuleDescriptor(
        id = "ForEachPreference",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "6.5 Iteration",
        disposition = MineKotRuleDisposition.REWRITE,
        correctionMode = MineKotCorrectionMode.REQUIRES_SEMANTIC_PROOF,
        factory = ::ForEachPreferenceRule,
    ),
    MineKotRuleDescriptor(
        id = "GradleDslConventions",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "Gradle sections 3, 5, 6, and 7",
        disposition = MineKotRuleDisposition.REWRITE,
        correctionMode = MineKotCorrectionMode.REQUIRES_SEMANTIC_PROOF,
        factory = ::GradleDslConventionsRule,
    ),
    MineKotRuleDescriptor(
        id = "ImportPolicy",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "3.1 Imports",
        disposition = MineKotRuleDisposition.REWRITE,
        correctionMode = MineKotCorrectionMode.REQUIRES_SEMANTIC_PROOF,
        factory = ::ImportPolicyRule,
    ),
    MineKotRuleDescriptor(
        id = "SourceFilePolicy",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "2 Source file basics and 4.2 Formatter control",
        correctionMode = MineKotCorrectionMode.SAFE,
        factory = ::SourceFilePolicyRule,
    ),
    MineKotRuleDescriptor(
        id = "CommentFormatting",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "8.4 Comment formatting",
        correctionMode = MineKotCorrectionMode.SAFE,
        factory = ::CommentFormattingRule,
    ),
    MineKotRuleDescriptor(
        id = "ExplicitScopeInNestedScope",
        defaultActive = false,
        severity = "Style",
        falsePositiveRisk = "high",
        codestyleSection = "6.2 Explicit scope resolution",
        correctionMode = MineKotCorrectionMode.DISABLED,
        requiresAnalysisApi = true,
        factory = ::ExplicitScopeInNestedScopeRule,
    ),
)
