package common


class GNULdRunner(val outputFileName: String) {
    private var objectFiles: List<String>? = null
    private var libs: List<String>? = null
    private var dynamicLinker: String? = null

    fun objs(objs: List<String>): GNULdRunner {
        objectFiles = objs
        return this
    }

    fun libs(libs: List<String>): GNULdRunner {
        this.libs = libs
        return this
    }

    fun dynamicLinker(dynamicLinker: String): GNULdRunner {
        this.dynamicLinker = dynamicLinker
        return this
    }

    fun execute(): ExecutionResult {
        val gnuLdCommandLine = arrayListOf<String>()
        if (libs != null) {
            gnuLdCommandLine.addAll(libs!!)
        }

        if (objectFiles != null) {
            gnuLdCommandLine.addAll(objectFiles!!)
        }

        if (dynamicLinker != null) {
            gnuLdCommandLine.addAll(listOf("--dynamic-linker", dynamicLinker!!))
        }

        gnuLdCommandLine.addAll(listOf("-m", "elf_x86_64"))

        gnuLdCommandLine.addAll(listOf("-o", outputFileName))

        return runCommand("ld", gnuLdCommandLine, null)
    }
}