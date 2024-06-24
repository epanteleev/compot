package common


object GNUAssemblerRunner {
    fun run(filename: String, outputFileName: String) {
        val gnuAsCommandLine = listOf(filename, "-o", outputFileName)
        runCommand("as", gnuAsCommandLine, null)
    }
}