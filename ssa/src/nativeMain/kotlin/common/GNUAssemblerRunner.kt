package common


object GNUAssemblerRunner {

    fun run(filename: String, outputFileName: String) {
        val gnuAsCommandLine = listOf(filename, "-o", outputFileName)
        val result = RunExecutable.runCommand("as", gnuAsCommandLine, null)
    }
}