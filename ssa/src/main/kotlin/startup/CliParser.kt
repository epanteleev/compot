package startup

import java.nio.file.Paths


class OptCLIArguments {
    private var dumpIrDirectoryOutput: String? = null
    private var filename = "output"
    private var optLevel = 0
    private var outputFilename: String? = null

    fun isDumpIr(): Boolean = dumpIrDirectoryOutput != null
    fun getDumpIrDirectory(): String {
        return dumpIrDirectoryOutput!!
    }

    fun setDumpIrDirectory(out: String) {
        dumpIrDirectoryOutput = out
    }

    fun outputFilename(): String {
        if (outputFilename != null) {
            return outputFilename!!
        }

        return getBasename()
    }

    fun setOutputFilename(name: String) {
        outputFilename = name
    }
    fun getFilename(): String = filename
    fun getBasename(): String = getName(filename)
    fun getLogDir(): String {
        return getBasename()
    }

    fun setFilename(name: String) {
        filename = name
    }

    fun getOptLevel(): Int = optLevel
    fun setOptLevel(level: Int) {
        optLevel = level
    }

    private fun getName(name: String): String {
        val fileName = Paths.get(name).fileName.toString()
        val lastIndex = fileName.lastIndexOf('.')
        if (lastIndex != -1) {
            return fileName.substring(0, lastIndex)
        }

        return fileName
    }
}

class CliParser {
    fun parse(args: Array<String>): OptCLIArguments? {
        if (args.isEmpty()) {
            printHelp()
            return null
        }

        var cursor = 0

        val commandLineArguments = OptCLIArguments()
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
                "-O", "--opt-level" -> {
                    if (cursor + 1 >= args.size) {
                        println("Expected optimization level after -O")
                        return null
                    }
                    cursor++
                    val level = args[cursor].toIntOrNull()
                    if (level == null) {
                        println("Invalid optimization level: ${args[cursor]}")
                        return null
                    }
                    commandLineArguments.setOptLevel(level)
                }
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
                "-h", "--help" -> {
                    printHelp()
                    return null
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
        println("Usage: ssa [options] <filename>")
        println("Options:")
        println("  -c, --compile <filename> Set output filename")
        println("  -O, --opt-level <level>  Set optimization level")
        println("  --dump-ir                Dump IR to files")
        println("  -h, --help               Show this help message")
    }
}