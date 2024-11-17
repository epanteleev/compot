package common.commandLine

import common.Files
import okio.Path.Companion.toPath

abstract class AnyCLIArguments {
    protected var dumpIrDirectoryOutput: String? = null
    protected var inputFilename = "<input>"
    protected var optimizationLevel = 0
    protected var outFilename: String? = null

    fun isDumpIr(): Boolean = dumpIrDirectoryOutput != null

    fun getDumpIrDirectory(): String {
        return dumpIrDirectoryOutput!!
    }

    fun setDumpIrDirectory(out: String) {
        dumpIrDirectoryOutput = out
    }

    fun getOutputFilename(): String {
        if (outFilename != null) {
            return outFilename!!
        }

        val name = getFilename()
        val lastIndex = name.lastIndexOf('.')
        val basename = if (lastIndex != -1) {
            name.substring(0, lastIndex)
        } else {
            name
        }

        return "$basename.o"
    }

    fun setOutputFilename(name: String) {
        outFilename = name
    }

    fun getFilename(): String = inputFilename
    fun getBasename(): String = getName(inputFilename)

    fun setFilename(name: String) {
        inputFilename = name
    }

    fun getOptLevel(): Int = optimizationLevel
    fun setOptLevel(level: Int) {
        optimizationLevel = level
    }

    protected fun getName(name: String): String {
        return Files.getBasename(name)
    }
}