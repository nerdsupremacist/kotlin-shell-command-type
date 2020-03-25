package org.jetbrains.kotlin.script.examples.shell.parser

inline fun <T> collect(collector: () -> T?): List<T> {
    val list = mutableListOf<T>()

    do {
        val value = collector()?.also { list.add(it) }
    } while (value != null)

    return list
}