package org.jetbrains.kotlin.script.examples.shell

data class ShellCommand(
    val name: String,
    val subCommands: List<ShellCommand>,
    val options: List<Option>
) {
    sealed class Option {
        data class Flag(val name: String) : Option()
        data class Value(val name: String, val mandatory: Boolean, val choices: List<String>?) : Option()
    }
}