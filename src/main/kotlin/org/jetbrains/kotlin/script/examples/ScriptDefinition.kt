package org.jetbrains.kotlin.script.examples

import org.jetbrains.kotlin.script.examples.shell.CommandBuilder
import org.jetbrains.kotlin.script.examples.shell.TypeSafeCommand
import org.jetbrains.kotlin.script.examples.shell.Flag
import org.jetbrains.kotlin.script.examples.shell.Value
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm

object ScriptDefinition : ScriptCompilationConfiguration({
    defaultImports(
        File::class,
        Command::class,
        TypeSafeCommand::class,
        Flag::class,
        Value::class,
        CommandBuilder::class
    )

    defaultImports("eu.jrie.jetbrains.kotlinshell.shell.*", "kotlinx.coroutines.*")

    jvm {
        dependenciesFromClassContext(ScriptDefinition::class, wholeClasspath = true)
    }

    refineConfiguration {
        onAnnotations(Command::class, handler = Configurator)
    }

    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }
})