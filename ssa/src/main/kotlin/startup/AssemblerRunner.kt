package startup

import java.io.File
import java.util.concurrent.TimeUnit

object AssemblerRunner {
    private fun runCommand(command: List<String>, workingDir: File? = null) {
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
    }

    fun run(filename: String, outputFileName: String) {
        val gnuAsCommandLine = listOf("as", filename, "-o", outputFileName)
        runCommand(gnuAsCommandLine, null)
    }
}