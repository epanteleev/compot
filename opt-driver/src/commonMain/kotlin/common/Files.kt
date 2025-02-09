package common

import okio.Path.Companion.toPath

object Files {
    fun getBasename(name: String): String {
        val fileName = name.toPath().name
        val lastIndex = fileName.lastIndexOf('.')
        if (lastIndex != -1) {
            return fileName.substring(0, lastIndex)
        }

        return fileName
    }

    fun getDirName(name: String): String {
        return name.toPath().parent.toString()
    }
}