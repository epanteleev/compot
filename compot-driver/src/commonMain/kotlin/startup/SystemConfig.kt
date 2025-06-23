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

    private fun findCrtStaticObjectPaths(path: String): List<String>? {
        val crtObjectPathRh = Path.of(path)
        if (!crtObjectPathRh.exists()) {
            return null
        }

        val objs = crtCommonStaticObjects.map { "$path$it" }
        val first = Path.of(objs.first())
        if (!first.exists()) {
            return null
        }

        return objs
    }

    private fun crtInit(crtObjects: List<String>): List<String> {
        val crtPath = crtPath()

        val objects = arrayListOf<String>()
        for (crtObject in crtObjects) {
            objects.add("$crtPath/$crtObject")
        }

        return objects
    }

    fun crtStaticObjects(): List<String> {
        val objects = crtInit(crtGCCObjects)


        val staticObjects = findCrtStaticObjectPaths(CRT_OBJECT_PATH)
            ?: findCrtStaticObjectPaths(CRT_OBJECT_PATH_RH)
            ?: findCrtStaticObjectPaths(CRT_OBJECT_GNU_LINUX_PATH)
            ?: arrayListOf()

        return objects + staticObjects
    }

    private fun findCrtSharedObjectPaths(path: String): List<String>? {
        val crtObjectPathRh = Path.of(path)
        if (!crtObjectPathRh.exists()) {
            return null
        }

        val objs = crtCommonSharedObjects.map { "$path$it" }
        val first = Path.of(objs.first())
        if (!first.exists()) {
            return null
        }

        return objs
    }

    fun crtSharedObjects(): List<String> {
        val objects = crtInit(crtSharedObjects)

        val crtObjectPcGcc = findCrtSharedObjectPaths(CRT_OBJECT_PATH_RH)
            ?: findCrtSharedObjectPaths(CRT_OBJECT_GNU_LINUX_PATH)
            ?: findCrtSharedObjectPaths(CRT_OBJECT_PATH)
            ?: arrayListOf()

        return objects + crtObjectPcGcc
    }

    private fun findCrtPath(path: String): String? {
        val crtObjectPath = Path.of(path)
        if (!crtObjectPath.exists()) {
            return null
        }

        val crtPath = crtObjectPath
            .listDirectoryEntries()
            .first()

        return crtPath.toString()
    }

    private fun crtPath(): String {
        return findCrtPath(CRT_OBJECT_GCC_RH) ?:
            findCrtPath(CRT_OBJECT_PC_GCC) ?:
            findCrtPath(CRT_OBJECT_GCC) ?:
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