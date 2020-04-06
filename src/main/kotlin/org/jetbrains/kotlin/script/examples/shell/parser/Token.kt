package org.jetbrains.kotlin.script.examples.shell.parser

sealed class Token {
    data class Word(val word: String) : Token()
    data class Line(val line: String) : Token()
    data class Option(val option: String) : Token()

    data class Placeholder(val placeholder: String) : Token()
    data class Group(val placeholder: Placeholder) : Token()

    data class Spacing(val spaces: Int) : Token()

    object Colon : Token()
    object EmptyLine : Token()

    object ChoicesStart : Token()
    object ChoicesEnd : Token()

    object Separator : Token()

    object OptionalStart : Token()
    object OptionalEnd : Token()

    override fun toString() = when (this) {
        is Word -> word
        is Line -> line
        is Option -> "-$option"
        is Placeholder -> "<$placeholder>"
        is Group -> "$placeholder..."
        is Spacing -> generateSequence { " " }.take(spaces).joinToString()
        Colon -> ":"
        EmptyLine -> "\n"
        ChoicesStart -> "["
        ChoicesEnd -> "]"
        Separator -> "|"
        OptionalStart -> "("
        OptionalEnd -> ")"
    }
}