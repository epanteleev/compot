package common


class GNULdRunner(val outputFileName: ProcessedFile) {
    private var objectFiles: List<String>? = null
    private val libs = arrayListOf<String>()
    private val libPaths = arrayListOf<String>()
    private var dynamicLinker: String? = null

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

    fun dynamicLinker(dynamicLinker: String): GNULdRunner {
        this.dynamicLinker = dynamicLinker
        return this
    }

    fun execute(): ExecutionResult {
        val gnuLdCommandLine = arrayListOf<String>()
        gnuLdCommandLine.addAll(listOf("-m", "elf_x86_64"))
        gnuLdCommandLine.addAll(listOf("-o", outputFileName.filename))
        gnuLdCommandLine.addAll(libPaths)

        if (dynamicLinker != null) {
            gnuLdCommandLine.addAll(listOf("--dynamic-linker", dynamicLinker!!))
        }

        if (objectFiles != null) {
            gnuLdCommandLine.addAll(objectFiles!!)
        }
        gnuLdCommandLine.addAll(libs)
        return runCommand("ld", gnuLdCommandLine, null)
    }
}