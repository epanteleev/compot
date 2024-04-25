package startup

import common.commandLine.AnyCLIArguments

class OptCLIArguments : AnyCLIArguments()


object CliParser {
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
                "-O1" -> {
                    commandLineArguments.setOptLevel(1)
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
        println("  -O1                      Set optimization level")
        println("  -o <filename>            Set output filename")
        println("  --dump-ir <directory>    Dump IR to directory")
        println("  -h, --help               Show this help message")
    }
}