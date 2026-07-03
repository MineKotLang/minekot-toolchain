package org.minekot.toolchain.lint

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

/**
 * Provides MineKot Detekt rules.
 */
class MineKotRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "minekot"

    override fun instance(config: Config): RuleSet =
        RuleSet(
            ruleSetId,
            mineKotRuleDescriptors.map { descriptor -> descriptor.factory(config) },
        )
}
