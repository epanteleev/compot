package common

expect fun runCommand(command: String, args: List<String>, workingDir: String? = null): ExecutionResult

fun checkedRunCommand(command: String, args: List<String>, workingDir: String? = null): ExecutionResult {
    val result = runCommand(command, args, workingDir)
    if (result.exitCode != 0) {
        throw RuntimeException("execution failed with code ${result.exitCode}:\n${result.error}")
    }

    return result
}

expect fun env(name: String): String?

expect fun pwd(): String

class ExecutionResult(val output: String, val error: String, val exitCode: Int)