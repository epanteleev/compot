package common


class GNULdRunner(val outputFileName: ProcessedFile) {
    private var objectFiles: List<String>? = null
    private var crtObjects = arrayListOf<String>()
    private val libs = arrayListOf<String>()
    private val libPaths = arrayListOf<String>()
    private var dynamicLinker: String? = null

    fun crtObjects(objects: List<String>): GNULdRunner {
        crtObjects.addAll(objects)
        return this
    }

    fun objs(objs: List<String>): GNULdRunner {
        objectFiles = objs
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

    fun execute(): ExecutionResult {
        val gnuLdCommandLine = arrayListOf<String>()
        gnuLdCommandLine.addAll(listOf("-m", "elf_x86_64"))
        gnuLdCommandLine.addAll(listOf("-o", outputFileName.filename))
        gnuLdCommandLine.addAll(libPaths)
        gnuLdCommandLine.addAll(crtObjects)

        if (dynamicLinker != null) {
            gnuLdCommandLine.addAll(listOf("--dynamic-linker", dynamicLinker!!))
        }

        if (objectFiles != null) {
            gnuLdCommandLine.addAll(objectFiles!!)
        }
        gnuLdCommandLine.addAll(libs)

        for (lib in gnuLdCommandLine) {
            print("$lib ")
        }
        println()

        return runCommand("ld", gnuLdCommandLine, null)
    }
}