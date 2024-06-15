package common

import startup.CCLIParser
import startup.ShlangDriver


abstract class CommonCTest: CommonTest() {
    abstract fun options(): List<String>

    protected fun runCTest(filename: String, extraFiles: List<String>, opts: List<String>): Result {
        val basename = filename.substringAfterLast("/").substringBeforeLast(".")
        compile(filename, basename, opts, extraFiles)

        val testResult = RunExecutable.runCommand("./$TEST_OUTPUT_DIR/$basename.out", listOf(), null)
        return Result(basename, testResult.output, testResult.error, testResult.exitCode)
    }

    private fun compile(filename: String, basename: String, optOptions: List<String>, extraFiles: List<String>) {
        val args = arrayOf("-c", "$TESTCASES_DIR/$filename.c", "--dump-ir", TEST_OUTPUT_DIR, "-I$TESTCASES_DIR") +
                optOptions +
                listOf("-o", "$TEST_OUTPUT_DIR/$basename")

        val cli = CCLIParser.parse(args) ?: throw RuntimeException("Failed to parse arguments: $args")
        ShlangDriver(cli).run()

        runGCC(basename, extraFiles)
    }
}