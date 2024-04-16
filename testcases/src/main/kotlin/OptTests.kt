package testcases

import common.RunExecutable
import startup.CliParser
import startup.OptDriver

data class Result(val testName: String, val output: String, val error: String, val exitCode: Int)

class OptTests(private val workingDir: String, val verbose: Boolean = false) {

    private fun testOpt() {
        val result = runTest("opt_ir/bubble_sort", listOf("runtime/runtime.c"))
        assert(result, "0 2 4 4 9 23 45 55 89 90 \n")
    }

    fun collectAllTests() {
        testOpt()
    }

    private fun runTest(filename: String, lib: List<String>): Result {
        val basename = filename.substringAfterLast("/").substringBeforeLast(".")

        compile(filename, basename, lib)

        val testResult = RunExecutable.runCommand(listOf("./$basename"), workingDir)
        return Result(basename, testResult.output, testResult.error, testResult.exitCode)
    }

    private fun compile(filename: String, basename: String, lib: List<String>) {
        val args = arrayOf("-c", "$workingDir/$TESTCASES_DIR/$filename.ir")

        val cli = CliParser().parse(args) ?: throw RuntimeException("Failed to parse arguments: $args")
        OptDriver(cli).run()

        val insertedPath = lib.map { "$TESTCASES_DIR/$it" }

        val gnuAsCommandLine = listOf("gcc", "$basename.o") + insertedPath + listOf("-o", basename)
        val result = RunExecutable.runCommand(gnuAsCommandLine, workingDir)
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
    companion object {
        const val TESTCASES_DIR = "src/resources/"
    }
}