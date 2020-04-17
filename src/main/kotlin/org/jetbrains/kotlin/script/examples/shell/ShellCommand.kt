package org.jetbrains.kotlin.script.examples.shell

import eu.jrie.jetbrains.kotlinshell.shell.shell
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.kotlin.script.examples.shell.parser.*
import org.jetbrains.kotlin.script.examples.shell.utils.toCamelCase
import org.jetbrains.kotlin.script.examples.shell.utils.toUpperCamelCase

data class ShellCommand(
    val name: List<Token.Word>,
    val subCommands: List<ShellCommand>,
    val options: List<Option>
) {
    sealed class Option {
        abstract val names: List<Token.Option>
        abstract val mandatory: Boolean
        data class Flag(override val names: List<Token.Option>, override val mandatory: Boolean) : Option()
        data class Value(override val names: List<Token.Option>, override val mandatory: Boolean, val inline: Boolean = false) : Option()
    }

    companion object
}

// Code Generation

fun ShellCommand.code(): String = buildString {
    val interfaceName = name.joinToString("") { it.word }.toUpperCamelCase() + "CommandBuilder"

    val parentInterfaceName = name
        .dropLast(1)
        .takeIf { it.isNotEmpty() }
        ?.joinToString("") { it.word }
        ?.let { it.toUpperCamelCase() + "CommandBuilder" }

    append("interface $interfaceName")
    parentInterfaceName?.let { appendln(" : $it") } ?: appendln()

    val distinctOptions = options.distinctBy { it.names }
    val mandatory = distinctOptions.filter { it.mandatory }
    val optionalOptions = distinctOptions.filter { !it.mandatory }

    val commandName = name.joinToString(" ") { it.word }
    val singleName = name.last().word.toCamelCase()

    if (parentInterfaceName != null) {
        if (mandatory.isNotEmpty()) {
            val optionsList = mandatory
                .joinToString(", ") { option ->
                    val usedName = option.names.maxBy { it.option.length } ?: TODO()
                    val optionName = if (usedName.option.length > 1) usedName.option.toCamelCase() else usedName.option
                    when (option) {
                        is ShellCommand.Option.Flag -> "$optionName: Boolean"
                        is ShellCommand.Option.Value -> "$optionName: String"
                    }
                }

            val optionsAssignment = mandatory
                .joinToString(", ") { option ->
                    val usedName = option.names.maxBy { it.option.length } ?: TODO()
                    val optionName = if (usedName.option.length > 1) usedName.option.toCamelCase() else usedName.option
                    when (option) {
                        is ShellCommand.Option.Flag -> "if ($optionName) \"${usedName.dashes}${usedName.option}\" else \"\""
                        is ShellCommand.Option.Value -> if (option.inline) "$optionName" else "\"${usedName.dashes}${usedName.option}\${$optionName}\""
                    }
                }

            appendln("@JvmName(\"get$interfaceName\") fun TypeSafeCommand<$parentInterfaceName>.$singleName($optionsList) = TypeSafeCommand<$interfaceName>(\"$commandName\", $optionsAssignment)")
        } else {
            appendln("val TypeSafeCommand<$parentInterfaceName>.$singleName @JvmName(\"get$interfaceName\")  get() = TypeSafeCommand<$interfaceName>(\"$commandName\")")
        }
    } else {
        if (mandatory.isNotEmpty()) {
            val optionsList = mandatory
                .joinToString(", ") { option ->
                    val usedName = option.names.maxBy { it.option.length } ?: TODO()
                    val optionName = if (usedName.option.length > 1) usedName.option.toCamelCase() else usedName.option
                    when (option) {
                        is ShellCommand.Option.Flag -> "$optionName: Boolean"
                        is ShellCommand.Option.Value -> "$optionName: String"
                    }
                }

            val optionsAssignment = mandatory
                .joinToString(", ") { option ->
                    val usedName = option.names.maxBy { it.option.length } ?: TODO()
                    val optionName = if (usedName.option.length > 1) usedName.option.toCamelCase() else usedName.option
                    when (option) {
                        is ShellCommand.Option.Flag -> "if ($optionName) \"${usedName.dashes}${usedName.option}\" else \"\""
                        is ShellCommand.Option.Value -> if (option.inline) "$optionName" else "\"${usedName.dashes}${usedName.option}\${$optionName}\""
                    }
                }

            appendln("fun $singleName($optionsList) = TypeSafeCommand<$interfaceName>(\"$commandName\", $optionsAssignment)")
        } else {
            appendln("val $singleName = TypeSafeCommand<$interfaceName>(\"$commandName\")")
        }
    }

    optionalOptions.forEach { option ->
        val usedName = option.names.maxBy { it.option.length } ?: return@forEach
        val optionName = if (usedName.option.length > 1) usedName.option.toCamelCase() else usedName.option
        when (option) {
            is ShellCommand.Option.Flag ->
                appendln("val <T : $interfaceName> CommandBuilder<T>.$optionName: Flag @JvmName(\"get$interfaceName${usedName.dashes}${usedName.option}\") get() = Flag(\"${usedName.dashes}${usedName.option}\")")
            is ShellCommand.Option.Value ->
                appendln("val <T : $interfaceName> CommandBuilder<T>.$optionName: Value @JvmName(\"get$interfaceName${usedName.dashes}${usedName.option}\") get() = Value(\"${usedName.dashes}${usedName.option}\")")
        }
    }

    subCommands.forEach { subCommand ->
        appendln(subCommand.code())
    }
}

// Parsing

@ExperimentalStdlibApi
@ExperimentalCoroutinesApi
suspend fun ShellCommand.Companion.fromName(name: String): ShellCommand? {
    var message = ""
    shell {
        pipeline {
            "$name --help".process() pipe stringLambda { string ->
                message += string
                "" to ""
            }
        }.join()
    }

    val scanner = Scanner(message)
    return takeFrom(scanner)
}

@ExperimentalStdlibApi
suspend fun ShellCommand.Companion.takeFrom(scanner: Scanner) = OverviewDescription.takeFrom(scanner)?.let { takeFrom(it) }

@ExperimentalCoroutinesApi
@ExperimentalStdlibApi
private suspend fun ShellCommand.Companion.takeFrom(description: OverviewDescription): ShellCommand? {
    val section = description
        .sections
        .firstOrNull { it.isUsageSection } ?: return null

    val usageSection = UsageSection.fromSection(section)
        ?: return null
    val name = usageSection.usages.first().components.takeWhile { it is UsageSection.Usage.Component.Word }.map { (it as UsageSection.Usage.Component.Word).word }
    val optionsFromUsage = usageSection.usages.map { it.options() }.merge()
    val otherSections = description.sections.filter { !it.isUsageSection }
    val options = otherSections.flatMap { section -> section.lines.mapNotNull { Scanner(it.line).takeOptionValue() } }
    val subCommands = otherSections
        .flatMap { section ->
            section.lines.mapNotNull { Scanner(it.line).takeSubcommand() }
        }
        .mapNotNull { subCommand -> fromName(name = (name + subCommand).joinToString(" ") { it.word }) }

    return ShellCommand(name, subCommands = subCommands, options = optionsFromUsage + options)
}

private fun List<List<ShellCommand.Option>>.merge(): List<ShellCommand.Option> {
    val numberOfChoices = count()

    if (numberOfChoices <= 1)
        return first()

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
    val iterator = components.iterator()
    while (iterator.hasNext()) {
        val options = iterator.next()
            .options()
            .takeIf { it.isNotEmpty() } ?: continue

        if (options.count() == 1 && iterator.hasNext()) {
            val option = options.first()
            when (iterator.next()) {
                is UsageSection.Usage.Component.Placeholder -> {
                    val newOption = ShellCommand.Option.Value(option.names, option.mandatory)
                    list.add(newOption)
                }
                else -> list.addAll(options)
            }
        } else {
            list.addAll(options)
        }
    }
    return list
}

@ExperimentalStdlibApi
private fun UsageSection.Usage.Component.options(): List<ShellCommand.Option> = when (this) {
    is UsageSection.Usage.Component.Word -> emptyList()
    is UsageSection.Usage.Component.Placeholder -> {
        val placeholdersThatAreNotValues = setOf("options", "option", "arguments", "arguments", "command", "subcommand")
        if (placeholdersThatAreNotValues.contains(placeholder.placeholder.toLowerCase()))
            emptyList()
        else
            listOf(ShellCommand.Option.Value(names = listOf(Token.Option("", placeholder.placeholder)), mandatory = true, inline = true))
    }
    is UsageSection.Usage.Component.Group -> emptyList()
    is UsageSection.Usage.Component.Option -> listOf(ShellCommand.Option.Flag(names = listOf(option), mandatory = true))
    is UsageSection.Usage.Component.Optional -> optional.options().map { it.notMandatory() }
    is UsageSection.Usage.Component.Choices -> choices.map { it.options() }.merge()
    UsageSection.Usage.Component.LeaveEmpty -> emptyList()
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

    val hasArgument = takeSpacing()?.let { it.spaces < 2 } ?: false
    takeLine()

    return if (hasArgument) {
        ShellCommand.Option.Value(names, false)
    } else {
        ShellCommand.Option.Flag(names, false)
    }
}

private fun Scanner.takeSubcommand(): Token.Word? {
    val first = takeWord() ?: return null
    takeSpacing()?.takeIf { it.spaces > 1 } ?: return null
    return first
}