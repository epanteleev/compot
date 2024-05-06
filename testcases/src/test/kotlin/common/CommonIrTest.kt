package common

import startup.CliParser
import startup.OptDriver


abstract class CommonIrTest: CommonTest() {
    abstract fun options(): List<String>

    protected fun runTest(filename: String, lib: List<String>, opts: List<String>): Result {
        val basename = filename.substringAfterLast("/").substringBeforeLast(".")
        compile(filename, basename, opts, lib)

        val testResult = RunExecutable.runCommand(listOf("./$TEST_OUTPUT_DIR/$basename.out"), null)
        return Result(basename, testResult.output, testResult.error, testResult.exitCode)
    }

    private fun compile(filename: String, basename: String, optOptions: List<String>, extraFiles: List<String>) {
        val args = arrayOf("-c", "$TESTCASES_DIR/$filename.ir", "--dump-ir", TEST_OUTPUT_DIR) +
                optOptions +
                listOf("-o", "$TEST_OUTPUT_DIR/$basename")

        val cli = CliParser.parse(args) ?: throw RuntimeException("Failed to parse arguments: $args")
        OptDriver(cli).run()

        runGCC(basename, extraFiles)
    }
}