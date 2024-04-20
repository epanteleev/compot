import common.RunExecutable
import startup.CCLIParser
import startup.CliParser
import startup.OptDriver
import startup.ShlangDriver
import kotlin.test.assertEquals


abstract class CommonTest {
    protected fun runTest(filename: String, lib: List<String>): Result {
        val basename = filename.substringAfterLast("/").substringBeforeLast(".")

        compile(filename, basename, lib)

        val testResult = RunExecutable.runCommand(listOf("./$TEST_OUTPUT_DIR/$basename.out"), null)
        return Result(basename, testResult.output, testResult.error, testResult.exitCode)
    }

    protected fun runCTest(filename: String, lib: List<String>): Result {
        val basename = filename.substringAfterLast("/").substringBeforeLast(".")

        compileCFile(filename, basename, lib)

        val testResult = RunExecutable.runCommand(listOf("./$TEST_OUTPUT_DIR/$basename.out"), null)
        return Result(basename, testResult.output, testResult.error, testResult.exitCode)
    }

    private fun compile(filename: String, basename: String, lib: List<String>) {
        val args = arrayOf("-c", "$TESTCASES_DIR/$filename.ir", "--dump-ir", TEST_OUTPUT_DIR, "-o", "$TEST_OUTPUT_DIR/$basename")

        val cli = CliParser().parse(args) ?: throw RuntimeException("Failed to parse arguments: $args")
        OptDriver(cli).run()

        val insertedPath = lib.map { "$TESTCASES_DIR/$it" }

        val gccCommandLine = listOf("gcc", "$TEST_OUTPUT_DIR/$basename.o") + insertedPath + listOf("-o", "$TEST_OUTPUT_DIR/$basename.out")
        val result = RunExecutable.runCommand(gccCommandLine, null)
        if (result.exitCode != 0) {
            throw RuntimeException("execution failed with code ${result.exitCode}:\n${result.error}")
        }
    }

    private fun compileCFile(filename: String, basename: String, lib: List<String>) {
        val args = arrayOf("-c", "$TESTCASES_DIR/$filename.c", "--dump-ir", TEST_OUTPUT_DIR ,"-o", "$TEST_OUTPUT_DIR/$basename")

        val cli = CCLIParser().parse(args) ?: throw RuntimeException("Failed to parse arguments: $args")
        ShlangDriver(cli).run()

        val insertedPath = lib.map { "$TESTCASES_DIR/$it" }

        val gccCommandLine = listOf("gcc", "$TEST_OUTPUT_DIR/$basename.o") + insertedPath + listOf("-o", "$TEST_OUTPUT_DIR/$basename.out")
        val result = RunExecutable.runCommand(gccCommandLine, null)
        if (result.exitCode != 0) {
            throw RuntimeException("execution failed with code ${result.exitCode}:\n${result.error}")
        }
    }

    protected fun assertReturnCode(result: Result, errorCode: Int) {
        assertEquals(errorCode, result.exitCode)
    }

    protected fun assert(result: Result, expectedStdio: String) {
        assertEquals(expectedStdio, result.output + result.error)
    }

    companion object {
        const val TESTCASES_DIR = "src/resources/"
        private val TEST_OUTPUT_DIR = RunExecutable.getenv("TEST_RESULT_DIR") ?:
            throw RuntimeException("TEST_OUTPUT_DIR is not set")
    }
}