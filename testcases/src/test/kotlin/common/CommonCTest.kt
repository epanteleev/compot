package common

import startup.CCLIParser
import startup.ShlangDriver


abstract class CommonCTest: CommonTest() {
    protected fun runCTest(filename: String, extraFiles: List<String>): Result {
        val basename = filename.substringAfterLast("/").substringBeforeLast(".")
        compileCFile(filename, basename, listOf(), extraFiles)

        val testResult = RunExecutable.runCommand(listOf("./$TEST_OUTPUT_DIR/$basename.out"), null)
        return Result(basename, testResult.output, testResult.error, testResult.exitCode)
    }

    protected fun runOptimizedCTest(filename: String, lib: List<String>): Result {
        val basename = filename.substringAfterLast("/").substringBeforeLast(".")
        compileCFile(filename, basename, listOf("-O1"), lib)

        val testResult = RunExecutable.runCommand(listOf("./$TEST_OUTPUT_DIR/$basename.out"), null)
        return Result(basename, testResult.output, testResult.error, testResult.exitCode)
    }

    private fun compileCFile(filename: String, basename: String, optOptions: List<String>, extraFiles: List<String>) {
        val args = arrayOf("-c", "$TESTCASES_DIR/$filename.c", "--dump-ir", TEST_OUTPUT_DIR, "-I$TESTCASES_DIR") +
                optOptions +
                listOf("-o", "$TEST_OUTPUT_DIR/$basename")

        val cli = CCLIParser.parse(args) ?: throw RuntimeException("Failed to parse arguments: $args")
        ShlangDriver(cli).run()

        runGCC(basename, extraFiles)
    }
}