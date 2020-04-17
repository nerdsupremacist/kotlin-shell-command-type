package org.jetbrains.kotlin.script.examples

import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

@ExperimentalStdlibApi
class ScriptTest {

    @Test
    fun `Sample Test`() {
        val out = captureOut {
            val res = evalFile("hello-world")
            assertSucceeded(res)
        }

        val header = File("testData/test.h")
        val implementation = File("testData/test.c")

        val expected = """
            # 1 "${implementation.absolutePath}"
            # 1 "<built-in>" 1
            # 1 "<built-in>" 3
            # 363 "<built-in>" 3
            # 1 "<command line>" 1
            # 1 "<built-in>" 2
            # 1 "${implementation.absolutePath}" 2
            # 1 "${header.absolutePath}" 1
            int foo();
            # 3 "${implementation.absolutePath}" 2
            int foo() {
                return 42;
            }
        """.trimIndent()

        Assert.assertEquals(expected, out)
    }

}

private fun assertSucceeded(res: ResultWithDiagnostics<EvaluationResult>) {
    Assert.assertTrue(
        "test failed:\n  ${res.reports.joinToString("\n  ") { it.message + if (it.exception == null) "" else ": ${it.exception}" }}",
        res is ResultWithDiagnostics.Success
    )
}

private fun evalFile(scriptName: String): ResultWithDiagnostics<EvaluationResult> {
    val scriptFile = File("testData/$scriptName.$extension.kts")
    val scriptDefinition = createJvmCompilationConfigurationFromTemplate<Script>()

    val evaluationEnv = ScriptEvaluationConfiguration {
        jvm {
            baseClassLoader(null)
        }
        constructorArgs(emptyArray<String>())
        enableScriptsInstancesSharing()
    }

    return BasicJvmScriptingHost().eval(scriptFile.toScriptSource(), scriptDefinition, evaluationEnv)
}

private fun captureOut(body: () -> Unit): String {
    val outStream = ByteArrayOutputStream()
    val prevOut = System.out
    System.setOut(PrintStream(outStream))
    try {
        body()
    } finally {
        System.out.flush()
        System.setOut(prevOut)
    }
    return outStream.toString().trim()
}