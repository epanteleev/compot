package preprocess

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import tokenizer.CTokenizer
import tokenizer.TokenList

enum class HeaderType {
    SYSTEM,
    USER
}


data class Header(val filename: String, val content: String, val includeType: HeaderType) {
    fun tokenize(): TokenList {
        return CTokenizer.apply(content)
    }
}

abstract class HeaderHolder(val includeDirectories: Set<String>) {
    protected val headers = hashMapOf<String, Header>()

    abstract fun getHeader(name: String, includeType: HeaderType): Header?

    fun clear() = headers.clear()
}

class PredefinedHeaderHolder(includeDirectories: Set<String>): HeaderHolder(includeDirectories) {
    fun addHeader(header: Header): PredefinedHeaderHolder {
        headers[header.filename] = header
        return this
    }

    override fun getHeader(name: String, includeType: HeaderType): Header? {
        return headers[name]
    }
}

class FileHeaderHolder(private val pwd: String, includeDirectories: Set<String>): HeaderHolder(includeDirectories) {
    private fun getUserHeader(name: String): Header? {
        val filePath = "$pwd/$name".toPath()
        if (!FileSystem.SYSTEM.exists(filePath)) {
            return null
        }

        val content = FileSystem.SYSTEM.read(filePath) {
            readUtf8()
        }
        return Header(name, content, HeaderType.USER)
    }

    private fun getSystemHeader(name: String): Header? {
        for (includeDirectory in includeDirectories) {
            val filePath = "$includeDirectory/$name".toPath()
            if (!FileSystem.SYSTEM.exists(filePath)) {
                continue
            }

            val content = FileSystem.SYSTEM.read(filePath) {
                readUtf8()
            }
            return Header(name, content, HeaderType.SYSTEM)
        }
        return null
    }

    override fun getHeader(name: String, includeType: HeaderType): Header? {
        return when (includeType) {
            HeaderType.USER   -> getUserHeader(name) ?: getSystemHeader(name)
            HeaderType.SYSTEM -> getSystemHeader(name)
        }
    }
}