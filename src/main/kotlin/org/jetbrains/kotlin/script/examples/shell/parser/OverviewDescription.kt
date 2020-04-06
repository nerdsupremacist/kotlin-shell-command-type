package org.jetbrains.kotlin.script.examples.shell.parser

data class OverviewDescription(
    val freeLines: List<Token.Line>,
    val sections: List<Section>
) {
    data class Section(
        val title: Token?,
        val lines: List<Token.Line>
    )

    companion object {
        fun takeFrom(scanner: Scanner) = scanner.tryLookahead { takeOverviewDescription() }
    }
}

private fun Scanner.takeOverviewDescription(): OverviewDescription? {
    val items = collect {
        skipLines {
            takeSection()
        }
    }.takeIf { it.isNotEmpty() } ?: return null

    val freeLines = items.flatMap { it.first?.takeLines() ?: emptyList() } + takeLines()
    val sections = items.map { it.second }

    return OverviewDescription(freeLines = freeLines, sections = sections)
}

private fun Scanner.takeLines() = collect {
    takeLine() ?: takeEmptyLine()?.let { Token.Line("") }
}

private fun Scanner.takeSection(): OverviewDescription.Section? {
    val title = tryLookahead {
        val word = takeWord() ?: return@tryLookahead null
        takeColon() ?: return@tryLookahead null
        word
    } ?: takeLine()

    takeEmptyLine()

    val lines = collect {
        takeSpacing() ?: return@collect null
        takeLine()
    }.takeIf { it.isNotEmpty() } ?: return null

    if (takeEmptyLine() == null && !hasFinished()) {
        return null
    }

    return OverviewDescription.Section(
        title = title,
        lines = lines
    )
}