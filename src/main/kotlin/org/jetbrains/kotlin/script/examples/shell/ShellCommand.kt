package org.jetbrains.kotlin.script.examples.shell

import org.jetbrains.kotlin.script.examples.shell.parser.*

data class ShellCommand(
    val name: List<Token.Word>,
    val subCommands: List<ShellCommand>,
    val options: List<Option>
) {
    sealed class Option {
        abstract val names: List<Token.Option>
        abstract val mandatory: Boolean
        data class Flag(override val names: List<Token.Option>, override val mandatory: Boolean) : Option()
        data class Value(override val names: List<Token.Option>, override val mandatory: Boolean, val choices: List<String>?) : Option()
    }

    companion object {
        @ExperimentalStdlibApi
        fun takeFrom(scanner: Scanner) = OverviewDescription.takeFrom(scanner)?.let { takeFrom(it) }

        @ExperimentalStdlibApi
        private fun takeFrom(description: OverviewDescription): ShellCommand? {
            val section = description
                .sections
                .firstOrNull { it.isUsageSection } ?: return null

            val usageSection = UsageSection.fromSection(section) ?: return null
            val name = usageSection.usages.first().components.takeWhile { it is UsageSection.Usage.Component.Word }.map { (it as UsageSection.Usage.Component.Word).word }

            val optionsFromUsage = usageSection.usages.map { it.options() }.merge()
            val otherSections = description.sections.filter { !it.isUsageSection }
            val options = section.lines.mapNotNull { Scanner(it.line).takeOptionValue() }

            TODO()
        }
    }
}

private fun List<List<ShellCommand.Option>>.merge(): List<ShellCommand.Option> {
    val numberOfChoices = count()
    return flatten()
        .groupBy { it.names }
        .map { entry ->
            val mandatory =
                entry.value.all { it.mandatory } && entry.value.count() == numberOfChoices
            if (mandatory) {
                entry.value.first()
            } else {
                entry.value.first().notMandatory()
            }
        }
}

@ExperimentalStdlibApi
private fun UsageSection.Usage.options(): List<ShellCommand.Option> {
    val list = mutableListOf<ShellCommand.Option>()
    components.forEach { component ->
        val options =  component
            .options()
            .takeIf { it.isNotEmpty() }

        if (options != null) {
            list.addAll(options)
        } else {
            when (component) {
                is UsageSection.Usage.Component.Word -> TODO()
                is UsageSection.Usage.Component.Placeholder -> {
                    val last = list.removeLast()

                }
                is UsageSection.Usage.Component.Group -> Unit
                is UsageSection.Usage.Component.Option -> Unit
                is UsageSection.Usage.Component.Optional -> Unit
                is UsageSection.Usage.Component.Choices -> Unit
            }
        }
    }
    return list
}

@ExperimentalStdlibApi
private fun UsageSection.Usage.Component.options(): List<ShellCommand.Option> = when (this) {
    is UsageSection.Usage.Component.Word -> emptyList()
    is UsageSection.Usage.Component.Placeholder -> emptyList()
    is UsageSection.Usage.Component.Group -> emptyList()
    is UsageSection.Usage.Component.Option -> listOf(ShellCommand.Option.Flag(names = listOf(option), mandatory = true))
    is UsageSection.Usage.Component.Optional -> optional.options().map { it.notMandatory() }
    is UsageSection.Usage.Component.Choices -> choices.map { it.options() }.merge()
}

private fun ShellCommand.Option.notMandatory() = when (this) {
    is ShellCommand.Option.Flag -> copy(mandatory = false)
    is ShellCommand.Option.Value -> copy(mandatory = false)
}

private fun Scanner.takeOptionValue(): ShellCommand.Option? {
    val first = takeOption() ?: return null
    val alternatives = collect {
        takeComma() ?: return@collect null
        takeOption()
    }

    val names = listOf(first) + alternatives

    val argument = tryLookahead {
        takePlaceholder()?.let { return@tryLookahead it }
        takeWord()?.takeIf { it.word.toLowerCase() == it.word }
    }

    takeSpacing()
    val rest = takeLine()
    print(rest?.line)

    return if (argument != null) {
        // TODO: Handle choices
        ShellCommand.Option.Value(names, false, null)
    } else {
        ShellCommand.Option.Flag(names, false)
    }
}