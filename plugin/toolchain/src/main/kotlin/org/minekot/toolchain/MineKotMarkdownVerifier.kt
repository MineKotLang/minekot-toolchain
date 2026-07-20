package org.minekot.toolchain

import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.*
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser
import java.io.File

/** Validates deterministic MineKot Markdown requirements. */
internal object MineKotMarkdownVerifier {
    /** Returns path-prefixed Markdown violations. */
    fun violations(
        file: File,
        root: File,
    ): List<String> {
        val relativePath = file.relativeTo(root).invariantSeparatorsPath
        val source = file.readText()
        val lines = source.lines().dropLastWhile(String::isEmpty)
        val excludedLines = formatterExcludedLines(lines)
        val document = markdownParser.parse(source)
        return buildList {
            addAll(lineViolations(relativePath, lines, excludedLines))
            addAll(documentViolations(relativePath, file, document, lines, excludedLines))
        }
    }

    private fun lineViolations(
        path: String,
        lines: List<String>,
        excludedLines: Set<Int>,
    ): List<String> = buildList {
        var fenced = false
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            if (lineNumber in excludedLines) {
                return@forEachIndexed
            }
            if (fencePattern.matches(line)) {
                fenced = !fenced
                if (fenced && fenceLanguagePattern.matchEntire(line)?.groupValues?.get(1).isNullOrBlank()) {
                    add("${path}:${lineNumber}: fenced code blocks require a language identifier")
                }
                return@forEachIndexed
            }
            if (line.length > markdownColumnLimit) {
                add("${path}:${lineNumber}: Markdown lines must not exceed ${markdownColumnLimit} characters")
            }
            if ('\t' in line) {
                add("${path}:${lineNumber}: use spaces instead of tabs")
            }
            if (!fenced) {
                if (unorderedListPattern.containsMatchIn(line)) {
                    add("${path}:${lineNumber}: unordered lists must use hyphens")
                }
                if (orderedParenthesisPattern.containsMatchIn(line)) {
                    add("${path}:${lineNumber}: ordered lists must use a number followed by a period")
                }
                if (setextHeadingPattern.matches(line) && index > 0 && lines[index - 1].isNotBlank()) {
                    add("${path}:${lineNumber}: use ATX-style headings")
                }
                if (underscoreEmphasisPattern.containsMatchIn(line)) {
                    add("${path}:${lineNumber}: use asterisks for emphasis")
                }
                if (indentedCodePattern.containsMatchIn(line)) {
                    add("${path}:${lineNumber}: use fenced code blocks with a language identifier")
                }
                if (tableSeparatorPattern.matches(line)) {
                    val cells = line.trim().removeSurrounding("|").split('|').map(String::trim)
                    if (cells.any { cell -> !cell.startsWith(':') && !cell.endsWith(':') }) {
                        add("${path}:${lineNumber}: specify alignment for every Markdown table column")
                    }
                }
            }
        }
    }

    private fun documentViolations(
        path: String,
        file: File,
        document: Node,
        lines: List<String>,
        excludedLines: Set<Int>,
    ): List<String> = buildList {
        var previousHeadingLevel = 0
        document.accept(
            object : AbstractVisitor() {
                override fun visit(heading: Heading) {
                    val line = heading.lineNumber() ?: return
                    if (line in excludedLines) {
                        return
                    }
                    if (previousHeadingLevel > 0 && heading.level > previousHeadingLevel + 1) {
                        add("${path}:${line}: heading hierarchy must not skip levels")
                    }
                    previousHeadingLevel = heading.level
                    if (line > 1 && lines.getOrNull(line - 2)?.isNotBlank() == true) {
                        add("${path}:${line}: place one blank line before headings")
                    }
                    if (lines.getOrNull(line)?.isNotBlank() == true) {
                        add("${path}:${line}: place one blank line after headings")
                    }
                    val title = heading.plainText()
                    if (title.isNotBlank() && title == title.uppercase() && title.any(Char::isLetter)) {
                        add("${path}:${line}: headings must use sentence case")
                    }
                    visitChildren(heading)
                }

                override fun visit(paragraph: Paragraph) {
                    val spans = paragraph.sourceSpans
                    val first = spans.minOfOrNull { span -> span.lineIndex }
                    val last = spans.maxOfOrNull { span -> span.lineIndex }
                    if (first != null && last != null && first != last && (first + 1..last + 1).none(excludedLines::contains)) {
                        add("${path}:${first + 1}: paragraph text must not contain hard source wrapping")
                    }
                    visitChildren(paragraph)
                }

                override fun visit(link: Link) {
                    val line = link.lineNumber() ?: return
                    if (line !in excludedLines) {
                        val label = link.plainText().trim().lowercase()
                        if (label in nonDescriptiveLinkLabels) {
                            add("${path}:${line}: link text must describe its destination")
                        }
                        if (link.destination.startsWith('#')) {
                            val anchors = document.headingAnchors()
                            if (link.destination.removePrefix("#") !in anchors) {
                                add("${path}:${line}: internal heading link ${link.destination} does not exist")
                            }
                        } else if (link.destination.isLocalPath()) {
                            val target = file.parentFile.resolve(link.destination.substringBefore('#')).normalize()
                            if (!target.exists()) {
                                add("${path}:${line}: local link target ${link.destination} does not exist")
                            }
                        }
                    }
                    visitChildren(link)
                }

                override fun visit(image: Image) {
                    val line = image.lineNumber() ?: return
                    if (line !in excludedLines) {
                        val alt = image.plainText().trim()
                        if (alt.length < minimumDescriptiveTextLength || alt.equals(
                                File(image.destination).name,
                                true,
                            )
                        ) {
                            add("${path}:${line}: image alt text must describe image purpose")
                        }
                        if (image.destination.startsWith('/')) {
                            add("${path}:${line}: repository images must use relative paths")
                        }
                    }
                    visitChildren(image)
                }

                override fun visit(blockQuote: BlockQuote) {
                    addSpacingViolation(path, blockQuote, lines, excludedLines, "blockquotes", this@buildList)
                    visitChildren(blockQuote)
                }

                override fun visit(thematicBreak: ThematicBreak) {
                    val line = thematicBreak.lineNumber() ?: return
                    if (line !in excludedLines && lines.getOrNull(line - 1)?.trim()
                            ?.any { character -> character != '-' } == true
                    ) {
                        add("${path}:${line}: horizontal rules must use hyphens")
                    }
                    addSpacingViolation(path, thematicBreak, lines, excludedLines, "horizontal rules", this@buildList)
                    visitChildren(thematicBreak)
                }

                override fun visit(bulletList: BulletList) {
                    visitChildren(bulletList)
                }

                override fun visit(orderedList: OrderedList) {
                    visitChildren(orderedList)
                }

                override fun visit(fencedCodeBlock: FencedCodeBlock) {
                    visitChildren(fencedCodeBlock)
                }
            },
        )
    }

    private fun addSpacingViolation(
        path: String,
        node: Node,
        lines: List<String>,
        excludedLines: Set<Int>,
        subject: String,
        violations: MutableList<String>,
    ) {
        val first = node.sourceSpans.minOfOrNull { span -> span.lineIndex } ?: return
        val last = node.sourceSpans.maxOfOrNull { span -> span.lineIndex } ?: return
        val line = first + 1
        if (line in excludedLines) {
            return
        }
        if (first > 0 && lines.getOrNull(first - 1)?.isNotBlank() == true || lines.getOrNull(last + 1)
                ?.isNotBlank() == true
        ) {
            violations.add("${path}:${line}: place one blank line before and after ${subject}")
        }
    }

    private fun formatterExcludedLines(lines: List<String>): Set<Int> = buildSet {
        var excluded = false
        lines.forEachIndexed { index, line ->
            if (formatterOff in line) {
                excluded = true
            }
            if (excluded) {
                add(index + 1)
            }
            if (formatterOn in line) {
                excluded = false
            }
        }
    }

    private fun Node.headingAnchors(): Set<String> = buildSet {
        accept(
            object : AbstractVisitor() {
                override fun visit(heading: Heading) {
                    add(
                        heading.plainText().lowercase()
                            .replace(nonAnchorCharacters, "")
                            .trim()
                            .replace(whitespacePattern, "-"),
                    )
                    visitChildren(heading)
                }
            },
        )
    }

    private fun Node.plainText(): String = buildString {
        this@plainText.accept(
            object : AbstractVisitor() {
                override fun visit(text: Text) {
                    append(text.literal)
                }
            },
        )
    }

    private fun Node.lineNumber(): Int? = sourceSpans.minOfOrNull { span -> span.lineIndex + 1 }

    private fun String.isLocalPath(): Boolean =
        isNotBlank() && !startsWith('#') && !contains("://") && !startsWith("mailto:")

    private const val markdownColumnLimit: Int = 999
    private const val minimumDescriptiveTextLength: Int = 5
    private const val formatterOff: String = "@formatter:off"
    private const val formatterOn: String = "@formatter:on"
    private val markdownParser: Parser = Parser.builder()
        .extensions(listOf(TablesExtension.create()))
        .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
        .build()
    private val fencePattern: Regex = Regex("^\\s*(`{3,}|~{3,}).*")
    private val fenceLanguagePattern: Regex = Regex("^\\s*(?:`{3,}|~{3,})([A-Za-z0-9_+-]*)\\s*")
    private val unorderedListPattern: Regex = Regex("^\\s*[*+]\\s+")
    private val orderedParenthesisPattern: Regex = Regex("^\\s*\\d+\\)\\s+")
    private val setextHeadingPattern: Regex = Regex("^\\s*(?:=+|-+)\\s*")
    private val underscoreEmphasisPattern: Regex = Regex("(?<![A-Za-z0-9])_{1,2}[^_\\n]+_{1,2}(?![A-Za-z0-9])")
    private val indentedCodePattern: Regex = Regex("^(?: {4}|\\t)\\S")
    private val tableSeparatorPattern: Regex = Regex("^\\s*\\|?\\s*:?-{3,}:?(?:\\s*\\|\\s*:?-{3,}:?)+\\s*\\|?\\s*")
    private val nonAnchorCharacters: Regex = Regex("[^a-z0-9 _-]")
    private val whitespacePattern: Regex = Regex("\\s+")
    private val nonDescriptiveLinkLabels: Set<String> = setOf("click here", "here", "link", "more")
}
