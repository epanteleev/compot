package preprocess

import common.FileUtils
import common.getBuildInHeader
import tokenizer.CTokenizer
import tokenizer.TokenList
import java.io.FileInputStream
import java.nio.file.Path
import kotlin.io.path.exists

enum class HeaderType {
    SYSTEM,
    USER
}

data class Header(val filename: String, val content: String, val includeType: HeaderType) {
    fun tokenize(): TokenList {
        return CTokenizer.apply(content, filename)
    }
}

sealed class HeaderHolder(val includeDirectories: Set<String>) {
    protected val headers = hashMapOf<String, Header>()
    private val pragmaOnce = mutableSetOf<String>()

    fun addPragmaOnce(name: String) {
        pragmaOnce.add(name)
    }

    fun isPragmaOnce(name: String): Boolean {
        return pragmaOnce.contains(name)
    }

    abstract fun getHeader(headerName: String, filename: String, includeType: HeaderType): Header?
}

class PredefinedHeaderHolder(includeDirectories: Set<String>): HeaderHolder(includeDirectories) {
    fun addHeader(header: Header): PredefinedHeaderHolder {
        headers[header.filename] = header
        return this
    }

    override fun getHeader(headerName: String, filename: String, includeType: HeaderType): Header? {
        return headers[headerName]
    }
}

class FileHeaderHolder(includeDirectories: Set<String>): HeaderHolder(includeDirectories) {
    private fun tryReadHeader(fullPath: String, type: HeaderType): Header? {
        val filePath = Path.of(fullPath)
        if (!filePath.exists()) {
            return null
        }

        val content = FileInputStream(filePath.toFile()).use { inputStream ->
            inputStream.readBytes().decodeToString()
        }
        return Header(fullPath, content, type)
    }


    private fun getUserHeader(name: String, filename: String): Header? {
        val dir = FileUtils.getDirName(filename)
        val fileName = "$dir/$name"
        return tryReadHeader(fileName, HeaderType.USER)
    }

    private fun getSystemHeader(name: String): Header? {
        val predefined = getBuildInHeader(name)
        if (predefined != null) {
            return Header(name, predefined, HeaderType.SYSTEM)
        }

        for (includeDirectory in includeDirectories) {
            val fileName = "$includeDirectory/$name"
            return tryReadHeader(fileName, HeaderType.SYSTEM) ?: continue
        }
        return null
    }

    override fun getHeader(headerName: String, filename: String, includeType: HeaderType): Header? = when (includeType) {
        HeaderType.USER   -> getUserHeader(headerName, filename) ?: getSystemHeader(headerName)
        HeaderType.SYSTEM -> getSystemHeader(headerName)
    }
}