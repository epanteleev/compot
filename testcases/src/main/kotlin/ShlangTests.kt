package testcases

import common.RunExecutable
import startup.CCLIParser
import startup.ShlangDriver

data class FailedTestException(override val message: String): Exception(message)


class ShlangTests(private val workingDir: String, private val verbose: Boolean = false) {
    private val testCases = linkedMapOf(
        "doWhile" to ::testDoWhile,
        "arrayAccess" to ::testArrayAccess,
        "bubble_sort" to ::testBubbleSort,
        "fibonacci1" to ::testFibonacci
    )

    private fun testDoWhile() {
        val result = runTest("shlang/doWhile", listOf())
        assertReturnCode(result, 20)
    }

    private fun testArrayAccess() {
        val result = runTest("shlang/arrayAccess", listOf())
        assertReturnCode(result, 3)
    }

    private fun testBubbleSort() {
        val result = runTest("shlang/bubble_sort", listOf("runtime/runtime.c"))
        assert(result, "11 12 22 25 34 64 \n")
    }

    private fun testFibonacci() {
        val result = runTest("shlang/fibonacci1", listOf("runtime/runtime.c"))
        assert(result, "55\n")
    }

    fun collectAllTests() {
        testCases.forEach { (name, test) ->
            try {
                test()
            } catch (e: FailedTestException) {
                println("[Test failed: ${name}]")
                println(e.message)
                return@forEach
            }
            println("[Test passed: $name]")
        }
    }

    private fun runTest(filename: String, lib: List<String>): Result {
        val basename = filename.substringAfterLast("/").substringBeforeLast(".")

        compile(filename, basename, lib)

        val testResult = RunExecutable.runCommand(listOf("./$basename.out"), null)
        return Result(basename, testResult.output, testResult.error, testResult.exitCode)
    }

    private fun compile(filename: String, basename: String, lib: List<String>) {
        val args = arrayOf("-c", "$workingDir/$TESTCASES_DIR/$filename.c", "--dump-ir", "-O1")

        val cli = CCLIParser().parse(args) ?: throw RuntimeException("Failed to parse arguments: $args")
        ShlangDriver(cli).run()

        val insertedPath = lib.map { "$workingDir/$TESTCASES_DIR/$it" }

        val gccCommandLine = listOf("gcc", "$basename.o") + insertedPath + listOf("-o", "$basename.out")
        val result = RunExecutable.runCommand(gccCommandLine, null)
        if (result.exitCode != 0) {
            throw RuntimeException("execution failed with code ${result.exitCode}:\n${result.error}")
        }
    }

    private fun assert(result: Result, expectedStdio: String) {
        if (expectedStdio != result.output + result.error) {
            throw FailedTestException("\nExpected: '$expectedStdio'\nActual:   '${result.output + result.error}'")
        }
    }

    private fun assertReturnCode(result: Result, errorCode: Int) {
        if (errorCode != result.exitCode) {
            throw FailedTestException("\nExpected: '$errorCode'\nActual:   '${result.exitCode}'")
        }
    }

    companion object {
        const val TESTCASES_DIR = "src/resources/"
    }
}