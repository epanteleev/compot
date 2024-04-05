package startup


class CCLIArguments {
    private var dumpIrEnabled = false
    private var filename = "<input>"
    private var optLevel = 0

    fun isDumpIr(): Boolean = dumpIrEnabled
    internal fun enableDumpIr() {
        dumpIrEnabled = true
    }

    fun getFilename(): String = filename
    fun getBasename(): String = getName(filename)
    fun getLogDir(): String {
        return getBasename()
    }

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
        if (dumpIrEnabled) {
            optCLIArguments.enableDumpIr()
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
                "--dump-ir" -> commandLineArguments.enableDumpIr()
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