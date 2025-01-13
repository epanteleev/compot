package startup

import common.Files

enum class Extension(val value: String) {
    C(".c"),
    IR(".ir"),
    ASM(".s"),
    OBJ(".o"),
    EXE(".exe")
}

data class ProcessedFile(val filename: String, val extension: Extension) {
    fun basename(): String {
        return Files.getBasename(filename)
    }

    companion object {
        fun fromFilename(filename: String): ProcessedFile {
            val extension = when {
                filename.endsWith(Extension.C.value) -> Extension.C
                filename.endsWith(Extension.IR.value) -> Extension.IR
                filename.endsWith(Extension.ASM.value) -> Extension.ASM
                filename.endsWith(Extension.OBJ.value) -> Extension.OBJ
                else -> Extension.EXE
            }

            return ProcessedFile(filename, extension)
        }
    }
}


class ShlangCLIArguments {
    private val includeDirectories = hashSetOf<String>()
    private val defines = hashMapOf<String, String>()
    private var preprocessOnly = false
    private var dumpDefines = false
    private var optionC = false
    private var inputs = arrayListOf<ProcessedFile>()

    private var dumpIrDirectoryOutput: String? = null
    private var optimizationLevel = 0
    private var outFilename = ProcessedFile.fromFilename("a.out")

    fun inputs(): List<ProcessedFile> = inputs

    fun setDumpIrDirectory(out: String) {
        dumpIrDirectoryOutput = out
    }

    fun getDumpIrDirectory(): String? = dumpIrDirectoryOutput

    fun setOutputFilename(name: String) {
        outFilename = ProcessedFile.fromFilename(name)
    }

    fun setOptLevel(level: Int) {
        optimizationLevel = level
    }

    fun getOptLevel(): Int = optimizationLevel

    fun getOutputFilename(): ProcessedFile = outFilename

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
        inputs.add(ProcessedFile.fromFilename(executableFileName))
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
}