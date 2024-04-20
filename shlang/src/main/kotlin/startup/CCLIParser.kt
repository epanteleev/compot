package startup


class CCLIArguments {
    private var dumpIrDirectoryOutput: String? = null
    private var filename = "<input>"
    private var optLevel = 0
    private var outputFilename: String? = null

    fun isDumpIr(): Boolean = dumpIrDirectoryOutput != null
    fun getDumpIrDirectory(): String {
        return dumpIrDirectoryOutput!!
    }

    internal fun setDumpIrDirectory(out: String) {
        dumpIrDirectoryOutput = out
    }

    fun outputFilename(): String {
        if (outputFilename != null) {
            return outputFilename!!
        }

        return getFilename()
    }

    internal fun setOutputFilename(name: String) {
        outputFilename = name
    }

    fun getFilename(): String = filename
    fun getBasename(): String = getName(filename)
    fun getLogDir(): String = getBasename()

    internal fun setFilename(name: String) {
        filename = name
    }

    fun getOptLevel(): Int = optLevel
    internal fun setOptLevel(level: Int) {
        optLevel = level
    }

    private fun getName(name: String): String {
        val fileName = java.nio.file.Paths.get(name).fileName.toString()
        val lastIndex = fileName.lastIndexOf('.')
        if (lastIndex != -1) {
            return fileName.substring(0, lastIndex)
        }

        return fileName
    }

    fun makeOptCLIArguments(): OptCLIArguments {
        val optCLIArguments = OptCLIArguments()
        optCLIArguments.setFilename(filename)
        optCLIArguments.setOptLevel(optLevel)
        if (isDumpIr()) {
            optCLIArguments.setDumpIrDirectory(dumpIrDirectoryOutput!!)
        }
        if (outputFilename != null) {
            optCLIArguments.setOutputFilename(outputFilename!!)
        }
        return optCLIArguments
    }
}


class CCLIParser {
    fun parse(args: Array<String>): CCLIArguments? {
        if (args.isEmpty()) {
            printHelp()
            return null
        }

        var cursor = 0

        val commandLineArguments = CCLIArguments()
        while (cursor < args.size) {
            when (val arg = args[cursor]) {
                "-c", "--compile" -> {
                    if (cursor + 1 >= args.size) {
                        println("Expected input filename after -o")
                            return null
                    }
                    cursor++
                    commandLineArguments.setFilename(args[cursor])
                }
                "-O0" -> commandLineArguments.setOptLevel(0)
                "-O1" -> commandLineArguments.setOptLevel(1)
                "--dump-ir" -> {
                    if (cursor + 1 >= args.size) {
                        println("Expected output directory after --dump-ir")
                        return null
                    }
                    cursor++
                    commandLineArguments.setDumpIrDirectory(args[cursor])
                }
                "-o" -> {
                    if (cursor + 1 >= args.size) {
                        println("Expected output filename after -o")
                        return null
                    }
                    cursor++
                    commandLineArguments.setOutputFilename(args[cursor])
                }
                else -> {
                    println("Unknown argument: $arg")
                    return null
                }
            }
            cursor++
        }

        return commandLineArguments
    }

    private fun printHelp() {
        println("Usage: shlang [options] <filename>")
        println("Options:")
        println("  -c, --compile <filename>  Compile the input file")
        println("  -O0                       Disable optimizations")
        println("  -O1                       Enable optimizations")
        println("  --dump-ir                 Dump IR to files")
    }
}