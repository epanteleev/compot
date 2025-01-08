package startup

import okio.FileSystem
import okio.Path.Companion.toPath

object SystemConfig {
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

    fun crtObjects(): List<String>? {
        if (FileSystem.SYSTEM.exists(CRT_OBJECT_PATH.toPath())) {
            return crtObjects.mapTo(ArrayList()) { "$CRT_OBJECT_PATH/$it" }
        }

        if (FileSystem.SYSTEM.exists(CRT_OBJECT_GNU_LINUX_PATH.toPath())) {
            return crtObjects.mapTo(ArrayList()) { "$CRT_OBJECT_GNU_LINUX_PATH/$it" }
        }

        return null
    }

    fun dynamicLinker(): String = "/lib64/ld-linux-x86-64.so.2"

    fun runtimeLibraries(): List<String> = arrayListOf( // Manjaro
        "-L/usr/lib/x86_64-linux-gnu",
        "-L/usr/lib64",
        "-lc",
    )

    private val crtObjects = listOf(
        "crt1.o",
        "crti.o",
        "crtn.o",
    )

    // Manjaro
    private const val USR_INCLUDE_PATH = "/usr/include/"
    private const val CRT_OBJECT_PATH = "/usr/lib/"

    // Ubuntu
    private const val USR_INCLUDE_GNU_LINUX_PATH = "/usr/include/x86_64-linux-gnu/"
    private const val CRT_OBJECT_GNU_LINUX_PATH = "/usr/lib/x86_64-linux-gnu/"
}