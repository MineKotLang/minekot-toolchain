package org.minekot.toolchain.lint

import com.intellij.psi.PsiComment
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.internal.AutoCorrectable
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * Flags Java APIs where Kotlin standard library or kotlinx APIs are preferred.
 */
@AutoCorrectable(since = "2.0.0")
class KotlinxPreferenceRule(config: Config) : Rule(config, "MineKot codestyle rule.") {
    private val edits: MineKotTextEdits = MineKotTextEdits()
    private val requiredImports: MutableSet<String> = mutableSetOf()
    private var correctedFilesCall: Boolean = false
    private var uncorrectedFilesCall: Boolean = false
    private val issue: Issue = Issue(
        id = "KotlinxPreference",
        severity = Severity.Style,
        description = "MineKot prefers Kotlin standard library and kotlinx APIs over Java equivalents.",
        debt = Debt.TEN_MINS,
    )

    override val ruleName: RuleName get() = RuleName(issue.id)

    override fun preVisit(root: KtFile) {
        edits.clear()
        requiredImports.clear()
        correctedFilesCall = false
        uncorrectedFilesCall = false
    }

    override fun postVisit(root: KtFile) {
        if (correctedFilesCall && !uncorrectedFilesCall) {
            root.importDirectives
                .filter { directive -> directive.importedFqName?.asString() == filesImport }
                .forEach { directive ->
                    val endOffset = directive.textRange.endOffset +
                            root.text.substring(directive.textRange.endOffset)
                                .takeWhile { character -> character == '\n' }.length
                    edits.replace(directive.textRange.startOffset, endOffset, "")
                }
        }
        requiredImports
            .filterNot { requiredImport ->
                root.importDirectives.any { directive ->
                    directive.importedFqName?.asString() == requiredImport && directive.aliasName == null
                }
            }
            .sorted()
            .takeIf { imports -> imports.isNotEmpty() }
            ?.let { imports ->
                val anchor = root.importList?.textRange?.endOffset ?: root.packageDirective?.textRange?.endOffset ?: 0
                val prefix = if (anchor == 0) "" else "\n"
                edits.replace(anchor, anchor, "${prefix}${imports.joinToString("\n") { path -> "import ${path}" }}\n")
            }
        edits.applyTo(root, autoCorrect)
    }

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        val path = importDirective.importedFqName?.asString() ?: return
        val replacement = blockedImports[path] ?: return
        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(importDirective),
                message = "Prefer ${replacement} over ${path}.",
            ),
        )
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        if (expression.receiverExpression.text != "Files") {
            return
        }
        if (expression.isInsideMineKotFormatterControl() || expression.textContainsComment()) {
            return
        }
        val call = expression.selectorExpression as? KtCallExpression ?: return
        val correction = call.toPathCorrection()
        if (correction == null) {
            uncorrectedFilesCall = true
            return
        }
        correctedFilesCall = true
        requiredImports += correction.requiredImport
        edits.replace(expression.textRange.startOffset, expression.textRange.endOffset, correction.source)
    }

    private fun KtDotQualifiedExpression.textContainsComment(): Boolean =
        node.getChildren(null).any { child -> child.psi is PsiComment } || text.contains("/*") || text.contains("//")

    private fun KtCallExpression.toPathCorrection(): PathCorrection? {
        val arguments = valueArguments.mapNotNull { argument -> argument.getArgumentExpression()?.text }
        return when (calleeExpression?.text) {
            "exists" -> arguments.singleOrNull()?.let { path ->
                PathCorrection("${path}.exists()", "kotlin.io.path.exists")
            }

            "newInputStream" -> arguments.singleOrNull()?.let { path ->
                PathCorrection("${path}.inputStream()", "kotlin.io.path.inputStream")
            }

            "newOutputStream" -> arguments.singleOrNull()?.let { path ->
                PathCorrection("${path}.outputStream()", "kotlin.io.path.outputStream")
            }

            "deleteIfExists" -> arguments.singleOrNull()?.let { path ->
                PathCorrection("${path}.deleteIfExists()", "kotlin.io.path.deleteIfExists")
            }

            "move" -> arguments.takeIf { values ->
                values.size == 3 && values.last().substringAfterLast('.') == "REPLACE_EXISTING"
            }?.let { values ->
                PathCorrection(
                    "moveMineKotReplacing(${values[0]}, ${values[1]})",
                    "org.minekot.kotlin.io.moveMineKotReplacing",
                )
            }

            else -> null
        }
    }

    private companion object {
        private const val filesImport: String = "java.nio.file.Files"

        private val blockedImports: Map<String, String> = mapOf(
            "java.io.File" to "java.nio.file.Path with kotlin.io.path or kotlinx-io",
            "java.nio.file.Files" to "kotlin.io.path or kotlinx-io helpers",
            "java.util.Timer" to "kotlinx-coroutines",
            "java.lang.Thread" to "kotlinx-coroutines",
            "java.util.ArrayList" to "Kotlin MutableList",
            "java.util.HashMap" to "Kotlin MutableMap",
            "java.util.HashSet" to "Kotlin MutableSet",
            "java.io.BufferedInputStream" to "kotlinx-io Buffer",
            "java.io.BufferedOutputStream" to "kotlinx-io Buffer",
            "java.io.FileInputStream" to "kotlinx-io Source",
            "java.io.FileOutputStream" to "kotlinx-io Sink",
            "java.io.InputStream" to "kotlinx-io Source",
            "java.io.OutputStream" to "kotlinx-io Sink",
            "java.io.Reader" to "kotlinx-io Source",
            "java.io.Writer" to "kotlinx-io Sink",
            "java.util.concurrent.CompletableFuture" to "kotlinx-coroutines Deferred",
            "java.util.concurrent.Executor" to "kotlinx-coroutines CoroutineDispatcher",
            "java.util.concurrent.ExecutorService" to "kotlinx-coroutines CoroutineDispatcher",
            "java.util.concurrent.Executors" to "kotlinx-coroutines",
            "com.fasterxml.jackson.databind.ObjectMapper" to "kotlinx-serialization",
            "com.google.gson.Gson" to "kotlinx-serialization",
            "org.json.JSONArray" to "kotlinx-serialization",
            "org.json.JSONObject" to "kotlinx-serialization",
        )
    }
}

private data class PathCorrection(
    val source: String,
    val requiredImport: String,
)
