package startup

import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

// Mini FAQ about the misc libc/gcc crt files.
// https://dev.gentoo.org/~vapier/crt.txt
internal object SystemConfig {
    fun systemHeadersPaths(): List<String> {
        val paths = arrayListOf<String>()
        if (FileSystem.SYSTEM.exists(USR_INCLUDE_GNU_LINUX_PATH.toPath())) {
            paths.add(USR_INCLUDE_GNU_LINUX_PATH)
        }

        if (FileSystem.SYSTEM.exists(USR_INCLUDE_PATH.toPath())) {
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

        if (FileSystem.SYSTEM.exists(CRT_OBJECT_GNU_LINUX_PATH.toPath())) {
            return crtCommonStaticObjects.mapTo(objects) { "$CRT_OBJECT_GNU_LINUX_PATH$it" }
        }

        if (FileSystem.SYSTEM.exists(CRT_OBJECT_PATH.toPath())) {
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

        if (FileSystem.SYSTEM.exists(CRT_OBJECT_GNU_LINUX_PATH.toPath())) {
            return crtCommonSharedObjects.mapTo(objects) { "$CRT_OBJECT_GNU_LINUX_PATH$it" }
        }

        if (FileSystem.SYSTEM.exists(CRT_OBJECT_PATH.toPath())) {
            return crtCommonSharedObjects.mapTo(objects) { "$CRT_OBJECT_PATH$it" }
        }

        return objects
    }

    private fun crtPath(): String {
        val crtPathGccPc = Path(CRT_OBJECT_PC_GCC)
        if (crtPathGccPc.exists()) {
            val crtPath = Path(CRT_OBJECT_PC_GCC)
                .listDirectoryEntries()
                .first()

            return crtPath.toString()
        }

        val crtPathGcc = Path(CRT_OBJECT_GCC)
        if (crtPathGcc.exists()) {
            val crtPath = Path(CRT_OBJECT_GCC)
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
    )

    private val crtSharedObjects = listOf(
        "crtendS.o",
        "crtbeginS.o",
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
}