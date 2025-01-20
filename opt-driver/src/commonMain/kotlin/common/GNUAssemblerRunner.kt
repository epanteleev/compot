package common


object GNUAssemblerRunner {
    fun compileAsm(filename: String, outputFileName: String): ExecutionResult {
        val gnuAsCommandLine = listOf(filename, "-o", outputFileName)
        return runCommand("as", gnuAsCommandLine, null)
    }
}