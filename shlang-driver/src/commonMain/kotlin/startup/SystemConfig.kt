package startup

import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries


internal object SystemConfig {
    fun systemHeadersPaths(): List<String>? {
        val paths = arrayListOf<String>()
        if (FileSystem.SYSTEM.exists(USR_INCLUDE_GNU_LINUX_PATH.toPath())) {
            paths.add(USR_INCLUDE_GNU_LINUX_PATH)
        }

        if (FileSystem.SYSTEM.exists(USR_INCLUDE_PATH.toPath())) {
            paths.add(USR_INCLUDE_PATH)
        }

        return paths.takeIf { it.isNotEmpty() }
    }

    fun crtObjects(): List<String> {
        val crtPath = crtPath() ?: throw IllegalStateException("Cannot find crt path")

        val objects = arrayListOf<String>()
        for (gccPath in crtGCCObjects) {
            objects.add("$crtPath/$gccPath")
        }

        if (FileSystem.SYSTEM.exists(CRT_OBJECT_GNU_LINUX_PATH.toPath())) {
            return crtObjects.mapTo(objects) { "$CRT_OBJECT_GNU_LINUX_PATH$it" }
        }

        if (FileSystem.SYSTEM.exists(CRT_OBJECT_PATH.toPath())) {
            return crtObjects.mapTo(objects) { "$CRT_OBJECT_PATH$it" }
        }

        return objects
    }

    private fun crtPath(): String? {
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

        return null
    }

    fun dynamicLinker(): String = "/lib64/ld-linux-x86-64.so.2"

    fun runtimePathes(): List<String> = arrayListOf( // Manjaro
        "-L/usr/lib64",
        "-L/lib64",
        "-L/usr/lib",
        "-L/lib",
    )

    fun runtimeLibraries(): List<String> = arrayListOf("-lc")

    private val crtObjects = listOf(
        "crt1.o",
        "crti.o",
        "crtn.o",
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