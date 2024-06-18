package common

import java.io.File
import java.util.concurrent.TimeUnit


object RunExecutable {
    fun runCommand(command: String, args: List<String>, workingDir: String? = null): ExecutionResult {
        val process = ProcessBuilder(listOf(command) + args)
            .directory(workingDir?.let { File(it) })
            .start()
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroy()
            throw RuntimeException("execution timed out: $this")
        }
        return ExecutionResult(process)
    }

    fun checkedRunCommand(command: String, args: List<String>, workingDir: String? = null): ExecutionResult {
        val result = runCommand(command, args, workingDir)
        if (result.exitCode != 0) {
            throw RuntimeException("execution failed with code ${result.exitCode}:\n${result.error}")
        }

        return result
    }

    fun env(name: String): String? {
        return System.getenv(name)
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