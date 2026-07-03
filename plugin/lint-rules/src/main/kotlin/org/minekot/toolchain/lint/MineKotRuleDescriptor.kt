package org.minekot.toolchain.lint

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Rule

/**
 * Metadata for a MineKot Detekt rule.
 *
 * @property id Rule id.
 * @property defaultActive Whether the bundled config enables this rule.
 * @property severity Rule severity name.
 * @property falsePositiveRisk False-positive risk description.
 * @property codestyleSection MineKot codestyle section implemented by this rule.
 * @property failingFixture Failing fixture name.
 * @property cleanFixture Clean fixture name.
 * @property factory Rule factory.
 */
data class MineKotRuleDescriptor(
    val id: String,
    val defaultActive: Boolean,
    val severity: String,
    val falsePositiveRisk: String,
    val codestyleSection: String,
    val failingFixture: String,
    val cleanFixture: String,
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
        failingFixture = "forbidden-try-catch.kt",
        cleanFixture = "run-catching.kt",
        factory = ::ForbiddenTryCatchRule,
    ),
    MineKotRuleDescriptor(
        id = "StringTemplateBraces",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "4.6 String templates",
        failingFixture = "simple-string-template.kt",
        cleanFixture = "braced-string-template.kt",
        factory = ::StringTemplateBracesRule,
    ),
    MineKotRuleDescriptor(
        id = "MiniMessageText",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "7.5 Text and localization",
        failingFixture = "legacy-text.kt",
        cleanFixture = "minimessage-text.kt",
        factory = ::MiniMessageTextRule,
    ),
    MineKotRuleDescriptor(
        id = "MissingKDoc",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "8 Documentation and comments",
        failingFixture = "missing-kdoc.kt",
        cleanFixture = "documented-api.kt",
        factory = ::MissingKDocRule,
    ),
    MineKotRuleDescriptor(
        id = "CoroutinePreference",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "low",
        codestyleSection = "6.3 Concurrency",
        failingFixture = "threading.kt",
        cleanFixture = "coroutines.kt",
        factory = ::CoroutinePreferenceRule,
    ),
    MineKotRuleDescriptor(
        id = "MagicNumber",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "7.2 Zero hardcoding",
        failingFixture = "magic-number.kt",
        cleanFixture = "named-constant.kt",
        factory = ::MagicNumberRule,
    ),
    MineKotRuleDescriptor(
        id = "ResultHandling",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "6.1 Error handling",
        failingFixture = "ignored-result.kt",
        cleanFixture = "handled-result.kt",
        factory = ::ResultHandlingRule,
    ),
    MineKotRuleDescriptor(
        id = "KotlinxPreference",
        defaultActive = true,
        severity = "Style",
        falsePositiveRisk = "medium",
        codestyleSection = "5.1 Library preference and 5.2 Kotlinx mandate",
        failingFixture = "java-api.kt",
        cleanFixture = "kotlinx-api.kt",
        factory = ::KotlinxPreferenceRule,
    ),
)
