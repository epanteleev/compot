package common.commandLine

import common.Files

abstract class AnyCLIArguments {
    protected var dumpIrDirectoryOutput: String? = null
    protected var optimizationLevel = 0
    protected var outFilename: String? = null

    fun isDumpIr(): Boolean = dumpIrDirectoryOutput != null

    fun getDumpIrDirectory(): String {
        return dumpIrDirectoryOutput!!
    }

    fun setDumpIrDirectory(out: String) {
        dumpIrDirectoryOutput = out
    }

    fun setOutputFilename(name: String) {
        outFilename = name
    }

    fun getOptLevel(): Int = optimizationLevel
    fun setOptLevel(level: Int) {
        optimizationLevel = level
    }

    protected fun getName(name: String): String {
        return Files.getBasename(name)
    }
}