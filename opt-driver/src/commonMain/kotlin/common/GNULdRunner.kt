package common


class GNULdRunner(private val outputFileName: ProcessedFile) {
    private var objectFiles = arrayListOf<ProcessedFile>()
    private var crtObjects = arrayListOf<String>()
    private val libs = arrayListOf<String>()
    private val libPaths = arrayListOf<String>()
    private var dynamicLinker: String? = null
    private var static = false
    private var dynamic = false

    fun crtObjects(objects: List<String>): GNULdRunner {
        crtObjects.addAll(objects)
        return this
    }

    fun objs(objs: List<ProcessedFile>): GNULdRunner {
        objectFiles.addAll(objs)
        return this
    }

    fun libs(libs: List<String>): GNULdRunner {
        this.libs.addAll(libs)
        return this
    }

    fun libPaths(libPaths: List<String>): GNULdRunner {
        this.libPaths.addAll(libPaths)
        return this
    }

    fun dynamicLinker(dynLinker: String): GNULdRunner {
        dynamicLinker = dynLinker
        return this
    }

    fun static(enabled: Boolean): GNULdRunner {
        static = enabled
        return this
    }

    fun dynamic(enabled: Boolean): GNULdRunner {
        dynamic = enabled
        return this
    }

    fun execute(): ExecutionResult {
        val gnuLdCommandLine = arrayListOf<String>()
        gnuLdCommandLine.addAll(listOf("-m", "elf_x86_64"))
        gnuLdCommandLine.addAll(listOf("-o", outputFileName.filename))
        gnuLdCommandLine.addAll(libPaths)
        gnuLdCommandLine.addAll(crtObjects)

        if (dynamicLinker != null) {
            gnuLdCommandLine.addAll(listOf("--dynamic-linker", dynamicLinker!!))
        }

        if (static) {
            gnuLdCommandLine.add("-static")
        }
        if (dynamic) {
            gnuLdCommandLine.add("-shared")
        }

        for (obj in objectFiles) {
            gnuLdCommandLine.add(obj.filename)
        }
        gnuLdCommandLine.addAll(libs)

        return runCommand("ld", gnuLdCommandLine, null)
    }
}