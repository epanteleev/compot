package common

import java.io.File
import java.util.concurrent.TimeUnit

object RunExecutable {
    fun runCommand(command: List<String>, workingDir: File? = null): ExecutionResult {
        val process = ProcessBuilder(command)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroy()
            throw RuntimeException("execution timed out: $this")
        }
        if (process.exitValue() != 0) {
            throw RuntimeException("execution failed with code ${process.exitValue()}: $this")
        }
        return ExecutionResult(process)
    }
}

data class ExecutionResult(val process: Process) {
    val output: String
        get() = process.inputStream.bufferedReader().readText()
    val error: String
        get() = process.errorStream.bufferedReader().readText()
    val exitCode: Int
        get() = process.exitValue()
}