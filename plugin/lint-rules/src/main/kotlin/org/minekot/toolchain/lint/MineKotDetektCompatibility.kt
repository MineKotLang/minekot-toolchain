package org.minekot.toolchain.lint

import dev.detekt.api.Entity
import dev.detekt.api.Finding

/**
 * Minimal compatibility layer for MineKot rules migrated from Detekt 1 findings to Detekt 2 findings.
 */
internal data class Issue(
    val id: String,
    val severity: Severity,
    val description: String,
    val debt: Debt,
)

/**
 * Legacy severity names used by MineKot rule descriptors.
 */
internal enum class Severity {
    Style,
}

/**
 * Legacy debt markers retained for MineKot rule metadata.
 */
internal enum class Debt {
    FIVE_MINS,
    TEN_MINS,
}

/**
 * Creates a Detekt 2 finding while retaining old MineKot rule call sites.
 */
@Suppress("FunctionName")
internal fun CodeSmell(
    issue: Issue,
    entity: Entity,
    message: String,
): Finding = Finding(
    entity = entity,
    message = message,
)
