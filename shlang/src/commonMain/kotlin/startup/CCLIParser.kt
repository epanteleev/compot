package startup


object CCLIParser {
    private fun loop(args: Array<String>): ShlangCLIArguments? {
        var cursor = 0

        val commandLineArguments = ShlangCLIArguments()
        while (cursor < args.size) {
            when (val arg = args[cursor]) {
                "-h", "--help" -> {
                    printHelp()
                    return null
                }
                "-c" -> commandLineArguments.setIsCompile(true)
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

                "-E" -> {
                    commandLineArguments.setPreprocessOnly(true)
                }
                else -> {
                    if (arg.startsWith("-I")) {
                        commandLineArguments.addIncludeDirectory(arg.substring(2))
                    } else if (arg.startsWith("-D")) {
                        val define = arg.substring(2)
                        parseDefine(commandLineArguments, define)
                    } else if (arg.startsWith("-dM")) {
                        commandLineArguments.setDumpDefines(true)
                    } else if (IGNORED_OPTIONS.contains(arg)) {
                        println("Ignoring option: $arg")
                    } else {
                        commandLineArguments.setInputFileName(arg)
                    }
                }
            }
            cursor++
        }

        return commandLineArguments
    }

    fun parse(args: Array<String>): ShlangCLIArguments? {
        if (args.isEmpty()) {
            printHelp()
            return null
        }

        return loop(args)
    }

    private fun parseDefine(shlangCLIArguments: ShlangCLIArguments, define: String) {
        val parts = define.split('=')
        if (parts.size == 1) {
            shlangCLIArguments.addDefine(parts[0], "1")
            return
        }
        if (parts.size != 2) {
            println("Invalid define: $define")
            return
        }
        val macro = parts[0]
        val value = parts[1]
        if (value.startsWith('\'')) {
            val unquote = value.substring(1, value.length - 1)
            shlangCLIArguments.addDefine(macro, unquote)
        } else {
            shlangCLIArguments.addDefine(macro, value)
        }
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
        println("  -D <macro>                Predefine name as a macro, with definition 1.")
        println("  -D <macro>=<value>        Predefine name as a macro, with definition value.")
        println("  -h, --help                Print this help message")
        println("  -E                        Preprocess only; do not compile, assemble or link")
    }


    private val IGNORED_OPTIONS = hashSetOf(
        "-Wall",
        "-pedantic",
        "-ansi",
    )
}