package org.minekot.toolchain.lint

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/**
 * Provides MineKot Detekt rules.
 */
class MineKotRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("minekot")

    override fun instance(): RuleSet =
        RuleSet(
            ruleSetId,
            mineKotRuleDescriptors.map { descriptor -> descriptor.factory },
        )
}
