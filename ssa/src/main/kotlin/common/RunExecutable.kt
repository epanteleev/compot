package common

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

object RunExecutable {
    fun runCommand(command: List<String>, workingDir: String? = null): ExecutionResult {
        val process = ProcessBuilder(command)
            .directory(workingDir?.let { File(it) })
            .start()
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroy()
            throw RuntimeException("execution timed out: $this")
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