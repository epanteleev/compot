package startup

import common.ProcessedFile

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
                    val outputFilename = ProcessedFile.fromFilename(args[cursor])
                    commandLineArguments.setFilename(outputFilename)
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
                    val outputFilename = ProcessedFile.fromFilename(args[cursor])
                    commandLineArguments.setOutputFilename(outputFilename)
                }
                "-fPIC" -> commandLineArguments.setPic(true)
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
        println("Usage: opt [options] <filename>")
        println("Options:")
        println("  -c, --compile <filename> Set output filename")
        println("  -O<NUM>                  Set optimization level")
        println("  -o <filename>            Set output filename")
        println("  --dump-ir <directory>    Dump IR to directory")
        println("  -h, --help               Show this help message")
    }
}