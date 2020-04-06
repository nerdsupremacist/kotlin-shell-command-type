package org.jetbrains.kotlin.script.examples.shell.parser

private val wordRegex = Regex("^[^\\S\\r\\n]*([a-zA-Z][a-zA-Z0-9-_]*)\\b")
private val lineRegex = Regex("^[^\\S\\r\\n]*(.+)(\n|$)")
private val optionRegex = Regex("^[^\\S\\r\\n]*-{1,2}([a-zA-Z][a-zA-Z0-9-_]*)\\b")
private val placeholderRegex = Regex("^[^\\S\\r\\n]*(?:(?:<([a-zA-Z][a-zA-Z0-9-]*)>)|([A-Z][A-Z-]*))")
private val ellipsisRegex = Regex("^[^\\S\\r\\n]*\\.{3}")
private val spacingRegex = Regex("^([^\\S\\r\\n]+)")
private val colonRegex = Regex("^[^\\S\\r\\n]*:")
private val commaRegex = Regex("^[^\\S\\r\\n]*,")
private val emptyLineRegex = Regex("^([^\\S\\r\\n]*\\n)+")
private val choicesStartRegex = Regex("^[^\\S\\r\\n]*\\(")
private val choicesEndRegex = Regex("^[^\\S\\r\\n]*\\)")
private val separatorRegex = Regex("^[^\\S\\r\\n]*\\|")
private val optionalsStartRegex = Regex("^[^\\S\\r\\n]*\\[")
private val optionalsEndRegex = Regex("^[^\\S\\r\\n]*]")

class Scanner(text: String) {
    private var consumed = ""
    private var remaining = text

    fun <T> tryLookahead(scan: Scanner.() -> T?): T? {
        val scanner = Scanner(text = remaining)
        return scanner
            .scan()
            ?.apply {
                consumed += scanner.consumed
                remaining = scanner.remaining
            }
    }

    private fun <T> skipTextLines(until: Scanner.() -> T?): Pair<String?, T>? {
        if (hasFinished()) {
            // Last try!
            return until()?.let { null to it }
        }

        val value = tryLookahead(until)

        if (value == null) {
            val line = takeLine()?.line ?: takeEmptyLine()?.let { "" } ?: return null
            return skipTextLines(until)
                ?.let { result ->
                    val text = result.first?.let { line + "\n" + it } ?: line
                    text to result.second
                }
        }

        return null to value
    }

    fun <T> skipLines(until: Scanner.() -> T?) = skipTextLines(until)?.let { it.first?.let(::Scanner) to it.second }

    private fun take(regex: Regex) = regex
        .find(remaining)
        ?.also { match ->
            consumed += match.value
            remaining = remaining.removePrefix(match.value)
        }

    fun takeWord() = take(wordRegex)?.let { Token.Word(it.groups[1]!!.value) }

    fun takeLine() = take(lineRegex)?.let { Token.Line(it.groups[1]!!.value) }

    fun takeOption() = take(optionRegex)?.let { Token.Option(it.groups[1]!!.value) }

    fun takePlaceholder() = take(placeholderRegex)?.let { Token.Placeholder(it.groups[1]?.value ?: it.groups[2]!!.value) }

    fun takeGroup() = tryLookahead {
        val placeholder = takePlaceholder() ?: return@tryLookahead null
        take(ellipsisRegex)?.let { Token.Group(placeholder) }
    }

    fun takeSpacing() = take(spacingRegex)?.let { Token.Spacing(it.groups[1]!!.value.length) }

    fun takeColon() = take(colonRegex)?.let { Token.Colon }

    fun takeComma() = take(commaRegex)?.let { Token.Comma }

    fun takeEmptyLine() = take(emptyLineRegex)?.let { Token.EmptyLine }

    fun takeChoicesStart() = take(choicesStartRegex)?.let { Token.ChoicesStart }

    fun takeChoicesEnd() = take(choicesEndRegex)?.let { Token.ChoicesEnd }

    fun takeSeparator() = take(separatorRegex)?.let { Token.Separator }

    fun takeOptionalStart() = take(optionalsStartRegex)?.let { Token.OptionalStart }

    fun takeOptionalEnd() = take(optionalsEndRegex)?.let { Token.OptionalEnd }

    fun hasFinished(): Boolean = remaining.isBlank()
}


