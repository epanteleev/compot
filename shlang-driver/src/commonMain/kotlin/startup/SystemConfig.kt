package startup

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

// Mini FAQ about the misc libc/gcc crt files.
// https://dev.gentoo.org/~vapier/crt.txt
internal object SystemConfig {
    fun systemHeadersPaths(): List<String> {
        val paths = arrayListOf<String>()
        val usrIncludeGNUPath = Path.of(USR_INCLUDE_GNU_LINUX_PATH)
        if (usrIncludeGNUPath.exists()) {
            paths.add(USR_INCLUDE_GNU_LINUX_PATH)
        }

        val usrIncludePath = Path.of(USR_INCLUDE_PATH)
        if (usrIncludePath.exists()) {
            paths.add(USR_INCLUDE_PATH)
        }

        return paths
    }

    fun crtStaticObjects(): List<String> {
        val crtPath = crtPath()

        val objects = arrayListOf<String>()
        for (crtObject in crtGCCObjects) {
            objects.add("$crtPath/$crtObject")
        }

        val crtObjectPathRh = Path.of(CRT_OBJECT_PATH_RH)
        if (crtObjectPathRh.exists()) {
            return crtCommonStaticObjects.mapTo(objects) { "$CRT_OBJECT_PATH_RH$it" }
        }

        val crtObjectGNUPath = Path.of(CRT_OBJECT_GNU_LINUX_PATH)
        if (crtObjectGNUPath.exists()) {
            return crtCommonStaticObjects.mapTo(objects) { "$CRT_OBJECT_GNU_LINUX_PATH$it" }
        }

        val crtObjectPath = Path.of(CRT_OBJECT_PATH)
        if (crtObjectPath.exists()) {
            return crtCommonStaticObjects.mapTo(objects) { "$CRT_OBJECT_PATH$it" }
        }

        return objects
    }

    fun crtSharedObjects(): List<String> {
        val crtPath = crtPath()

        val objects = arrayListOf<String>()
        for (crtObject in crtSharedObjects) {
            objects.add("$crtPath/$crtObject")
        }

        val crtObjectPathRh = Path.of(CRT_OBJECT_PATH_RH)
        if (crtObjectPathRh.exists()) {
            return crtCommonSharedObjects.mapTo(objects) { "$CRT_OBJECT_PATH_RH$it" }
        }

        val crtObjectGNUPath = Path.of(CRT_OBJECT_GNU_LINUX_PATH)
        if (crtObjectGNUPath.exists()) {
            return crtCommonSharedObjects.mapTo(objects) { "$CRT_OBJECT_GNU_LINUX_PATH$it" }
        }

        val crtObjectPath = Path.of(CRT_OBJECT_PATH)
        if (crtObjectPath.exists()) {
            return crtCommonSharedObjects.mapTo(objects) { "$CRT_OBJECT_PATH$it" }
        }

        return objects
    }

    private fun crtPath(): String {
        val crtPathGccRh = Path.of(CRT_OBJECT_GCC_RH)
        if (crtPathGccRh.exists()) {
            val crtPath = crtPathGccRh
                .listDirectoryEntries()
                .first()

            return crtPath.toString()
        }

        val crtPathGccPc = Path.of(CRT_OBJECT_PC_GCC)
        if (crtPathGccPc.exists()) {
            val crtPath = crtPathGccPc
                .listDirectoryEntries()
                .first()

            return crtPath.toString()
        }

        val crtPathGcc = Path.of(CRT_OBJECT_GCC)
        if (crtPathGcc.exists()) {
            val crtPath = crtPathGcc
                .listDirectoryEntries()
                .first()

            return crtPath.toString()
        }

        throw IllegalStateException("Cannot find appropriate GCC toolchain")
    }

    fun dynamicLinker(): String = "/lib64/ld-linux-x86-64.so.2"

    fun runtimePathes(): List<String> = arrayListOf( // Manjaro
        "-L/usr/lib64",
        "-L/lib64",
        "-L/usr/lib",
        "-L/lib",
    )

    fun runtimeLibraries(): List<String> = arrayListOf("-lc")

    private val crtCommonStaticObjects = listOf(
        "crt1.o",
        "crti.o",
        "crtn.o",
    )

    private val crtCommonSharedObjects = listOf(
        "crti.o",
        "crtn.o"
    )

    private val crtSharedObjects = listOf(
        "crtbeginS.o",
        "crtendS.o",
    )

    private val crtGCCObjects = listOf(
        "crtbegin.o",
        "crtend.o",
    )

    // Manjaro
    private const val USR_INCLUDE_PATH = "/usr/include/"
    private const val CRT_OBJECT_PATH = "/usr/lib/"
    private const val CRT_OBJECT_PC_GCC = "/usr/lib/gcc/x86_64-pc-linux-gnu/"

    // Ubuntu
    private const val USR_INCLUDE_GNU_LINUX_PATH = "/usr/include/x86_64-linux-gnu/"
    private const val CRT_OBJECT_GNU_LINUX_PATH = "/usr/lib/x86_64-linux-gnu/"
    private const val CRT_OBJECT_GCC = "/usr/lib/gcc/x86_64-linux-gnu/"

    // Red Hat
    private const val CRT_OBJECT_PATH_RH = "/usr/lib64/"
    private const val CRT_OBJECT_GCC_RH = "/usr/lib/gcc/x86_64-redhat-linux/"
}