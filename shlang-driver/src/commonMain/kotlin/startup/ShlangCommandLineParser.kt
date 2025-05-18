package startup


object ShlangCommandLineParser {
    private fun loop(args: Array<String>): ShlangArguments? {
        var cursor = 0

        val commandLineArguments = ShlangArguments()
        while (cursor < args.size) {
            when (val arg = args[cursor]) {
                "-h", "--help" -> {
                    printHelp()
                    return null
                }
                "-c" -> commandLineArguments.setIsCompile(true)
                "-shared" -> commandLineArguments.setLinkage(LinkageType.SHARED)
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
                "-static" -> commandLineArguments.setLinkage(LinkageType.STATIC)
                "-fPIC" -> commandLineArguments.setPic(true)
                "-E" -> commandLineArguments.setPreprocessOnly(true)
                else -> parseOption(commandLineArguments, arg)
            }
            cursor++
        }

        return commandLineArguments
    }

    private fun parseOption(cli: ShlangArguments, arg: String) {
        if (arg.startsWith("-I")) {
            cli.addIncludeDirectory(arg.substring(2))

        } else if (arg.startsWith("-O")) {
            val levelString = arg.substring(2)
            val level = levelString.toIntOrNull() ?: 0
            cli.setOptLevel(level)

        } else if (arg.startsWith("-D")) {
            val define = arg.substring(2)
            parseDefine(cli, define)

        } else if (arg.startsWith("-dM")) {
            cli.setDumpDefines(true)

        } else if (arg.startsWith("-l")) {
            cli.addLibrary(arg)

        } else if (arg.startsWith("-L")) {
            cli.addLibraryDirectory(arg)

        } else if (arg.startsWith("-Wl,")) {
            val linkerOption = arg.substring(4).split(",")
            cli.addExtraLDFlags(linkerOption)

        } else if (isIgnoredOption(arg)) {
            ignoreOption(arg)

        } else {
            cli.setInputFileName(arg)
        }
    }

    private fun isIgnoredOption(arg: String): Boolean {
        if (IGNORED_OPTIONS.contains(arg)) {
            return true
        }
        if (arg.startsWith("-f") ||
            arg.startsWith("-fno-") ||
            arg.startsWith("-std=") ||
            arg.startsWith("-W") ||
            arg.startsWith("-march=")) {
            return true
        }

        return false
    }

    private fun ignoreOption(arg: String) {
        println("Ignoring option: $arg")
    }

    fun parse(args: Array<String>): ShlangArguments? {
        if (args.isEmpty()) {
            printHelp()
            return null
        }

        return loop(args)
    }

    private fun parseDefine(cli: ShlangArguments, define: String) {
        val parts = define.split('=')
        if (parts.size == 1) {
            cli.addDefine(parts[0], "1")
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
            cli.addDefine(macro, unquote)
        } else {
            cli.addDefine(macro, value)
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
        "-pedantic",
        "-ansi",
        "-g",
        "-s",
        "-malign-double",
        "-MMD",
        "-MP",
        "-m64",
    )
}