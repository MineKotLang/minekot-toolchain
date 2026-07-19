package org.minekot.toolchain.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.internal.AutoCorrectable
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Enforces MineKot Gradle Kotlin DSL conventions that can be determined safely from syntax.
 */
@AutoCorrectable(since = "2.0.0")
class GradleDslConventionsRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val issue: Issue = Issue(
        id = "GradleDslConventions",
        severity = Severity.Style,
        description = "MineKot Gradle scripts use declarative, reproducible, type-safe conventions.",
        debt = Debt.TEN_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)
    private val edits = MineKotTextEdits()

    override fun preVisit(root: KtFile) {
        edits.clear()
    }

    override fun postVisit(root: KtFile) {
        edits.applyTo(root, autoCorrect)
    }

    override fun visit(root: KtFile) {
        super.visit(root)
        if (!root.name.endsWith(".gradle.kts")) {
            return
        }
        val source = root.text
        val pluginsOffset = source.indexOf("plugins {")
        val firstBuildConfigurationOffset = listOf(source.indexOf("repositories {"), source.indexOf("dependencies {"))
            .filter { offset -> offset >= 0 }
            .minOrNull()
        if (pluginsOffset >= 0 && firstBuildConfigurationOffset != null && pluginsOffset > firstBuildConfigurationOffset) {
            root.reportConvention("Place the plugins block before build configuration blocks.")
        }
        val localRepositoryOffset = source.indexOf("mavenLocal()")
        val centralRepositoryOffset = source.indexOf("mavenCentral()")
        if (
            localRepositoryOffset >= 0 &&
            centralRepositoryOffset >= 0 &&
            localRepositoryOffset > centralRepositoryOffset
        ) {
            root.reportConvention("Declare mavenLocal() before mavenCentral().")
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (!expression.containingKtFile.name.endsWith(".gradle.kts")) {
            return
        }
        when {
            expression.isImperativePluginApplication() -> expression.reportConvention(
                "Apply plugins with the plugins block and type-safe accessors.",
            )

            expression.isDynamicDependency() -> expression.reportConvention(
                "Use a fixed dependency version or version catalog entry.",
            )

            expression.hasInvalidTaskName() -> expression.reportConvention(
                "Gradle task names must use camelCase and start with a verb.",
            )

            expression.isVerboseMavenRepositoryDeclaration() -> {
                expression.reportConvention("Use maven(\"url\") for repository declarations.")
                expression.scheduleVerboseMavenRepositoryCorrection()
            }

            expression.isSingleLineGradleBlock() -> expression.reportConvention(
                "Format Gradle configuration blocks across multiple lines.",
            )

            expression.isShortKotlinDependencyNotation() -> expression.reportConvention(
                "Use a version catalog or an explicitly versioned dependency string.",
            )

            expression.isFileProjectDependency() -> expression.reportConvention(
                "Use project() notation instead of depending on a built project JAR.",
            )

            expression.isEagerTaskConfiguration() -> expression.reportConvention(
                "Configure tasks lazily with named() or register().",
            )

            expression.isUntypedAccessor() -> expression.reportConvention(
                "Use a type-safe Gradle accessor or typed extension block.",
            )

            expression.hasInvalidNamedContainerElement() -> expression.reportConvention(
                "Gradle extension and configuration names must use lower camel case.",
            )

            expression.isBroadSubprojectConfiguration() -> expression.reportConvention(
                "Move shared subproject configuration into a convention plugin.",
            )

            expression.isInlineTaskAction() -> expression.reportConvention(
                "Move inline task actions into a typed task class or convention plugin.",
            )
        }
    }

    private fun KtCallExpression.isImperativePluginApplication(): Boolean =
        calleeExpression?.text == "apply" && valueArguments.any { argument ->
            argument.getArgumentName()?.text == "plugin"
        } && parents.filterIsInstance<KtCallExpression>().none { parentCall ->
            parentCall.calleeExpression?.text in setOf("allprojects", "subprojects")
        }

    private fun KtCallExpression.isDynamicDependency(): Boolean {
        val callName = calleeExpression?.text ?: return false
        if (!dependencyConfigurationPattern.matches(callName)) {
            return false
        }
        val dependency = valueArguments
            .firstOrNull()
            ?.getArgumentExpression() as? KtStringTemplateExpression ?: return false
        return dynamicVersionPattern.containsMatchIn(dependency.text.removeSurrounding("\""))
    }

    private fun KtCallExpression.hasInvalidTaskName(): Boolean {
        if (calleeExpression?.text !in taskRegistrationCalls) {
            return false
        }
        val qualifiedExpression = parent as? KtDotQualifiedExpression ?: return false
        if (qualifiedExpression.receiverExpression.text != "tasks") {
            return false
        }
        val taskName = valueArguments
            .firstOrNull()
            ?.getArgumentExpression()
            ?.text
            ?.removeSurrounding("\"") ?: return false
        return !camelCaseTaskNamePattern.matches(taskName)
    }

    private fun KtCallExpression.isVerboseMavenRepositoryDeclaration(): Boolean =
        calleeExpression?.text == "maven" && lambdaArguments.any { argument ->
            argument.getLambdaExpression()?.text?.contains(repositoryUrlAssignmentPattern) == true
        }

    private fun KtCallExpression.scheduleVerboseMavenRepositoryCorrection() {
        val match = simpleVerboseMavenRepositoryPattern.matchEntire(text) ?: return
        edits.replace(textRange.startOffset, textRange.endOffset, "maven(${match.groupValues[1]})")
    }

    private fun KtCallExpression.isSingleLineGradleBlock(): Boolean =
        calleeExpression?.text in multilineGradleBlocks &&
                lambdaArguments.any { argument -> '\n' !in argument.text }

    private fun KtCallExpression.isShortKotlinDependencyNotation(): Boolean {
        if (calleeExpression?.text != "kotlin") {
            return false
        }
        val valueArgument = parent as? org.jetbrains.kotlin.psi.KtValueArgument ?: return false
        val dependencyCall = valueArgument.parent?.parent as? KtCallExpression ?: return false
        return dependencyConfigurationPattern.matches(dependencyCall.calleeExpression?.text.orEmpty())
    }

    private fun KtCallExpression.isFileProjectDependency(): Boolean {
        if (!dependencyConfigurationPattern.matches(calleeExpression?.text.orEmpty())) {
            return false
        }
        return valueArguments.any { argument ->
            (argument.getArgumentExpression() as? KtCallExpression)?.calleeExpression?.text == "files"
        }
    }

    private fun KtCallExpression.isEagerTaskConfiguration(): Boolean {
        val qualifiedExpression = parent as? KtDotQualifiedExpression ?: return false
        if (qualifiedExpression.receiverExpression.text != "tasks") {
            return false
        }
        if (calleeExpression?.text == "withType" && parents.filterIsInstance<KtDotQualifiedExpression>().any { chain ->
                (chain.selectorExpression as? KtCallExpression)?.calleeExpression?.text == "configureEach"
            }
        ) {
            return false
        }
        return calleeExpression?.text in eagerTaskCalls
    }

    private fun KtCallExpression.isUntypedAccessor(): Boolean =
        calleeExpression?.text == "the" ||
                calleeExpression?.text == "withType" && valueArguments.any { argument ->
            argument.text.contains("::class.java")
        }

    private fun KtCallExpression.hasInvalidNamedContainerElement(): Boolean {
        val qualifiedExpression = parent as? KtDotQualifiedExpression ?: return false
        if (
            qualifiedExpression.receiverExpression.text !in namedContainers ||
            calleeExpression?.text !in setOf("create", "register")
        ) {
            return false
        }
        val name = valueArguments.firstOrNull()?.getArgumentExpression()?.text?.removeSurrounding("\"") ?: return false
        return !camelCaseTaskNamePattern.matches(name)
    }

    private fun KtCallExpression.isBroadSubprojectConfiguration(): Boolean =
        calleeExpression?.text in setOf("allprojects", "subprojects") && lambdaArguments.isNotEmpty()

    private fun KtCallExpression.isInlineTaskAction(): Boolean {
        if (calleeExpression?.text !in setOf("doFirst", "doLast")) {
            return false
        }
        val body = lambdaArguments.firstOrNull()?.getLambdaExpression()?.bodyExpression ?: return false
        return body.statements.size > 1 || body.statements.any { statement -> '\n' in statement.text }
    }

    private fun KtCallExpression.reportConvention(message: String) {
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(this),
                message = message,
            ),
        )
    }

    private fun KtFile.reportConvention(message: String) {
        report(CodeSmell(issue, Entity.from(this), message))
    }

    private companion object {
        private val dependencyConfigurationPattern: Regex =
            Regex("(?:api|implementation|compileOnly|runtimeOnly|testImplementation|testRuntimeOnly)")
        private val dynamicVersionPattern: Regex = Regex("(?:^|:)(?:latest\\.[A-Za-z]+|[^:]*\\+)(?:$|@)")
        private val camelCaseTaskNamePattern: Regex = Regex("[a-z][A-Za-z0-9]*")
        private val repositoryUrlAssignmentPattern: Regex = Regex("url\\s*=\\s*uri\\(")
        private val simpleVerboseMavenRepositoryPattern: Regex =
            Regex("maven\\s*\\{\\s*url\\s*=\\s*uri\\((\"[^\"]*\")\\)\\s*}")
        private val taskRegistrationCalls: Set<String> = setOf("create", "register")
        private val eagerTaskCalls: Set<String> = setOf("create", "getByName", "getAt", "withType")
        private val multilineGradleBlocks: Set<String> = setOf("dependencies", "plugins", "repositories")
        private val namedContainers: Set<String> = setOf("configurations", "extensions")
    }
}
