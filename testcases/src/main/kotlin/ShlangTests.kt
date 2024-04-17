package testcases

import common.RunExecutable
import startup.CCLIParser
import startup.ShlangDriver


class ShlangTests(private val workingDir: String, private val verbose: Boolean = false) {

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

    fun collectAllTests() {
        testDoWhile()
        testArrayAccess()
        testBubbleSort()
    }

    private fun runTest(filename: String, lib: List<String>): Result {
        val basename = filename.substringAfterLast("/").substringBeforeLast(".")

        compile(filename, basename, lib)

        val testResult = RunExecutable.runCommand(listOf("./$basename.out"), null)
        return Result(basename, testResult.output, testResult.error, testResult.exitCode)
    }

    private fun compile(filename: String, basename: String, lib: List<String>) {
        val args = arrayOf("-c", "$workingDir/$TESTCASES_DIR/$filename.c", "--dump-ir")

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
            throw RuntimeException("\nExpected: '$expectedStdio'\nActual:   '${result.output + result.error}'")
        } else {
            println("[Test passed: ${result.testName}]")
            if (verbose) {
                println("  Expected: '$expectedStdio'")
                println("  Stdin:  '${result.output}'")
                println("  Stderr: '${result.error}'")
                println("  Exit code: ${result.exitCode}")
            }
        }
    }

    private fun assertReturnCode(result: Result, errorCode: Int) {
        if (errorCode != result.exitCode) {
            throw RuntimeException("\nExpected: '$errorCode'\nActual:   '${result.exitCode}'")
        } else {
            println("[Test passed: ${result.testName}]")
            if (verbose) {
                println("  Expected: '$errorCode'")
                println("  Actual:   '${result.exitCode}'")
            }
        }
    }

    companion object {
        const val TESTCASES_DIR = "src/resources/"
    }
}