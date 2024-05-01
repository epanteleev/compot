package preprocess

import tokenizer.CTokenizer
import tokenizer.TokenIterator
import java.io.File

data class Header(val filename: String, val content: String) {
    fun tokenize(): TokenIterator {
        return CTokenizer.apply(content)
    }
}

abstract class HeaderHolder(val includeDirectories: Set<String>) {
    protected val headers = hashMapOf<String, Header>()

    abstract fun getHeader(name: String): Header?

    fun clear() = headers.clear()
}

class PredefinedHeaderHolder(includeDirectories: Set<String>): HeaderHolder(includeDirectories) {
    fun addHeader(header: Header): PredefinedHeaderHolder {
        headers[header.filename] = header
        return this
    }

    override fun getHeader(name: String): Header? {
        return headers[name]
    }
}