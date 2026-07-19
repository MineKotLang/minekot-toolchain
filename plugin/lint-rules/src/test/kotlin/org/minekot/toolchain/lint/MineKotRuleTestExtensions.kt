package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.modifiedText
import dev.detekt.test.utils.compileContentForTest
import org.jetbrains.kotlin.config.*

internal fun Rule.lint(
    content: String,
    filename: String = "Test.kt",
): List<Finding> =
    visitFile(
        root = compileContentForTest(content, filename),
        languageVersionSettings = mineKotLanguageVersionSettings(),
    )

internal fun Rule.lintAndCorrect(
    content: String,
    filename: String = "Test.kt",
): MineKotLintResult {
    val root = compileContentForTest(content, filename)
    val findings = visitFile(
        root = root,
        languageVersionSettings = mineKotLanguageVersionSettings(),
    )
    return MineKotLintResult(findings, root.modifiedText ?: root.text)
}

internal data class MineKotLintResult(
    val findings: List<Finding>,
    val correctedSource: String,
)

internal val mineKotAutoCorrectConfig: Config = object : Config {
    override val parent: Config = this

    override fun subConfig(key: String): Config = this

    override fun subConfigKeys(): Set<String> = emptySet()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> valueOrNull(key: String): T? =
        when (key) {
            Config.ACTIVE_KEY,
            Config.AUTO_CORRECT_KEY,
                -> true as T

            else -> null
        }
}

private fun mineKotLanguageVersionSettings(): LanguageVersionSettingsImpl =
    LanguageVersionSettingsImpl(
        languageVersion = LanguageVersion.LATEST_STABLE,
        apiVersion = ApiVersion.LATEST_STABLE,
        analysisFlags = mapOf(AnalysisFlags.explicitApiMode to ExplicitApiMode.DISABLED),
        specificFeatures = emptyMap(),
    )
