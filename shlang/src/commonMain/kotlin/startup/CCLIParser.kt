package startup

import common.commandLine.AnyCLIArguments


class CCLIArguments : AnyCLIArguments() {
    private val includeDirectories = mutableSetOf<String>()

    fun addIncludeDirectory(directory: String) {
        includeDirectories.add(directory)
    }

    fun getIncludeDirectories(): Set<String> = includeDirectories

    fun makeOptCLIArguments(): OptCLIArguments {
        val optCLIArguments = OptCLIArguments()
        optCLIArguments.setFilename(inputFilename)
        optCLIArguments.setOptLevel(optimizationLevel)
        if (isDumpIr()) {
            optCLIArguments.setDumpIrDirectory(dumpIrDirectoryOutput!!)
        }
        if (outFilename != null) {
            optCLIArguments.setOutputFilename(outFilename!!)
        }
        return optCLIArguments
    }
}


object CCLIParser {
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
                    if (arg.startsWith("-I")) {
                        commandLineArguments.addIncludeDirectory(arg.substring(2))
                    } else {
                        println("Unknown argument: $arg")
                        return null
                    }
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
        println("  -o <filename>             Specify output filename")
        println("  -I <directory>            Add include directory")
    }
}