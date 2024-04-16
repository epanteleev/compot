package startup

import common.RunExecutable


object GNUAssemblerRunner {

    fun run(filename: String, outputFileName: String) {
        val gnuAsCommandLine = listOf("as", filename, "-o", outputFileName)
        RunExecutable.runCommand(gnuAsCommandLine, null)
    }
}