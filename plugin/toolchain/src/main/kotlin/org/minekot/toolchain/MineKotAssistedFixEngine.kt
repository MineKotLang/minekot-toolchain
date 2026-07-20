package org.minekot.toolchain

import kotlinx.serialization.json.Json
import java.security.MessageDigest
import kotlin.io.path.*

/** Computes assisted-fix previews and validated source replacements. */
internal class MineKotAssistedFixEngine(private val root: java.nio.file.Path) {
    /** Builds a preview without mutating source files. */
    fun preview(document: MineKotAssistRequestDocument): MineKotAssistPreview {
        require(document.schemaVersion == schemaVersion) {
            "Unsupported MineKot assisted-fix schema ${document.schemaVersion}; expected ${schemaVersion}."
        }
        val sources = sourceFiles().associateWith { path -> path.readText() }
        val candidates = sources
            .flatMap { (path, source) -> path.findCandidates(source) }
            .sortedBy { candidate -> candidate.findingId }
        val requests = document.requests.associateBy { request -> request.findingId }
        val replacements = mutableMapOf<java.nio.file.Path, String>()
        val readyApplications = mutableListOf<ReadyApplication>()
        val reports = candidates.map { candidate ->
            val request = requests[candidate.findingId]
            val optionValues = request?.options?.values().orEmpty()
            val requiredOptions = candidate.requiredOptions + candidate.dynamicRequiredOptions(request)
            val missingOptions = requiredOptions.distinct().filter { option -> optionValues[option].isNullOrBlank() }
            val validationError = request?.let { candidate.validate(it) }
            val status = when {
                candidate.unsupportedReason != null -> "unsupported"
                request == null || missingOptions.isNotEmpty() -> "needs-input"
                request.action != candidate.action -> "invalid-request"
                validationError != null -> "invalid-request"
                else -> "ready"
            }
            if (status == "ready" && request != null) {
                readyApplications += ReadyApplication(
                    candidate,
                    optionValues.mapNotNull { (key, value) -> value?.let { key to it } }.toMap(),
                )
            }
            MineKotAssistCandidate(
                findingId = candidate.findingId,
                action = candidate.action,
                path = candidate.path.relativeTo(root).invariantSeparatorsPathString,
                line = candidate.line,
                status = status,
                requiredOptions = missingOptions,
                message = candidate.unsupportedReason ?: validationError ?: candidate.message,
            )
        } + document.requests
            .filter { request -> candidates.none { candidate -> candidate.findingId == request.findingId } }
            .map { request ->
                MineKotAssistCandidate(
                    findingId = request.findingId,
                    action = request.action,
                    path = "",
                    line = 0,
                    status = "invalid-request",
                    requiredOptions = emptyList(),
                    message = "Finding is stale or does not exist in the current source tree.",
                )
            }
        val fileMigration = readyApplications.firstOrNull { application ->
            application.candidate.action == fileToPathAction
        }
        fileMigration?.options?.get("symbol")?.let { symbol ->
            sources.forEach { (path, source) ->
                source.migrateProjectFiles(symbol)?.let { corrected -> replacements[path] = corrected }
            }
        }
        readyApplications
            .filterNot { application -> application.candidate.action == fileToPathAction }
            .groupBy { application -> application.candidate.path }
            .forEach { (path, applications) ->
                val original = sources.getValue(path)
                applyApplications(original, applications)?.let { corrected -> replacements[path] = corrected }
            }
        val normalizedRequests = document.copy(confirmation = null)
        val planId = sha256(
            Json.encodeToString(normalizedRequests) +
                    candidates.joinToString("|") { candidate -> candidate.findingId },
        )
        return MineKotAssistPreview(
            report = MineKotAssistReport(
                schemaVersion = schemaVersion,
                planId = planId,
                candidates = reports,
                changedFiles = replacements.keys
                    .map { path -> path.relativeTo(root).invariantSeparatorsPathString }
                    .sorted(),
            ),
            replacements = replacements,
            diff = replacements.entries.joinToString("\n") { (path, corrected) ->
                path.unifiedDiff(sources.getValue(path), corrected)
            },
        )
    }

    private fun sourceFiles(): List<java.nio.file.Path> =
        root.walk()
            .filter { path ->
                path.isRegularFile() && path.extension in sourceExtensions &&
                        path.none { segment -> segment.name in ignoredDirectories }
            }
            .toList()

    private fun java.nio.file.Path.findCandidates(source: String): List<InternalCandidate> = buildList {
        addFileToPathCandidate(source)?.let(::add)
        magicNumberPattern.findAll(source).forEach { match ->
            val literal = match.value
            if (literal.toNormalizedNumber() !in allowedNumbers) {
                val numericType = source.inferNumericType(match.range, literal)
                add(
                    candidate(
                        source,
                        match.range,
                        magicNumberAction,
                        listOf("constant-name"),
                        "Extract named ${numericType} constant.",
                        numericType = numericType,
                    ),
                )
            }
        }
        ignoredResultPattern.findAll(source).forEach { match ->
            add(candidate(source, match.range, resultAction, listOf("result-mode"), "Choose Result failure behavior."))
        }
        threadSleepPattern.findAll(source).forEach { match ->
            add(
                candidate(
                    source,
                    match.range,
                    coroutineAction,
                    listOf("scope-property", "dispatcher", "cancellation-point", "cleanup-method"),
                    "Replace blocking sleep after confirming lifecycle, dispatcher, cancellation, and cleanup.",
                ),
            )
        }
        val supportedCoroutineRanges = mutableListOf<IntRange>()
        simpleThreadPattern.findAll(source).forEach { match ->
            supportedCoroutineRanges += match.range
            add(
                candidate(
                    source,
                    match.range,
                    coroutineAction,
                    listOf("scope-property", "dispatcher", "cancellation-point", "cleanup-method"),
                    "Replace owned thread with lifecycle-managed coroutine work.",
                ),
            )
        }
        legacyTextPattern.findAll(source).forEach { match ->
            add(
                candidate(
                    source,
                    match.range,
                    miniMessageAction,
                    listOf("localization-mode", "semantic-tag"),
                    "Choose MiniMessage localization and semantic tag.",
                    requiresPlaceholders = '$' in match.value,
                ),
            )
        }
        simpleTryCatchPattern.findAll(source).forEach { match ->
            val trailing = source.drop(match.range.last + 1).trimStart()
            val unsupportedReason = when {
                unsafeTryCatchBodyPattern.containsMatchIn(match.value) ->
                    "Automatic rewrite refused: returns, rethrows, and explicit throws require control-flow analysis."

                trailing.startsWith("finally") ->
                    "Automatic rewrite refused: finally cleanup must remain explicit."

                trailing.startsWith("catch") ->
                    "Automatic rewrite refused: multiple catch filters are ambiguous."

                else -> null
            }
            add(
                candidate(
                    source,
                    match.range,
                    tryCatchAction,
                    listOf("failure-mode"),
                    "Choose catch failure behavior.",
                    unsupportedReason,
                ),
            )
        }
        unsupportedCoroutinePattern.findAll(source).forEach { match ->
            if (supportedCoroutineRanges.any { range -> match.range.first in range }) {
                return@forEach
            }
            add(
                candidate(
                    source,
                    match.range,
                    coroutineAction,
                    listOf("scope-property", "dispatcher", "close-hook"),
                    "Lifecycle migration requires explicit ownership choices.",
                    "Automatic rewrite refused: owned thread, timer, or executor shape needs lifecycle-aware structural analysis.",
                ),
            )
        }
    }

    private fun java.nio.file.Path.addFileToPathCandidate(source: String): InternalCandidate? {
        val match = fileImportPattern.find(source) ?: return null
        return candidate(
            source,
            match.range,
            fileToPathAction,
            listOf("symbol"),
            "Migrate selected File symbol and resolved local consumers to Path.",
        )
    }

    private fun java.nio.file.Path.candidate(
        source: String,
        range: IntRange,
        action: String,
        requiredOptions: List<String>,
        message: String,
        unsupportedReason: String? = null,
        numericType: String? = null,
        requiresPlaceholders: Boolean = false,
    ): InternalCandidate {
        val fingerprint = sha256(source)
        val relativePath = relativeTo(root).invariantSeparatorsPathString
        return InternalCandidate(
            findingId = sha256("${action}|${relativePath}|${range.first}|${range.last + 1}|${fingerprint}"),
            action = action,
            path = this,
            range = range,
            line = source.take(range.first).count { character -> character == '\n' } + 1,
            requiredOptions = requiredOptions,
            message = message,
            unsupportedReason = unsupportedReason,
            numericType = numericType,
            requiresPlaceholders = requiresPlaceholders,
        )
    }

    private fun applyApplications(
        source: String,
        applications: List<ReadyApplication>,
    ): String? {
        val edits = mutableListOf<MineKotSourceEdit>()
        val imports = mutableSetOf<String>()
        val declarations = mutableListOf<String>()
        applications.sortedByDescending { application -> application.candidate.range.first }.forEach { application ->
            val candidate = application.candidate
            val options = application.options
            val expected = source.substring(candidate.range)
            val replacement = when (candidate.action) {
                magicNumberAction -> {
                    val name = options.getValue("constant-name")
                    val type = candidate.numericType ?: expected.numericType()
                    declarations += "private const val ${name}: ${type} = ${expected}"
                    name
                }

                resultAction -> expected.handleResultExpression(options.getValue("result-mode"), options)
                coroutineAction -> {
                    imports += "kotlinx.coroutines.delay"
                    imports += "kotlinx.coroutines.launch"
                    expected.coroutineExpression(options)
                }

                miniMessageAction -> {
                    imports += "org.minekot.adventure.minimessage.mineKotMiniMessage"
                    expected.miniMessageExpression(options)
                }

                tryCatchAction -> expected.tryCatchExpression(options.getValue("failure-mode"))
                else -> null
            } ?: return null
            edits += MineKotSourceEdit(
                startOffset = candidate.range.first,
                endOffset = candidate.range.last + 1,
                expectedText = expected,
                replacement = replacement,
            )
        }
        var corrected = MineKotSourceEditEngine.apply(source, edits)
        imports.sorted().forEach { path -> corrected = corrected.addImport(path) }
        if (declarations.isNotEmpty()) {
            val insertionOffset = corrected.importBlockEnd()
            val text = declarations.distinct().sorted().joinToString(separator = "\n") + "\n\n"
            corrected = corrected.replaceRange(insertionOffset, insertionOffset, text)
        }
        return corrected.takeUnless { text -> text == source }
    }

    private fun InternalCandidate.dynamicRequiredOptions(request: MineKotAssistRequest?): List<String> = buildList {
        val values = request?.options?.values().orEmpty()
        if (action == resultAction && values["result-mode"] == "fallback") {
            add("fallback-expression")
        }
        if (action == resultAction && values["result-mode"] == "on-failure") {
            add("failure-handler")
        }
        if (action == miniMessageAction && values["localization-mode"] == "key") {
            add("localization-expression")
        }
        if (action == miniMessageAction && requiresPlaceholders) {
            add("placeholders-expression")
        }
    }

    private fun InternalCandidate.validate(request: MineKotAssistRequest): String? = when {
        request.action != action -> "Request action ${request.action} does not match candidate action ${action}."
        !request.options.matchesAction(action) -> "Options type does not match action ${action}."
        request.options?.values()?.values?.filterNotNull()?.any(String::isBlank) == true ->
            "Assisted-fix option values cannot be blank."

        action == fileToPathAction && request.options?.values()?.get("symbol")?.matches(identifierPattern) != true ->
            "symbol must be a Kotlin identifier."

        action == resultAction && request.options?.values()
            ?.get("result-mode") !in resultModes -> "Unsupported result-mode."

        action == miniMessageAction && request.options?.values()?.get("localization-mode") !in localizationModes ->
            "Unsupported localization-mode."

        action == tryCatchAction && request.options?.values()?.get("failure-mode") !in tryCatchModes ->
            "Unsupported failure-mode."

        action == coroutineAction && request.options?.values()?.get("scope-property")
            ?.matches(identifierPattern) != true ->
            "scope-property must be a Kotlin identifier."

        action == miniMessageAction && request.options?.values()?.get("semantic-tag")
            ?.matches(semanticTagPattern) != true ->
            "semantic-tag must be a valid MiniMessage tag name."

        action == magicNumberAction && request.options?.values()?.get("constant-name")
            ?.matches(constantNamePattern) != true ->
            "constant-name must use uppercase snake case."

        else -> null
    }

    private fun MineKotAssistOptions?.matchesAction(action: String): Boolean = when (action) {
        fileToPathAction -> this is MineKotFileToPathOptions
        magicNumberAction -> this is MineKotMagicNumberOptions
        resultAction -> this is MineKotResultHandlingOptions
        coroutineAction -> this is MineKotCoroutineOptions
        miniMessageAction -> this is MineKotMiniMessageOptions
        tryCatchAction -> this is MineKotTryCatchOptions
        else -> false
    }

    private fun String.migrateProjectFiles(symbol: String): String? {
        if (!containsFileUsagePattern.containsMatchIn(this) || !contains(Regex("\\b${Regex.escape(symbol)}\\s*\\("))) {
            return null
        }
        val selectedRanges = buildList {
            Regex("(?m)^.*\\bfun\\s+${Regex.escape(symbol)}\\s*\\([^\\n]*$").findAll(this@migrateProjectFiles)
                .mapTo(this) { match -> match.range }
            Regex("(?m)^.*\\b${Regex.escape(symbol)}\\s*\\([^\\n]*$").findAll(this@migrateProjectFiles)
                .mapTo(this) { match -> match.range }
        }.distinct()
        if (selectedRanges.isEmpty()) {
            return null
        }
        val edits = mutableListOf<MineKotSourceEdit>()
        val importMatch = fileImportPattern.find(this)
        importMatch?.let { match ->
            edits += MineKotSourceEdit(
                startOffset = match.range.first,
                endOffset = match.range.last + 1,
                expectedText = match.value,
                replacement = "import java.nio.file.Path",
            )
        }
        kotlinCodeMask().let { code ->
            Regex("\\bFile\\b").findAll(code).forEach { match ->
                if (selectedRanges.none { range -> match.range.first in range }) {
                    return@forEach
                }
                val nextCharacter = drop(match.range.last + 1).firstOrNull { character -> !character.isWhitespace() }
                edits += MineKotSourceEdit(
                    startOffset = match.range.first,
                    endOffset = match.range.last + 1,
                    expectedText = "File",
                    replacement = if (nextCharacter == '(') "Path.of" else "Path",
                )
            }
        }
        val corrected = MineKotSourceEditEngine.apply(this, edits)
        return corrected.takeUnless { source -> source == this }
    }

    private fun String.kotlinCodeMask(): String {
        val masked = toCharArray()
        var index = 0
        while (index < length) {
            val end = when {
                startsWith("//", index) -> indexOf('\n', index).let { if (it < 0) length else it }
                startsWith("/*", index) -> indexOf("*/", index + 2).let { if (it < 0) length else it + 2 }
                startsWith("\"\"\"", index) -> indexOf("\"\"\"", index + 3).let { if (it < 0) length else it + 3 }
                this[index] == '"' -> quotedEnd(index, '"')
                this[index] == '\'' -> quotedEnd(index, '\'')
                else -> {
                    index += 1
                    continue
                }
            }
            (index until end).forEach { offset ->
                if (masked[offset] != '\n') masked[offset] = ' '
            }
            index = end
        }
        return masked.concatToString()
    }

    private fun String.quotedEnd(start: Int, quote: Char): Int {
        var index = start + 1
        while (index < length) {
            if (this[index] == '\\') {
                index += 2
            } else if (this[index] == quote) {
                return index + 1
            } else {
                index += 1
            }
        }
        return length
    }

    private fun String.handleResultExpression(mode: String, options: Map<String, String>): String? = when (mode) {
        "discard" -> "${this}.getOrNull()"
        "fallback" -> "${this}.getOrElse { ${options["fallback-expression"] ?: return null} }"
        "on-failure" -> "${this}.onFailure { ${options["failure-handler"] ?: return null} }"
        else -> null
    }

    private fun String.coroutineExpression(options: Map<String, String>): String? {
        val scope = options["scope-property"] ?: return null
        val dispatcher = options["dispatcher"] ?: return null
        val cancellationPoint = options["cancellation-point"] ?: return null
        val cleanupMethod = options["cleanup-method"] ?: return null
        val body = simpleThreadPattern.matchEntire(this)?.groupValues?.get(1)?.trim()
            ?: replace(Regex("(?:java\\.lang\\.)?Thread\\.sleep"), "delay")
        return "${scope}.scope.launch(${dispatcher}) { ${cancellationPoint}; ${body} }" +
                ".also { job -> job.invokeOnCompletion { ${cleanupMethod} } }"
    }

    private fun String.miniMessageExpression(options: Map<String, String>): String? {
        if (options["localization-mode"] !in localizationModes) {
            return null
        }
        val tag = options.getValue("semantic-tag").takeIf(semanticTagPattern::matches) ?: return null
        val content = removeSurrounding("\"")
            .replace(legacyColorPattern, "")
            .replace(stringTemplatePattern) { match -> "<${match.groupValues[1]}>" }
        val sourceExpression = if (options["localization-mode"] == "key") {
            options["localization-expression"] ?: return null
        } else {
            "\"<${tag}>${content}</${tag}>\""
        }
        return if ('$' in this) {
            "mineKotMiniMessage(${sourceExpression}, ${options["placeholders-expression"] ?: return null})"
        } else {
            "mineKotMiniMessage(${sourceExpression})"
        }
    }

    private fun String.tryCatchExpression(mode: String): String? {
        val match = simpleTryCatchPattern.matchEntire(this) ?: return null
        val body = match.groupValues[1].trim()
        val exceptionType = match.groupValues[3]
        val failure = match.groupValues[4].trim()
        val guardedFailure = "if (throwable is ${exceptionType}) { ${failure} } else { throw throwable }"
        return when (mode) {
            "fallback", "rethrow-unmatched" -> "runCatching { ${body} }.getOrElse { throwable -> ${guardedFailure} }"
            "discard" ->
                "runCatching { ${body} }.getOrElse { throwable -> " +
                        "if (throwable is ${exceptionType}) { ${failure}; null } else { throw throwable } }"

            else -> null
        }
    }

    private fun String.extractConstant(
        range: IntRange,
        name: String,
        inferredType: String?,
    ): String? {
        if (!constantNamePattern.matches(name)) {
            return null
        }
        val literal = substring(range)
        val type = inferredType ?: literal.numericType()
        val declaration = "private const val ${name}: ${type} = ${literal}\n\n"
        val insertionOffset = importBlockEnd()
        return replaceRange(range, name).let { corrected ->
            corrected.replaceRange(insertionOffset, insertionOffset, declaration)
        }
    }

    private fun String.handleResult(range: IntRange, mode: String, options: Map<String, String>): String? {
        val expression = substring(range)
        val replacement = when (mode) {
            "discard" -> "${expression}.getOrNull()"
            "fallback" -> "${expression}.getOrElse { ${options["fallback-expression"] ?: return null} }"
            "on-failure" -> "${expression}.onFailure { ${options["failure-handler"] ?: return null} }"
            else -> return null
        }
        return replaceRange(range, replacement)
    }

    private fun String.replaceSleep(range: IntRange, scopeProperty: String): String? {
        if (!identifierPattern.matches(scopeProperty)) {
            return null
        }
        val delayCall = substring(range).replace(Regex("(?:java\\.lang\\.)?Thread\\.sleep"), "delay")
        val replacement = "${scopeProperty}.scope.launch { ${delayCall} }"
        return replaceRange(range, replacement)
            .addImport("kotlinx.coroutines.delay")
            .addImport("kotlinx.coroutines.launch")
    }

    private fun String.wrapMiniMessage(range: IntRange, options: Map<String, String>): String? {
        if (options["localization-mode"] !in setOf("inline", "key")) {
            return null
        }
        val tag = options.getValue("semantic-tag").takeIf { value -> semanticTagPattern.matches(value) } ?: return null
        val literal = substring(range)
        val content = literal.removeSurrounding("\"")
            .replace(legacyColorPattern, "")
            .replace(stringTemplatePattern) { match -> "<${match.groupValues[1]}>" }
        val sourceExpression = if (options["localization-mode"] == "key") {
            options["localization-expression"] ?: return null
        } else {
            "\"<${tag}>${content}</${tag}>\""
        }
        val placeholders = options["placeholders-expression"]
        val replacement = if ('$' in literal) {
            "mineKotMiniMessage(${sourceExpression}, ${placeholders ?: return null})"
        } else {
            "mineKotMiniMessage(${sourceExpression})"
        }
        return replaceRange(range, replacement).addImport("org.minekot.adventure.minimessage.mineKotMiniMessage")
    }

    private fun String.convertTryCatch(range: IntRange, mode: String): String? {
        val match = simpleTryCatchPattern.matchEntire(substring(range)) ?: return null
        val body = match.groupValues[1].trim()
        val exceptionType = match.groupValues[3]
        val failure = match.groupValues[4].trim()
        val guardedFailure =
            "if (throwable is ${exceptionType}) { ${failure} } else { throw throwable }"
        val replacement = when (mode) {
            "fallback", "rethrow-unmatched" -> "runCatching { ${body} }.getOrElse { throwable -> ${guardedFailure} }"
            "discard" ->
                "runCatching { ${body} }.getOrElse { throwable -> " +
                        "if (throwable is ${exceptionType}) { ${failure}; null } else { throw throwable } }"

            else -> return null
        }
        return replaceRange(range, replacement)
    }

    private fun String.addImport(path: String): String {
        if (contains("import ${path}")) {
            return this
        }
        val offset = importBlockEnd()
        return replaceRange(offset, offset, "import ${path}\n")
    }

    private fun String.importBlockEnd(): Int {
        val imports = Regex("(?m)^import .+\\n").findAll(this).toList()
        if (imports.isNotEmpty()) {
            return imports.last().range.last + 1
        }
        return Regex("(?m)^package .+\\n+").find(this)?.range?.last?.plus(1) ?: 0
    }

    private fun java.nio.file.Path.unifiedDiff(before: String, after: String): String {
        val relativePath = relativeTo(root).invariantSeparatorsPathString
        val beforeLines = before.lines()
        val afterLines = after.lines()
        val commonPrefix = beforeLines.zip(afterLines).takeWhile { (old, new) -> old == new }.size
        val maximumSuffix = minOf(beforeLines.size, afterLines.size) - commonPrefix
        val commonSuffix = (1..maximumSuffix).takeWhile { distance ->
            beforeLines[beforeLines.size - distance] == afterLines[afterLines.size - distance]
        }.size
        val contextStart = (commonPrefix - unifiedDiffContext).coerceAtLeast(0)
        val oldChangeEnd = beforeLines.size - commonSuffix
        val newChangeEnd = afterLines.size - commonSuffix
        val oldContextEnd = (oldChangeEnd + unifiedDiffContext).coerceAtMost(beforeLines.size)
        val newContextEnd = (newChangeEnd + unifiedDiffContext).coerceAtMost(afterLines.size)
        val leadingContext = beforeLines.subList(contextStart, commonPrefix)
        val trailingContext = beforeLines.subList(oldChangeEnd, oldContextEnd)
        return buildString {
            appendLine("--- a/${relativePath}")
            appendLine("+++ b/${relativePath}")
            appendLine(
                "@@ -${contextStart + 1},${oldContextEnd - contextStart} " +
                        "+${contextStart + 1},${newContextEnd - contextStart} @@",
            )
            leadingContext.forEach { line -> appendLine(" ${line}") }
            beforeLines.subList(commonPrefix, oldChangeEnd).forEach { line -> appendLine("-${line}") }
            afterLines.subList(commonPrefix, newChangeEnd).forEach { line -> appendLine("+${line}") }
            trailingContext.forEach { line -> appendLine(" ${line}") }
        }
    }

    private fun String.numericType(): String = when {
        endsWith("L", ignoreCase = true) -> "Long"
        endsWith("F", ignoreCase = true) -> "Float"
        contains('.') -> "Double"
        else -> "Int"
    }

    private fun String.inferNumericType(range: IntRange, literal: String): String {
        val surroundingPrefix = take(range.first).takeLast(expectedTypeWindow)
        val surroundingSuffix = drop(range.last + 1).take(expectedTypeWindow)
        Regex("(?:[:(,]\\s*)(Byte|Short|Int|Long|Float|Double)\\s*=\\s*${'$'}")
            .find(surroundingPrefix)
            ?.groupValues
            ?.get(1)
            ?.let { return it }
        Regex("^\\s*\\.to(Byte|Short|Int|Long|Float|Double)\\s*\\(")
            .find(surroundingSuffix)
            ?.groupValues
            ?.get(1)
            ?.let { return it }
        val signatures = functionSignaturePattern.findAll(this).associate { match ->
            match.groupValues[1] to match.groupValues[2]
                .split(',')
                .mapNotNull { parameter ->
                    parameter.substringAfter(':', "").trim().substringBefore('<').takeIf(String::isNotEmpty)
                }
        }
        val call = functionCallPrefixPattern.find(surroundingPrefix)
        val functionName = call?.groupValues?.get(1)
        val argumentIndex = call?.groupValues?.get(2)?.count { character -> character == ',' } ?: 0
        return signatures[functionName]?.getOrNull(argumentIndex)
            ?.takeIf { type -> type in numericTypes }
            ?: literal.numericType()
    }

    private fun String.toNormalizedNumber(): Double? {
        val normalized = replace("_", "")
            .removeSuffix("L")
            .removeSuffix("l")
            .removeSuffix("F")
            .removeSuffix("f")
        return when {
            normalized.startsWith("0x", ignoreCase = true) -> normalized.drop(2).toLongOrNull(16)?.toDouble()
            normalized.startsWith("0b", ignoreCase = true) -> normalized.drop(2).toLongOrNull(2)?.toDouble()
            else -> normalized.toDoubleOrNull()
        }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        private const val schemaVersion: Int = 1
        private const val fileToPathAction: String = "file-to-path"
        private const val magicNumberAction: String = "magic-number"
        private const val resultAction: String = "result-handling"
        private const val coroutineAction: String = "coroutine-preference"
        private const val miniMessageAction: String = "minimessage-text"
        private const val tryCatchAction: String = "forbidden-try-catch"
        private const val expectedTypeWindow: Int = 240
        private const val unifiedDiffContext: Int = 3
        private val sourceExtensions: Set<String> = setOf("kt", "kts")
        private val ignoredDirectories: Set<String> = setOf(".git", ".gradle", ".idea", "build", "out")
        private val allowedNumbers: Set<Double?> = setOf(0.0, 1.0, 2.0)
        private val constantNamePattern: Regex = Regex("[A-Z][A-Z0-9_]*")
        private val identifierPattern: Regex = Regex("[A-Za-z_][A-Za-z0-9_]*")
        private val numericTypes: Set<String> = setOf("Byte", "Short", "Int", "Long", "Float", "Double")
        private val resultModes: Set<String> = setOf("discard", "fallback", "on-failure")
        private val localizationModes: Set<String> = setOf("inline", "key")
        private val tryCatchModes: Set<String> = setOf("discard", "fallback", "rethrow-unmatched")
        private val functionSignaturePattern: Regex = Regex("fun\\s+(\\w+)\\s*\\(([^)]*)\\)")
        private val functionCallPrefixPattern: Regex = Regex("(\\w+)\\s*\\(([^()]*)${'$'}")
        private val semanticTagPattern: Regex = Regex("[a-z][a-z0-9_-]*")
        private val fileImportPattern: Regex = Regex("(?m)^import java\\.io\\.File${'$'}")
        private val containsFileUsagePattern: Regex = Regex("(?:java\\.io\\.)?File\\b")
        private val magicNumberPattern: Regex = Regex(
            "(?<![A-Za-z0-9_])(?:0[xX][0-9A-Fa-f](?:_?[0-9A-Fa-f])*|0[bB][01](?:_?[01])*|" +
                    "(?:[1-9][0-9]*)(?:_[0-9]+)*(?:\\.[0-9]+)?)[fFlL]?",
        )
        private val ignoredResultPattern: Regex = Regex("runCatching\\s*\\{[^{}]*}(?!\\s*\\.)")
        private val threadSleepPattern: Regex = Regex("(?:java\\.lang\\.)?Thread\\.sleep\\s*\\([^)]*\\)")
        private val simpleThreadPattern: Regex = Regex("Thread\\s*\\{([^{}]*)}\\s*\\.start\\s*\\(\\s*\\)")
        private val unsupportedCoroutinePattern: Regex = Regex(
            "(?:Thread|Timer)\\s*(?:\\(|\\{)|Executors\\.new[A-Za-z]+\\s*\\(",
        )
        private val legacyTextPattern: Regex = Regex("\"[^\"\\n]*(?:§.|&[0-9A-FK-ORa-fk-or])[^\"\\n]*\"")
        private val legacyColorPattern: Regex = Regex("(?:§.|&[0-9A-FK-ORa-fk-or])")
        private val stringTemplatePattern: Regex = Regex("\\$\\{?([A-Za-z_][A-Za-z0-9_]*)}?")
        private val simpleTryCatchPattern: Regex = Regex(
            "try\\s*\\{([^{}]*)}\\s*catch\\s*\\(\\s*(\\w+)\\s*:\\s*([A-Za-z0-9_.]+)\\s*\\)\\s*\\{([^{}]*)}",
        )
        private val unsafeTryCatchBodyPattern: Regex = Regex("\\b(?:return|throw)\\b")
    }
}

/** Assisted-fix preview plus staged replacements. */
internal data class MineKotAssistPreview(
    val report: MineKotAssistReport,
    val replacements: Map<java.nio.file.Path, String>,
    val diff: String,
)

private data class InternalCandidate(
    val findingId: String,
    val action: String,
    val path: java.nio.file.Path,
    val range: IntRange,
    val line: Int,
    val requiredOptions: List<String>,
    val message: String,
    val unsupportedReason: String?,
    val numericType: String?,
    val requiresPlaceholders: Boolean,
)

private data class ReadyApplication(
    val candidate: InternalCandidate,
    val options: Map<String, String>,
)
