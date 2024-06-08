package common


object RunExecutable {
    fun runCommand(command: List<String>, workingDir: String? = null): ExecutionResult {
        TODO()
    }

    fun checkedRunCommand(command: List<String>, workingDir: String? = null): ExecutionResult {
        val result = runCommand(command, workingDir)
        if (result.exitCode != 0) {
            throw RuntimeException("execution failed with code ${result.exitCode}:\n${result.error}")
        }
        return result
    }

    fun getenv(name: String): String? {
        TODO()
    }
}

class ExecutionResult() {
    val output: String
        get() = TODO()
    val error: String
        get() = TODO()
    val exitCode: Int
        get() = TODO()
}