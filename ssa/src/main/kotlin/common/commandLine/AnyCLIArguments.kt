package common.commandLine

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

        return getFilename()
    }

    fun setOutputFilename(name: String) {
        outFilename = name
    }

    fun getFilename(): String = inputFilename
    fun getBasename(): String = getName(inputFilename)
    fun getLogDir(): String = getBasename()

    fun setFilename(name: String) {
        inputFilename = name
    }

    fun getOptLevel(): Int = optimizationLevel
    fun setOptLevel(level: Int) {
        optimizationLevel = level
    }

    private fun getName(name: String): String {
        val fileName = java.nio.file.Paths.get(name).fileName.toString()
        val lastIndex = fileName.lastIndexOf('.')
        if (lastIndex != -1) {
            return fileName.substring(0, lastIndex)
        }

        return fileName
    }
}