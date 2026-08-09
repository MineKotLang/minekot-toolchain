package org.minekot.toolchain.lint.core

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/**
 * Provides the custom MineKot lint rule set to Detekt.
 *
 * This class serves as the entry point for Detekt to discover and load the custom
 * static analysis rules defined within the MineKot toolchain. By implementing
 * [dev.detekt.api.RuleSetProvider], it registers the unique identifier for the
 * rule set and provides an instance of [dev.detekt.api.RuleSet] containing
 * all the individual lint rules.
 */

class MineKotRuleSetProvider : RuleSetProvider {
    /**
     * The unique identifier for the MineKot rule set.
     *
     * This ID, "minekot", is used by Detekt to reference and configure this specific
     * collection of lint rules. It ensures that the rule set can be uniquely
     * identified and enabled/disabled within Detekt's configuration.
     */
    override val ruleSetId: RuleSetId = RuleSetId("minekot")

    /**
     * Creates and returns an instance of the MineKot rule set.
     *
     * Detekt invokes this method to obtain the actual collection of lint rules
     * that comprise the MineKot rule set. It constructs a [dev.detekt.api.RuleSet]
     * by mapping the factories from [mineKotRuleDescriptors] to create the individual
     * rule instances. Each descriptor in [mineKotRuleDescriptors] is expected to
     * provide a factory capable of instantiating a specific lint rule.
     *
     * @return A [RuleSet] containing all the custom MineKot lint rules.
     */
    override fun instance(): RuleSet =
        RuleSet(
            ruleSetId,
            mineKotRuleDescriptors.map { descriptor -> descriptor.factory },
        )
}
