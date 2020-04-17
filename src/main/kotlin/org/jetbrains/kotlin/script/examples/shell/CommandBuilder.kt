package org.jetbrains.kotlin.script.examples.shell

import eu.jrie.jetbrains.kotlinshell.shell.shell
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.kotlin.script.examples.Command
import java.io.File
import java.lang.StringBuilder

data class TypeSafeCommand<C>(internal val name: String, val arguments: List<String> = emptyList()) {
    constructor(name: String, vararg arguments: String) : this(name, arguments.toList())

    operator fun invoke(init: CommandBuilder<C>.() -> Unit = {}): String {
        return CommandBuilder<C>(name).apply {
            init()
            arguments.forEach { +it }
        }.build()
    }
}

data class Flag(internal val name: String)

data class Value(internal val name: String?) {
    operator fun invoke(value: String) = listOf(name, value).mapNotNull { it }.joinToString(" ")
    operator fun invoke(file: File) = this(file.absolutePath)
}

class CommandBuilder<C>(name: String) {
    private val builder = StringBuilder().apply { append(name) }

    operator fun String.unaryPlus() {
        builder.append(' ')
        builder.append(this)
    }

    operator fun File.unaryPlus() {
        +absolutePath
    }

    operator fun Flag.unaryPlus() {
        +name
    }

    internal fun build(): String {
        return builder.toString()
    }
}
