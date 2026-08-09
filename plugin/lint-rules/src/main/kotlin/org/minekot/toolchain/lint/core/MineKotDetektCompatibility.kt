package org.minekot.toolchain.lint.core

import dev.detekt.api.Entity
import dev.detekt.api.Finding

/**
 * Represents a detected code issue within the MineKot linting framework.
 *
 * This data class serves as a standardized structure for defining issues identified by MineKot's custom Detekt rules.
 * It encapsulates essential information about a finding, including its unique identifier, severity, a detailed
 * description, and an estimate of the technical debt associated with its resolution. This structure facilitates
 * the migration of older Detekt 1-based findings to the Detekt 2 API, ensuring compatibility and consistency
 * across the linting toolchain.
 *
 * @property id A unique identifier for this specific type of issue, often used for configuration or suppression.
 * For example, "MineKot:UnnecessaryCast".
 * @property severity The [Severity] level of the issue, indicating its impact and priority. This helps in
 * categorizing and prioritizing fixes.
 * @property description A detailed explanation of the issue, including why it's problematic and
 * potentially how to resolve it. This message is typically displayed to the user.
 * @property debt An estimate of the [Debt] (technical debt) associated with fixing this issue,
 * quantified in terms of time or effort.
 *
 * Minimal compatibility layer for MineKot rules migrated from Detekt 1 findings to Detekt 2 findings.
 */
internal data class Issue(
    val id: String,
    val severity: Severity,
    val description: String,
    val debt: Debt,
)

/**
 * Represents the severity level of a detected code issue.
 *
 * This enum categorizes the impact or importance of a [Finding] or [Issue] identified by a MineKot lint rule.
 * It helps in prioritizing which issues to address first, aligning with common static analysis tool classifications.
 */
internal enum class Severity {
    /**
     * Indicates a potential defect or error in the code that could lead to incorrect behavior.
     */
    Bug,

    /**
     * Points to a structural problem in the code that might hinder maintainability, readability, or extensibility,
     * even if it's not an immediate bug.
     */
    CodeSmell,

    /**
     * Highlights issues that could negatively impact the runtime efficiency or resource consumption of the application.
     */
    Performance,

    /**
     * Denotes violations of coding conventions or stylistic guidelines, often related to code consistency and readability.
     */
    Style,
}

/**
 * Represents the estimated technical debt associated with fixing a detected code issue.
 *
 * This enum quantifies the effort or time required to resolve a [Finding] or [Issue] identified by a MineKot lint rule.
 * It provides a standardized way to categorize the complexity of fixes, aiding in prioritization and resource allocation
 * for code quality improvements. The values are approximate and serve as a guideline for the impact of a given issue.
 * This helps in understanding the "cost" of maintaining or improving code quality based on the severity and complexity
 * of the identified problems.
 */
internal enum class Debt {
    /**
     * Represents a very minor issue expected to take approximately 10 seconds to fix.
     * This typically applies to simple stylistic changes or trivial refactorings.
     */
    TEN_SECONDS,

    /**
     * Represents a minor issue expected to take approximately 1 minute to fix.
     * This might include small code adjustments, renaming, or minor structural improvements.
     */
    ONE_MIN,

    /**
     * Represents a moderate issue expected to take approximately 5 minutes to fix.
     * This could involve refactoring a small function, adjusting logic, or addressing a clear but contained code smell.
     */
    FIVE_MINS,

    /**
     * Represents a more significant issue expected to take approximately 10 minutes to fix.
     * This category might cover more complex refactorings, addressing performance bottlenecks, or resolving
     * architectural concerns within a limited scope.
     */
    TEN_MINS,
}

/**
 * Creates a Detekt 2 [Finding] instance, specifically representing a code smell, while maintaining compatibility
 * with older MineKot rule call sites that might have expected a direct "CodeSmell" type.
 *
 * This function serves as a crucial part of the compatibility layer, allowing MineKot's custom Detekt rules,
 * which were potentially designed around Detekt 1's finding structure or a more specific "CodeSmell" concept,
 * to seamlessly integrate with the Detekt 2 API. It encapsulates the provided [entity] and [message] into a
 * standard Detekt [Finding], effectively translating the context of a MineKot [Issue] into the Detekt 2 framework.
 * The [issue] parameter provides the rich metadata (like [Issue.id], [Issue.severity], and [Issue.debt])
 * that defines the nature of the code smell according to MineKot's classification, even if not all its properties
 * are directly mapped to the Detekt [Finding] constructor.
 *
 * @param issue The custom [Issue] object containing detailed information about the detected code smell,
 * including its unique identifier, severity, description, and estimated technical debt.
 * @param entity The [Entity] representing the specific code element (e.g., class, function, expression)
 * in the source code where this code smell was detected.
 * @param message A concise and human-readable message describing the detected code smell, which will be
 * displayed as part of the Detekt [Finding].
 * @return A new [Finding] object, conforming to the Detekt 2 API, representing the identified code smell.
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
