package common

import ir.read.ModuleReader
import startup.*


abstract class CommonIrTest: CommonTest() {
    abstract fun options(): List<String>

    protected fun runTest(filename: String, lib: List<String>, opts: List<String>): Result {
        val basename = filename.substringAfterLast("/").substringBeforeLast(".")
        compile(filename, basename, opts, lib)

        val testResult = runCommand("./$TEST_OUTPUT_DIR/$basename.out", listOf(), null)
        return Result(basename, testResult.output, testResult.error, testResult.exitCode)
    }

    private fun compile(filename: String, basename: String, optOptions: List<String>, extraFiles: List<String>) {
        val output = "$TEST_OUTPUT_DIR/$basename"
        val args = arrayOf("-c", "$TESTCASES_DIR/$filename.ir", "--dump-ir", TEST_OUTPUT_DIR) +
                optOptions +
                listOf("-o", "$output.o")

        val cli = CliParser.parse(args) ?: throw RuntimeException("Failed to parse arguments: $args")
        val module = ModuleReader.read(cli.inputs().first().filename)
        OptDriver.compile(cli, module)

        runGCC(output, extraFiles)
    }
}