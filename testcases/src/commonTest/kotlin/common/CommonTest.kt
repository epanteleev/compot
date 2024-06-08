package common

import kotlin.test.assertEquals


data class Result(val testName: String, val output: String, val error: String, val exitCode: Int)

abstract class CommonTest {
    protected fun assertReturnCode(result: Result, errorCode: Int) {
        assertEquals(errorCode, result.exitCode)
    }

    protected fun assert(result: Result, expectedStdio: String) {
        assertEquals(expectedStdio, result.output + result.error)
    }

    protected fun runGCC(basename: String, extraFiles: List<String>) {
        val insertedPath = extraFiles.map { "$TESTCASES_DIR/$it" }
        val gccCommandLine = listOf("gcc", "$TEST_OUTPUT_DIR/$basename.o") + insertedPath + listOf("-o", "$TEST_OUTPUT_DIR/$basename.out")
        RunExecutable.checkedRunCommand(gccCommandLine, null)
    }

    companion object {
        const val TESTCASES_DIR = "src/resources/"
        val TEST_OUTPUT_DIR =  "test-results"// RunExecutable.getenv("TEST_RESULT_DIR") ?: TODO
          //  throw RuntimeException("TEST_OUTPUT_DIR is not set")
    }
}