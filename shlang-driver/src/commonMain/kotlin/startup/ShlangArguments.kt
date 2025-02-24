package startup

import common.Extension
import common.ProcessedFile
import logging.CommonLogger


class ShlangArguments {
    private val includeDirectories = hashSetOf<String>()
    private val defines = hashMapOf<String, String>()
    private var preprocessOnly = false
    private var dumpDefines = false
    private var optionC = false
    private var sharedOption = false
    private val dynamicLibraries = hashSetOf<String>()
    private val libraryDirectories = hashSetOf<String>()
    private var inputs = arrayListOf<ProcessedFile>()
    private var pic = false
    private var static = false

    private var dumpIrDirectoryOutput: String? = null
    private var optimizationLevel = 0
    private var outFilename = DEFAULT_OUTPUT

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

    fun setSharedOption(flag: Boolean) {
        sharedOption = flag
    }

    fun isSharedOption() = sharedOption

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

    fun addLibrary(library: String) {
        dynamicLibraries.add(library)
    }

    fun getDynamicLibraries(): Set<String> = libraryDirectories

    fun addLibraryDirectory(directory: String) {
        libraryDirectories.add(directory)
    }

    fun getLibraryDirectories(): Set<String> = libraryDirectories

    fun pic(): Boolean {
        return pic
    }
    fun setPic(pic: Boolean) {
        this.pic = pic
    }

    fun static(): Boolean {
        return static
    }

    fun setStatic(static: Boolean) {
        this.static = static
    }

    fun logger(): CommonLogger {
        return CommonLogger(true)
    }

    companion object {
        val DEFAULT_OUTPUT = ProcessedFile.create("a", Extension.EXE)
    }
}