package startup

import common.Extension
import common.ProcessedFile


class OptCLIArguments {
    private var dumpIrDirectoryOutput: String? = null
    private var optimizationLevel = 0
    private var outFilename = ProcessedFile.fromFilename("out.o")
    private var inputFilename = arrayListOf<ProcessedFile>()

    fun isDumpIr(): Boolean = dumpIrDirectoryOutput != null

    fun getDumpIrDirectory(): String {
        return dumpIrDirectoryOutput!!
    }

    fun setDumpIrDirectory(out: String) {
        dumpIrDirectoryOutput = out
    }

    fun setOutputFilename(output: ProcessedFile) {
        outFilename = output
    }

    fun getOptLevel(): Int = optimizationLevel
    fun setOptLevel(level: Int) {
        optimizationLevel = level
    }

    fun getOutputFilename(): ProcessedFile = outFilename

    fun setFilename(name: ProcessedFile) {
        if (name.extension != Extension.IR) {
            throw IllegalArgumentException("Invalid file extension: ${name.extension}")
        }

        inputFilename.add(name)
    }

    fun inputs(): List<ProcessedFile> = inputFilename
}