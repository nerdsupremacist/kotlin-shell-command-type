package org.jetbrains.kotlin.script.examples

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.script.examples.shell.ShellCommand
import org.jetbrains.kotlin.script.examples.shell.code
import org.jetbrains.kotlin.script.examples.shell.fromName
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.toScriptSource

object Configurator : RefineScriptCompilationConfigurationHandler {

    @ExperimentalStdlibApi
    override fun invoke(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        val baseDirectory = (context.script as? FileBasedScriptSource)?.file?.parentFile

        val annotations = context
            .collectedData
            ?.get(ScriptCollectedData.foundAnnotations)
            ?.mapNotNull { annotation ->
                when (annotation) {
                    is Command -> annotation
                    else -> null
                }
            }
            ?.takeIf { it.isNotEmpty() } ?: return context.compilationConfiguration.asSuccess()

        val generatedCode = runBlocking {
            annotations.map {
                async {
                    ShellCommand.fromName(it.name)
                }
            }.awaitAll().mapNotNull { it?.code() }
        }

        val generatedScripts = generatedCode
            .map { resolvedCode ->
                createTempFile(prefix = "CodeGen", suffix = ".$extension.kts", directory = baseDirectory)
                    .apply { writeText(resolvedCode) }
                    .apply { deleteOnExit() }
                    .toScriptSource()
            }

        return ScriptCompilationConfiguration(context.compilationConfiguration) {
            importScripts.append(generatedScripts)
        }.asSuccess()
    }

}