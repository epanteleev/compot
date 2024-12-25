package common

import okio.FileSystem
import okio.Path.Companion.toPath
import startup.ShlangDriver
import startup.CCLIParser
import kotlin.random.Random


abstract class CommonCTest: CommonTest() {
    abstract fun options(): List<String>

    protected fun runCTest(filename: String, runtimeLib: List<String>, opts: List<String>): Result {
        val basename = filename.substringAfterLast("/").substringBeforeLast(".") + Random.nextInt()
        compileObject(filename, basename, opts, runtimeLib)

        val testResult = runCommand("./$TEST_OUTPUT_DIR/$basename.out", listOf(), null)
        return Result(basename, testResult.output, testResult.error, testResult.exitCode)
    }

    protected fun compile(filename: String, basename: String, optOptions: List<String>, runtimeLib: List<String>): String {
        val output = "$TEST_OUTPUT_DIR/$basename"

        val args = arrayOf("-c", "$TESTCASES_DIR/$filename.c", "--dump-ir", TEST_OUTPUT_DIR, "-I$TESTCASES_DIR") +
                optOptions +
                listOf("-o", output)

        val cli = CCLIParser.parse(args) ?: throw RuntimeException("Failed to parse arguments: $args")
        ShlangDriver(cli).run()
        return output
    }

    protected fun readExpectedOutput(filename: String): String {
        return FileSystem.SYSTEM.read("$TESTCASES_DIR/expected_out/$filename".toPath()) {
            readUtf8()
        }
    }

    private fun compileObject(filename: String, basename: String, optOptions: List<String>, runtimeLib: List<String>) {
        val output = "$TEST_OUTPUT_DIR/$basename"
        val args = arrayOf("-c", "$TESTCASES_DIR/$filename.c", "--dump-ir", TEST_OUTPUT_DIR, "-I$TESTCASES_DIR") +
                optOptions +
                listOf("-o", "$output.o")

        val cli = CCLIParser.parse(args) ?: throw RuntimeException("Failed to parse arguments: $args")
        ShlangDriver(cli).run()

        runGCC(output, runtimeLib)
    }
}