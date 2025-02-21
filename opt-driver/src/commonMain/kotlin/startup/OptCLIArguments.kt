package startup

import common.Extension
import common.ProcessedFile


class OptCLIArguments {
    private var dumpIrDirectoryOutput: String? = null
    private var optimizationLevel = 0
    private var outFilename = ProcessedFile.fromFilename("out.o")
    private var inputFilename = arrayListOf<ProcessedFile>()
    private var pic = false

    fun isDumpIr(): Boolean = dumpIrDirectoryOutput != null

    fun getDumpIrDirectory(): String {
        return dumpIrDirectoryOutput!!
    }

    fun setDumpIrDirectory(out: String?): OptCLIArguments {
        dumpIrDirectoryOutput = out
        return this
    }

    fun setOutputFilename(output: ProcessedFile): OptCLIArguments {
        outFilename = output
        return this
    }

    fun getOptLevel(): Int = optimizationLevel
    fun setOptLevel(level: Int): OptCLIArguments {
        optimizationLevel = level
        return this
    }

    fun isPic(): Boolean = pic
    fun setPic(pic: Boolean): OptCLIArguments {
        this.pic = pic
        return this
    }

    fun getOutputFilename(): ProcessedFile = outFilename

    fun setFilename(name: ProcessedFile): OptCLIArguments {
        if (name.extension != Extension.IR) {
            throw IllegalArgumentException("Invalid file extension: ${name.extension}")
        }

        inputFilename.add(name)
        return this
    }

    fun inputs(): List<ProcessedFile> = inputFilename
}