package org.jetbrains.kotlin.script.examples.shell.parser

data class UsageSection(
    val usages: List<Usage>
) {

    data class Usage(val components: List<Component>) {
        sealed class Component {
            data class Word(val word: Token.Word) : Component()
            data class Placeholder(val placeholder: Token.Placeholder) : Component()
            data class Group(val placeholder: Token.Group) : Component()
            data class Option(val option: Token.Option) : Component()
            data class Optional(val optional: Usage) : Component()
            data class Choices(val choices: List<Usage>) : Component()
        }
    }

    companion object {
        fun fromSection(section: OverviewDescription.Section): UsageSection? {
            assert(section.name.word.toLowerCase() == "usage")

            val lines = section.lines.map { Scanner(it.line) }
            val usages = lines.map { scanner ->
                scanner.takeUsage()?.takeIf { scanner.hasFinished() } ?: return null
            }
            return UsageSection(usages)
        }
    }
}

private fun Scanner.takeUsage() = collect { takeComponent() }
    .takeIf { it.isNotEmpty() }
    ?.let { UsageSection.Usage(it) }

private fun Scanner.takeComponent(): UsageSection.Usage.Component? {
    takeGroup()?.let { return UsageSection.Usage.Component.Group(it) }

    takeWord()?.let { return UsageSection.Usage.Component.Word(it) }
    takePlaceholder()?.let { return UsageSection.Usage.Component.Placeholder(it) }
    takeOption()?.let { return UsageSection.Usage.Component.Option(it) }

    tryLookahead {
        takeOptionalStart() ?: return@tryLookahead null

        val choices = collect {
            takeUsage()
                .also { takeSeparator() }
        }.takeIf { it.isNotEmpty() } ?: return@tryLookahead null

        val usage = if (choices.count() == 1)
            choices.first()
        else
            UsageSection.Usage(
                listOf(
                    UsageSection.Usage.Component.Choices(choices)
                )
            )

        takeOptionalEnd() ?: return@tryLookahead null
        usage
    }?.let { return UsageSection.Usage.Component.Optional(it) }

    tryLookahead {
        takeChoicesStart() ?: return@tryLookahead null

        val choices = collect {
            takeUsage()
                .also { takeSeparator() }
        }.takeIf { it.isNotEmpty() } ?: return@tryLookahead null

        takeChoicesEnd() ?: return@tryLookahead null
        choices
    }?.let { return UsageSection.Usage.Component.Choices(it) }

    return null
}