package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Rule

/**
 * Metadata for a MineKot Detekt rule.
 *
 * @property id Rule id.
 * @property defaultActive Whether the bundled config enables this rule.
 * @property severity Rule severity name.
 * @property falsePositiveRisk False-positive risk description.
 * @property codestyleSection MineKot codestyle section implemented by this rule.
 * @property factory Rule factory.
 */
data class MineKotRuleDescriptor(
    val id: String,
    val defaultActive: Boolean,
    val severity: String,
    val falsePositiveRisk: String,
    val codestyleSection: String,
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
        factory = ::ForbiddenTryCatchRule,
    ),
    MineKotRuleDescriptor(
        id = "StringTemplateBraces",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "4.6 String templates",
        factory = ::StringTemplateBracesRule,
    ),
    MineKotRuleDescriptor(
        id = "MiniMessageText",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "7.5 Text and localization",
        factory = ::MiniMessageTextRule,
    ),
    MineKotRuleDescriptor(
        id = "MissingKDoc",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "8 Documentation and comments",
        factory = ::MissingKDocRule,
    ),
    MineKotRuleDescriptor(
        id = "CoroutinePreference",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "6.3 Concurrency",
        factory = ::CoroutinePreferenceRule,
    ),
    MineKotRuleDescriptor(
        id = "MagicNumber",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "7.2 Zero hardcoding",
        factory = ::MagicNumberRule,
    ),
    MineKotRuleDescriptor(
        id = "ResultHandling",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "6.1 Error handling",
        factory = ::ResultHandlingRule,
    ),
    MineKotRuleDescriptor(
        id = "KotlinxPreference",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "5.1 Library preference and 5.2 Kotlinx mandate",
        factory = ::KotlinxPreferenceRule,
    ),
    MineKotRuleDescriptor(
        id = "ResolvedApiPreference",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "5.1 Library preference, 5.2 Kotlinx mandate, and 6.3 Concurrency",
        factory = ::ResolvedApiPreferenceRule,
    ),
    MineKotRuleDescriptor(
        id = "TrailingComma",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "4.4 Trailing commas",
        factory = ::TrailingCommaRule,
    ),
    MineKotRuleDescriptor(
        id = "LineWrapping",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "4.1 Column limit and 4.3 Indentation",
        factory = ::LineWrappingRule,
    ),
    MineKotRuleDescriptor(
        id = "ForEachPreference",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "6.5 forEach loops",
        factory = ::ForEachPreferenceRule,
    ),
    MineKotRuleDescriptor(
        id = "GradleDslConventions",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "Gradle sections 3, 5, 6, and 7",
        factory = ::GradleDslConventionsRule,
    ),
    MineKotRuleDescriptor(
        id = "ImportPolicy",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "3.1 Imports",
        factory = ::ImportPolicyRule,
    ),
    MineKotRuleDescriptor(
        id = "SourceFilePolicy",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "2 Source file basics and 4.2 Formatter control",
        factory = ::SourceFilePolicyRule,
    ),
    MineKotRuleDescriptor(
        id = "CommentFormatting",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "8.4 Comment formatting",
        factory = ::CommentFormattingRule,
    ),
    MineKotRuleDescriptor(
        id = "ExplicitScopeInNestedScope",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "6.2 Explicit scope resolution",
        factory = ::ExplicitScopeInNestedScopeRule,
    ),
)
