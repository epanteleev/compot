package startup

import common.Files
import common.commandLine.AnyCLIArguments


class ShlangCLIArguments : AnyCLIArguments() {
    private val includeDirectories = mutableSetOf<String>()
    private val defines = mutableMapOf<String, String>()
    private var preprocessOnly = false
    private var dumpDefines = false
    private var executableFileName: String? = null

    fun setDumpDefines(dumpDefines: Boolean) {
        this.dumpDefines = dumpDefines
    }

    fun setPreprocessOnly(preprocessOnly: Boolean) {
        this.preprocessOnly = preprocessOnly
    }

    fun setExecutableFileName(executableFileName: String) {
        this.executableFileName = executableFileName
        setFilename(executableFileName)
        setOutputFilename(Files.replaceExtension(executableFileName, ".o"))
    }

    fun getExecutableFileName(): String? = executableFileName

    fun isPreprocessOnly(): Boolean = preprocessOnly
    fun isDumpDefines(): Boolean = dumpDefines

    fun addIncludeDirectory(directory: String) {
        includeDirectories.add(directory)
    }

    fun addDefine(name: String, value: String) {
        defines[name] = value
    }

    fun getDefines(): Map<String, String> = defines

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
    fun parse(args: Array<String>): ShlangCLIArguments? {
        if (args.isEmpty()) {
            printHelp()
            return null
        }

        var cursor = 0

        val commandLineArguments = ShlangCLIArguments()
        while (cursor < args.size) {
            when (val arg = args[cursor]) {
                "-h", "--help" -> {
                    printHelp()
                    return null
                }
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
                        commandLineArguments.setExecutableFileName(arg)
                    }
                }
            }
            cursor++
        }

        return commandLineArguments
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


    val IGNORED_OPTIONS = hashSetOf(
        "-Wall",
        "-pedantic",
        "-ansi",
    )
}