package common

import java.io.File
import java.util.concurrent.TimeUnit


actual fun runCommand(command: String, args: List<String>, workingDir: String?): ExecutionResult {
    val process = ProcessBuilder(listOf(command) + args)
        .directory(workingDir?.let { File(it) })
        .start()
    if (!process.waitFor(60, TimeUnit.SECONDS)) {
        process.destroy()
        throw RuntimeException("execution timed out")
    }
    return ExecutionResult(
        process.inputStream.bufferedReader().readText(),
        process.errorStream.bufferedReader().readText(),
        process.exitValue())
}

actual fun env(name: String): String? {
    return System.getenv(name)
}

actual fun pwd(): String {
    return File(".").absolutePath
}