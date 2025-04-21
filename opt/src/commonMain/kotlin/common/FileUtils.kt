package common

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

object FileUtils {
    fun getBasename(name: String): String {
        val fileName = Path.of(name).fileName.toString()
        return removeExtension(fileName)
    }

    fun removeExtension(fileName: String): String {
        val lastIndex = fileName.lastIndexOf('.')
        if (lastIndex != -1) {
            return fileName.substring(0, lastIndex)
        }

        return fileName
    }

    fun getDirName(name: String): String {
        return Path.of(name).toAbsolutePath().parent.toString()
    }

    fun createTempFile(prefix: String): Path {
        val tempDir = Path.of(System.getProperty("java.io.tmpdir"))
        return Files.createTempFile(tempDir, prefix, null).toAbsolutePath()
    }

    fun deleteDirectory(directoryToBeDeleted: File): Boolean {
        val allContents = directoryToBeDeleted.listFiles()
        if (allContents != null) {
            for (file in allContents) {
                deleteDirectory(file)
            }
        }
        return directoryToBeDeleted.delete()
    }
}