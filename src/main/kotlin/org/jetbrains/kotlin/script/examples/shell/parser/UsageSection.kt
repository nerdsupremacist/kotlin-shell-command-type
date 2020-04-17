package org.jetbrains.kotlin.script.examples.shell.parser

data class UsageSection(
    val section: OverviewDescription.Section,
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
            object LeaveEmpty : Component()
        }
    }

    companion object {
        fun fromSection(section: OverviewDescription.Section): UsageSection? {
            assert(section.title is Token.Word && section.title.word.toLowerCase() == "usage")

            val lines = section.lines.map { Scanner(it.line) }
            val usages = mutableListOf<Usage>()
            lines.forEach { scanner ->
                val usage = scanner.takeUsage()?.takeIf { scanner.hasFinished() } ?: return null
                if (usages.isNotEmpty() && usage.components.firstOrNull() !is Usage.Component.Word) {
                    val index = usages.lastIndex
                    usages[index] = Usage(usages[index].components + usage.components)
                } else {
                    usages.add(usage)
                }
            }

            return UsageSection(section, usages)
        }
    }
}

private fun Scanner.takeUsage() = collect { takeComponent() }
    .takeIf { it.isNotEmpty() }
    ?.let { UsageSection.Usage(it) }

private fun Scanner.takeComponent(): UsageSection.Usage.Component? {
    tryLookahead {
        val choices = collect {
            takeLeadingComponent()
        }.takeIf { it.isNotEmpty() } ?: return@tryLookahead null

        val last = takeSingleComponent() ?: return@tryLookahead null
        (choices + last).map { UsageSection.Usage(listOf(it)) }
    }?.let { return UsageSection.Usage.Component.Choices(it) }

    return takeSingleComponent()
}

private fun Scanner.takeLeadingComponent(): UsageSection.Usage.Component? = tryLookahead {
    val component = takeSingleComponent()
    takeSeparator() ?: return@tryLookahead null
    component
}


private fun Scanner.takeSingleComponent(): UsageSection.Usage.Component? {
    takeGroup()?.let { return UsageSection.Usage.Component.Group(it) }

    takePlaceholder()?.let { return UsageSection.Usage.Component.Placeholder(it) }
    takeWord()?.let { return UsageSection.Usage.Component.Word(it) }
    takeOption()?.let { return UsageSection.Usage.Component.Option(it) }

    takeLeaveEmpty()?.let { return UsageSection.Usage.Component.LeaveEmpty }

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