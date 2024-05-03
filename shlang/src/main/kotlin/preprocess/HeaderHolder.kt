package preprocess

import tokenizer.CTokenizer
import tokenizer.TokenIterator
import java.io.File

enum class HeaderType {
    SYSTEM,
    USER
}


data class Header(val filename: String, val content: String, val includeType: HeaderType) {
    fun tokenize(): TokenIterator {
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
        val file = File("$pwd/$name")
        if (!file.exists()) {
            return null
        }

        val content = file.readText() //TODO read file everytime
        return Header(file.name, content, HeaderType.USER)
    }

    private fun getSystemHeader(name: String): Header? {
        for (includeDirectory in includeDirectories) {
            val file = File(includeDirectory + name)
            if (file.exists()) {
                val content = file.readText() //TODO read file everytime
                return Header(file.name, content, HeaderType.SYSTEM)
            }
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