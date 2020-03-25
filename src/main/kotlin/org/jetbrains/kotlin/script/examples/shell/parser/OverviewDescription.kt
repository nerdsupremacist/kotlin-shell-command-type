package org.jetbrains.kotlin.script.examples.shell.parser

data class OverviewDescription(
    val freeLines: List<Token.Line>,
    val sections: List<Section>
) {
    data class Section(
        val name: Token.Word,
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
    val sectionName = takeWord() ?: return null
    takeColon() ?: return null

    takeEmptyLine()

    val lines = collect {
        takeLine()
    }.takeIf { it.isNotEmpty() } ?: return null

    if (takeEmptyLine() == null && !hasFinished()) {
        return null
    }

    return OverviewDescription.Section(
        name = sectionName,
        lines = lines
    )
}