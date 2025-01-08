package startup

import common.Files


class ShlangCLIArguments {
    private val includeDirectories = hashSetOf<String>()
    private val defines = hashMapOf<String, String>()
    private var preprocessOnly = false
    private var dumpDefines = false
    private var optionC = false
    private var inputFilename = "<input>"

    private var dumpIrDirectoryOutput: String? = null
    private var optimizationLevel = 0
    private var outFilename: String? = null

    fun isDumpIr(): Boolean = dumpIrDirectoryOutput != null

    fun setDumpIrDirectory(out: String) {
        dumpIrDirectoryOutput = out
    }

    fun setOutputFilename(name: String) {
        outFilename = name
    }

    fun setOptLevel(level: Int) {
        optimizationLevel = level
    }

    fun getOutputFilename(): String {
        if (outFilename != null) {
            return outFilename!!
        }

        val name = getFilename()
        val lastIndex = name.lastIndexOf('.')
        val basename = if (lastIndex != -1) {
            name.substring(0, lastIndex)
        } else {
            name
        }

        return "$basename.o"
    }

    fun getFilename(): String = inputFilename
    fun getBasename(): String = Files.getBasename(inputFilename)

    fun setDumpDefines(dumpDefines: Boolean) {
        this.dumpDefines = dumpDefines
    }

    fun setIsCompile(flag: Boolean) {
        optionC = flag
    }

    fun isCompile() = optionC

    fun setPreprocessOnly(preprocessOnly: Boolean) {
        this.preprocessOnly = preprocessOnly
    }

    fun setInputFileName(executableFileName: String) {
        inputFilename = executableFileName

        if (outFilename == null && Files.getExtension(executableFileName) == ".o") {
            setOutputFilename(Files.replaceExtension(executableFileName, ".o"))
        }
    }

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