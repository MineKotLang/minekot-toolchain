package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Rule

/** Lifecycle disposition for a MineKot rule. */
enum class MineKotRuleDisposition {
    KEEP,
    REWRITE,
    DEPRECATED,
}

/** Safety classification for automatic correction. */
enum class MineKotCorrectionMode {
    SAFE,
    DISABLED,
    REQUIRES_SEMANTIC_PROOF,
}

/**
 * Metadata for a MineKot Detekt rule.
 *
 * @property id Rule id.
 * @property defaultActive Whether the bundled config enables this rule.
 * @property severity Rule severity name.
 * @property falsePositiveRisk False-positive risk description.
 * @property codestyleSection MineKot codestyle section implemented by this rule.
 * @property disposition Current rule lifecycle decision.
 * @property reportsByDefault Whether the bundled configuration reports findings.
 * @property correctionMode Automatic-correction safety classification.
 * @property replacementId Replacement rule for deprecated entries.
 * @property requiresAnalysisApi Whether the rule runs only with compiler analysis.
 * @property factory Rule factory.
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
 * Data-driven rule registry used by provider, docs, and tests.
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
